import json
import logging
import time
from concurrent.futures.thread import ThreadPoolExecutor
from urllib.parse import urlparse

import paho.mqtt.client as mqtt
import requests
from raspi_alexa.device import DevicesConfiguration


# TODO create cloudfront distribution to have a human-readable name
_ROOT_URL = 'https://c7knkzejobbqpnaz4gskh77nmm.appsync-api.eu-west-1.amazonaws.com/graphql'


def _do_nothing():
    pass


class DeviceIdClient:
    def __init__(self, device_id: str, openid_token: str):
        self._headers = {
            'Authorization': openid_token
        }
        self._device_id = device_id
        self.on_turn_on = _do_nothing
        self.on_turn_off = _do_nothing
        self.fetch_is_power_on = _do_nothing
        self._logger = logging.getLogger('client.raspi.alexa.mirko.io')

    @property
    def device_id(self) -> str:
        return self._device_id

    def _info(self, s: str, *args, **kwargs):
        self._log(self._logger.info, s, *args)

    def _debug(self, s: str, *args):
        self._log(self._logger.debug, s, *args)

    def _warning(self, s: str, *args):
        self._log(self._logger.warning, s, *args)

    def _error(self, s: str, *args):
        self._log(self._logger.error, s, *args)

    def _exception(self, s: str, *args):
        self._log(self._logger.exception, s, *args)

    def _log(self, log_function, s: str, *args, **kwargs):
        log_function('Device %s: {}'.format(s), self._device_id, *args)

    def _query(self, q: str):
        response = requests.post(_ROOT_URL, json={'query': q}, headers=self._headers)
        response.raise_for_status()
        return response.json()

    def _mutation(self, m: str, v: dict):
        response = requests.post(
            _ROOT_URL, json={'query': m, 'variables': v}, headers=self._headers)
        response.raise_for_status()
        return response.json()

    def listen(self):
        postHeaders = {
            'Content-Type': 'application/json',
            **self._headers
        }

        payload = {
            "operationName": "onCommand",
            "query": """subscription onCommand($deviceId: ID!) {
                  onCommandCreated(deviceId: $deviceId) {
                    commandId
                    deviceId
                    command
                    arguments
                    status
                  }
                }
            """,
            "variables": {"deviceId": self._device_id}
        }

        r = requests.post(_ROOT_URL, headers=postHeaders, json=payload)
        try:
            r.raise_for_status()
        except Exception:
            self._exception('Could not subscribe to device commands, status %s, response %s', r.status_code, r.content)
            raise
        data = r.json()
        # if data.get('errors'):
        #     raise ValueError('Error subscribing: {}'.format(data))
        client_id = data['extensions']['subscription']['mqttConnections'][0]['client']
        ws_url = r.json()['extensions']['subscription']['mqttConnections'][0]['url']
        topic = r.json()['extensions']['subscription']['mqttConnections'][0]['topics'][0]

        # noinspection PyUnusedLocal
        def on_message(_client, _userdata, msg):
            #  {
            #      "data": {
            #          "onCommandCreated": {
            #              "commandId": "3cb6a547-1fea-4034-a98e-9e2b1c17aed6",
            #              "deviceId": "device_001",
            #              "command": "turnOn",
            #              "arguments": [],
            #              "status": 1,
            #              "__typename": "Command"
            #          }
            #      }
            #  }
            command_payload = json.loads(msg.payload.decode())
            command = command_payload['data']['onCommandCreated']['command']
            command_id = command_payload['data']['onCommandCreated']['commandId']
            # noinspection PyBroadException
            try:
                if command == 'turnOn':
                    self.on_turn_on()
                elif command == 'turnOff':
                    self.on_turn_off()
                elif command == 'powerStatus':
                    response = 'ON' if self.fetch_is_power_on() else 'OFF'

                else:
                    raise ValueError('Unexpected command {}, payload {}'.format(msg.payload['command'], msg))
                self._command_executed(command_id)
            except Exception:
                self._exception('Could not process command %s (%s)', command, command_id)
                self._command_failed(command_id)

        # noinspection PyUnusedLocal
        def on_connect(_client, _userdata, flags, rc):
            self._debug('Connected to subscription topic')
            client.subscribe(topic)
        urlparts = urlparse(ws_url)

        headers = {
            "Host": "{0:s}".format(urlparts.netloc),
        }

        client = mqtt.Client(client_id=client_id, transport="websockets")
        client.on_connect = on_connect
        client.on_message = on_message

        client.ws_set_options(path="{}?{}".format(urlparts.path, urlparts.query), headers=headers)
        client.tls_set()

        self._debug('trying to connect now....')
        client.connect(urlparts.netloc, 443)
        try:
            client.loop_forever()
        finally:
            client.disconnect()

    def _command_executed(self, command_id: str):
        self._info('Marking command %s as executed', command_id)
        self._mutation(
            '''
            mutation commandExecuted($commandId: ID!) {
                  markCommandAsExecuted(id: $commandId) {
                    commandId
                    command
                    deviceId
                  }
                }
            ''',
            {
                'commandId': command_id
            }
        )

    def _command_responded(self, command_id: str, response: str):
        self._info('Marking command %s as executed', command_id)
        self._mutation(
            '''
            mutation commandResponded($commandId: ID!, $response: String!) {
                  markCommandAsResponded(id: $commandId, response: $response) {
                    commandId
                    command
                    deviceId
                  }
                }
            ''',
            {
                'commandId': command_id,
                'response': response
            }
        )

    def _command_failed(self, command_id: str):
        self._error('Marking command %s as FAILED', command_id)
        self._mutation(
            '''
            mutation commandExecuted($commandId: ID!) {
                  markCommandAsFailed(id: $commandId) {
                    commandId
                    command
                    deviceId
                  }
                }
            ''',
            {
                'commandId': command_id
            }
        )


class Client:
    def __init__(self, configuration: DevicesConfiguration):
        self._clients_by_device_id = {
            conf['device_id']: DeviceIdClient(conf['device_id'], conf['openid_token'])
            for conf in configuration.get_configuration()['Devices']
        }
        self._logger = logging.getLogger('client.raspi.alexa.mirko.io')

    def set_on_turn_on(self, device_id: str, fun: callable):
        self._clients_by_device_id[device_id].on_turn_on = fun

    def set_on_turn_off(self, device_id: str, fun: callable):
        self._clients_by_device_id[device_id].on_turn_off = fun

    def set_fetch_is_power_on(self, device_id: str, fun: callable):
        self._clients_by_device_id[device_id].fetch_is_power_on = fun

    def listen(self):
        executor = ThreadPoolExecutor(max_workers=len(self._clients_by_device_id))
        # noinspection PyTypeChecker
        for client in self._clients_by_device_id.values():
            executor.submit(self._safe_submit, client)

    def _safe_submit(self, client: DeviceIdClient):
        while True:
            self._logger.info('Start listening to device %s commands....', client.device_id)
            # noinspection PyBroadException
            try:
                client.listen()
            except Exception:
                self._logger.exception(
                    'Error listening to device %s commands. Retrying in 1 second...',
                    client.device_id)
            time.sleep(1)
