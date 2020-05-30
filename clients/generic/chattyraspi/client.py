import enum
import json
import logging
import time
from concurrent.futures.thread import ThreadPoolExecutor
from urllib.parse import urlparse

import paho.mqtt.client as mqtt
import requests
import typing

from chattyraspi.device import DevicesConfiguration


class GraphqlException(Exception):
    pass


# Subscriptions are not supported by Cloudfront. So we have to keep the original URL.
_ROOT_URL = 'https://c7knkzejobbqpnaz4gskh77nmm.appsync-api.eu-west-1.amazonaws.com/graphql'
_DEV_ROOT_URL = 'https://dnbcs5up6jcyro7ejt3xgc4zbu.appsync-api.eu-west-1.amazonaws.com/graphql'


def _do_nothing(*_):
    pass


@enum.unique
class ThermostatMode(enum.Enum):
    HEAT = 'HEAT'
    COOL = 'COOL'
    AUTO = 'AUTO'
    OFF = 'OFF'


DeviceID = typing.NewType('DeviceID', str)


class DeviceIdClient:
    # noinspection PyTypeChecker
    def __init__(self, device_id: str, openid_token: str, reconnect_time_seconds: int, appsync_url: str):
        self._headers = {
            'Authorization': openid_token
        }
        self._device_id = device_id
        self.on_turn_on = _do_nothing  # type: typing.Callable[[DeviceID], None]
        self.on_turn_off = _do_nothing  # type: typing.Callable[[DeviceID], None]
        self.fetch_is_power_on = _do_nothing  # type: typing.Callable[[DeviceID], typing.Optional[bool]]
        self.fetch_temperature = _do_nothing  # type: typing.Callable[[DeviceID], typing.Optional[float]]
        self.fetch_thermostat_mode = _do_nothing  # type: typing.Callable[[DeviceID], typing.Optional[ThermostatMode]]
        self.fetch_thermostat_target_setpoint = _do_nothing  # type: typing.Callable[[DeviceID], typing.Optional[float]]

        self.on_set_temperature = _do_nothing  # type: typing.Callable[[DeviceID, float], None]
        self.on_adjust_temperature = _do_nothing  # type: typing.Callable[[DeviceID, float], None]
        self.on_set_thermostat_mode = _do_nothing  # type: typing.Callable[[DeviceID, ThermostatMode], None]

        self._logger = logging.getLogger('client.raspi.alexa.mirko.io')
        self._reconnect_time_seconds = reconnect_time_seconds
        self._appsync_url = appsync_url

    @property
    def device_id(self) -> str:
        return self._device_id

    def _info(self, s: str, *args, **kwargs):
        # noinspection PyArgumentList
        self._log(self._logger.info, s, *args, **kwargs)

    def _debug(self, s: str, *args, **kwargs):
        # noinspection PyArgumentList
        self._log(self._logger.debug, s, *args, **kwargs)

    def _warning(self, s: str, *args, **kwargs):
        # noinspection PyArgumentList
        self._log(self._logger.warning, s, *args, **kwargs)

    def _error(self, s: str, *args, **kwargs):
        # noinspection PyArgumentList
        self._log(self._logger.error, s, *args, **kwargs)

    def _exception(self, s: str, *args, **kwargs):
        # noinspection PyArgumentList
        self._log(self._logger.exception, s, *args, **kwargs)

    def _log(self, log_function: callable, s: str, *args, **kwargs):
        # noinspection PyArgumentList
        log_function('[%s] ' + s, self._device_id, *args, **kwargs)

    def _query(self, q: str):
        response = requests.post(self._appsync_url, json={'query': q}, headers=self._headers)
        response.raise_for_status()
        return response.json()

    def _mutation(self, m: str, v: dict):
        response = requests.post(
            self._appsync_url, json={'query': m, 'variables': v}, headers=self._headers)
        response.raise_for_status()
        response = response.json()
        if response.get('errors'):
            raise GraphqlException(response['errors'])
        return response

    def listen(self):
        while True:
            self._listen()

    def _listen(self):
        self._info('Initializing client')
        post_headers = {
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

        r = requests.post(self._appsync_url, headers=post_headers, json=payload)
        try:
            r.raise_for_status()
        except Exception:
            self._exception('Could not subscribe to device commands, status %s, response %s', r.status_code, r.content)
            raise
        data = r.json()
        self._debug('Subscription response: %s', json.dumps(data, indent=3, sort_keys=True))

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
            arguments = command_payload['data']['onCommandCreated']['arguments']

            def response_execution():
                self._command_executed(command_id)

            # noinspection PyBroadException
            try:
                if command == 'turnOn':
                    self.on_turn_on(self._device_id)
                elif command == 'turnOff':
                    self.on_turn_off(self._device_id)
                elif command == 'powerStatus':
                    response_execution = self._on_power_status(command_id)
                elif command == 'setTemperature':
                    self.on_set_temperature(self._device_id, float(arguments[0]))
                elif command == 'adjustTemperature':
                    self.on_adjust_temperature(self._device_id, float(arguments[0]))
                elif command == 'setThermostatMode':
                    self.on_set_thermostat_mode(self._device_id, ThermostatMode(arguments[0]))
                else:
                    raise ValueError('Unexpected command {}, payload {}'.format(msg.payload['command'], msg))
                response_execution()
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
        client.enable_logger(logging.getLogger('paho-mqtt'))
        client.on_connect = on_connect
        client.on_message = on_message
        client.on_log = lambda *a, **kw: self._info('On log(%s, %s)', a, kw, exc_info=True)
        client.ws_set_options(path="{}?{}".format(urlparts.path, urlparts.query), headers=headers)
        client.tls_set()

        self._debug('Trying to connect now....')
        client.connect(urlparts.netloc, 443)
        try:
            self._debug('Start looping. We will reconnect after %s seconds', self._reconnect_time_seconds)
            client.loop_start()
            time.sleep(self._reconnect_time_seconds)
            self._debug('Timeout expiring')
            client.loop_stop(force=True)
        except Exception:
            self._exception('Error looping forever')
            raise
        finally:
            self._debug('Disconnecting')
            client.disconnect()
            del client

    def _on_power_status(self, command_id: str) -> callable:
        responses = list()

        responses.append('ON' if self.fetch_is_power_on(self._device_id) else 'OFF')

        # noinspection PyNoneFunctionAssignment
        temperature = self.fetch_temperature(self._device_id)
        if temperature is not None:
            # noinspection PyStringFormat
            responses.append("temperature:{:02f}".format(temperature))

        # noinspection PyNoneFunctionAssignment
        thermostat_mode = self.fetch_thermostat_mode(self._device_id)
        if thermostat_mode:
            responses.append("thermostatMode:{}".format(thermostat_mode.name))

        # noinspection PyNoneFunctionAssignment
        thermostat_target_setpoint = self.fetch_thermostat_target_setpoint(self._device_id)
        if thermostat_target_setpoint is not None:
            # noinspection PyStringFormat
            responses.append("thermostatTargetSetpoint:{:02f}".format(thermostat_target_setpoint))

        return lambda: self._command_responded(command_id, *responses)

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

    def _command_responded(self, command_id: str, *response: str):
        self._info('Marking command %s as executed', command_id)
        self._mutation(
            '''
            mutation commandResponded($commandId: ID!, $response: [String]!) {
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
    _SECONDS = 1
    _MINUTES = 60 * _SECONDS
    _HOURS = 60 * _MINUTES

    def __init__(
            self, configuration: DevicesConfiguration, reconnect_time_seconds: int = 12 * _HOURS,
            dev_environment: bool = False
    ):
        appsync_url = _DEV_ROOT_URL if dev_environment else _ROOT_URL
        self._clients_by_device_id = {
            conf['device_id']: DeviceIdClient(
                conf['device_id'], conf['openid_token'], reconnect_time_seconds,
                appsync_url)
            for conf in configuration.get_configuration()['Devices']
        }
        self._logger = logging.getLogger('client.raspi.alexa.mirko.io')

    def set_on_turn_on(self, device_id: str, fun: callable):
        self._clients_by_device_id[device_id].on_turn_on = fun

    def set_on_turn_off(self, device_id: str, fun: callable):
        self._clients_by_device_id[device_id].on_turn_off = fun

    def set_fetch_temperature(self, device_id: str, fun: callable):
        self._clients_by_device_id[device_id].fetch_temperature = fun

    def set_fetch_thermostat_mode(self, device_id: str, fun: callable):
        self._clients_by_device_id[device_id].fetch_thermostat_mode = fun

    def set_fetch_thermostat_target_setpoint(self, device_id: str, fun: callable):
        self._clients_by_device_id[device_id].fetch_thermostat_target_setpoint = fun

    def set_fetch_is_power_on(self, device_id: str, fun: callable):
        self._clients_by_device_id[device_id].fetch_is_power_on = fun

    def set_on_set_temperature(self, device_id, fun: callable):
        self._clients_by_device_id[device_id].set_on_set_temperature = fun

    def set_on_adjust_temperature(self, device_id, fun: callable):
        self._clients_by_device_id[device_id].set_on_adjust_temperature = fun

    def set_on_set_thermostat_mode(self, device_id, fun: callable):
        self._clients_by_device_id[device_id].set_on_set_thermostat_mode = fun

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
