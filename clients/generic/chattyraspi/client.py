import base64
import enum
import json
import logging
import threading
import time
import typing
import uuid
from concurrent.futures.thread import ThreadPoolExecutor
from urllib.parse import urlparse

import requests
import websocket

from chattyraspi.device import DevicesConfiguration


class GraphqlException(Exception):
    pass


# Subscriptions are not supported by Cloudfront. So we have to keep the original URL.
_ROOT_URL = 'https://c7knkzejobbqpnaz4gskh77nmm.appsync-api.eu-west-1.amazonaws.com/graphql'
_DEV_ROOT_URL = 'https://dnbcs5up6jcyro7ejt3xgc4zbu.appsync-api.eu-west-1.amazonaws.com/graphql'

_ROOT_WSS_URL = 'wss://c7knkzejobbqpnaz4gskh77nmm.appsync-realtime-api.eu-west-1.amazonaws.com/graphql'
_DEV_ROOT_WSS_URL = 'wss://dnbcs5up6jcyro7ejt3xgc4zbu.appsync-realtime-api.eu-west-1.amazonaws.com/graphql'

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
    def __init__(
            self, device_id: str, openid_token: str, reconnect_time_seconds: int,
            appsync_url: str, appsync_wss_url: str):
        self._headers = {
            'Authorization': openid_token
        }
        self._oidc_header = {
            'host': urlparse(appsync_url).netloc,
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
        self._appsync_wss_url = appsync_wss_url

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
        subscription_payload = json.dumps(
            {
                "operationName": "onCommandPureWebsockets",
                "query": """subscription onCommandPureWebsockets($deviceId: ID!) {
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
        )

        def reset_timer(ws):
            if reset_timer.timeout_timer:
                reset_timer.timeout_timer.cancel()
            timeout_timer = threading.Timer(reset_timer.timeout_interval, lambda: ws.close())
            timeout_timer.daemon = True
            timeout_timer.start()
        reset_timer.timeout_timer = None
        reset_timer.timeout_interval = 120

        def on_message(ws, msg):
            self._info('On message: %s', msg)
            command_payload = json.loads(msg)
            message_type = command_payload['type']

            if message_type == 'ka':
                reset_timer(ws)
            elif message_type == 'connection_ack':
                reset_timer.timeout_interval = int(json.dumps(command_payload['payload']['connectionTimeoutMs']))
                subscription_id = str(uuid.uuid4())
                self._info('Subscribing client %s, subscription_id %s', self._device_id, subscription_id)
                register = {
                    'id': subscription_id,
                    'payload': {
                        'data': subscription_payload,
                        'extensions': {
                            'authorization': self._oidc_header
                        }
                    },
                    'type': 'start'
                }
                start_sub = json.dumps(register)
                self._info('Start subscribing: %s', start_sub)
                ws.send(start_sub)
            elif message_type == 'error':
                self._error('Error from AppSync: %s', command_payload)
                ws.close()
            elif message_type == 'data':
                # deregister = {
                #     'type': 'stop',
                #     'id': self._device_id
                # }
                # end_sub = json.dumps(deregister)
                # self._info('Unsubscribing, %s' + end_sub)
                # ws.send(end_sub)
                execute_command(command_payload['payload'])

        def execute_command(command_payload: dict):
            self._info('Executing command %s', command_payload)
            command = command_payload['data']['onCommandCreated']['command']
            command_id = command_payload['data']['onCommandCreated']['commandId']
            arguments = command_payload['data']['onCommandCreated']['arguments']

            def response_execution():
                self._command_executed(command_id)

            # noinspection PyBroadException
            try:
                self._logger.info('Received command %s(%s)', command, arguments)
                if command == 'turnOn':
                    self.on_turn_on(self._device_id)
                elif command == 'turnOff':
                    self.on_turn_off(self._device_id)
                elif command == 'powerStatus':
                    response_execution = self._on_power_status(command_id)
                elif command == 'setTemperature':
                    self.on_set_temperature(self._device_id, float(arguments[0]))
                    response_execution = self._on_thermostat_changed(command_id)
                elif command == 'adjustTemperature':
                    self.on_adjust_temperature(self._device_id, float(arguments[0]))
                    response_execution = self._on_thermostat_changed(command_id)
                elif command == 'setThermostatMode':
                    self.on_set_thermostat_mode(self._device_id, ThermostatMode(arguments[0]))
                    response_execution = self._on_thermostat_changed(command_id)
                else:
                    raise ValueError('Unexpected command {}, payload {}'.format(command, command_payload))
                response_execution()
            except Exception:
                self._exception('Could not process command %s (%s)', command, command_id)
                self._command_failed(command_id)

        def on_error(ws, error):
            self._error('On error %s: %s', self._device_id, error)

        def on_close(ws, *a):
            self._warning('On close %s, %s', self._device_id, a)

        def on_open(ws):
            init = {
                'type': 'connection_init'
            }
            init_conn = json.dumps(init)
            print('>> ' + init_conn)
            ws.send(init_conn)

        payload = base64.urlsafe_b64encode(json.dumps({}).encode()).decode()
        header = base64.urlsafe_b64encode(json.dumps(self._oidc_header).encode()).decode()
        connection_url = '{}?header={}&payload={}'.format(self._appsync_wss_url, header, payload)

        # noinspection PyTypeChecker
        client = websocket.WebSocketApp(
            connection_url,
            subprotocols=['graphql-ws'],
            on_open=on_open,
            on_message=on_message,
            on_error=on_error,
            on_close=on_close)

        self._debug('Trying to connect now....')

        try:
            self._debug('Start looping. We will reconnect after %s seconds', self._reconnect_time_seconds)
            client.run_forever()
        except Exception:
            self._exception('Error looping forever')
            raise
        finally:
            self._debug('Disconnecting')
            client.close()
            del client

    def _on_thermostat_changed(self, command_id: str) -> callable:
        responses = list()
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
        appsync_wss_url = _DEV_ROOT_WSS_URL if dev_environment else _ROOT_WSS_URL
        self._clients_by_device_id = {
            conf['device_id']: DeviceIdClient(
                conf['device_id'], conf['openid_token'], reconnect_time_seconds,
                appsync_url, appsync_wss_url)
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
        self._clients_by_device_id[device_id].on_set_temperature = fun

    def set_on_adjust_temperature(self, device_id, fun: callable):
        self._clients_by_device_id[device_id].on_adjust_temperature = fun

    def set_on_set_thermostat_mode(self, device_id, fun: callable):
        self._clients_by_device_id[device_id].on_set_thermostat_mode = fun

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
