import json
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
            print('ERROR!!!!!!!!!!!!', r.content)
            raise
        data = r.json()
        # if data.get('errors'):
        #     raise ValueError('Error subscribing: {}'.format(data))
        client_id = data['extensions']['subscription']['mqttConnections'][0]['client']
        ws_url = r.json()['extensions']['subscription']['mqttConnections'][0]['url']
        topic = r.json()['extensions']['subscription']['mqttConnections'][0]['topics'][0]

        def on_message(client, userdata, msg):
            command = json.loads(msg.payload.decode())
            command_id = command['data']['onCommandCreated']['commandId']
            if command_id == 'turnOn':
                self.on_turn_on()
            elif command_id == 'turnOff':
                self.on_turn_off()
            else:
                raise ValueError('Unexpected command {}, payload {}'.format(msg.payload['command'], msg))

        def on_connect(client, userdata, flags, rc):
            print("Connected with result code " + str(rc))
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

        print("trying to connect now....")
        client.connect(urlparts.netloc, 443)
        client.loop_forever()


class Client:
    def __init__(self, configuration: DevicesConfiguration):
        self._clients_by_device_id = {
            conf['device_id']: DeviceIdClient(conf['device_id'], conf['openid_token'])
            for conf in configuration.get_configuration()['Devices']
        }

    def set_on_turn_on(self, device_id: str, fun: callable):
        self._clients_by_device_id[device_id].on_turn_on = fun

    def set_on_turn_off(self, device_id: str, fun: callable):
        self._clients_by_device_id[device_id].on_turn_off = fun

    def listen(self):
        executor = ThreadPoolExecutor(max_workers=len(self._clients_by_device_id))
        for client in self._clients_by_device_id.values():
            executor.submit(self._safe_submit, client)

    def _safe_submit(self, client: DeviceIdClient):
        while True:
            try:
                client.listen()
            except Exception:
                pass
            time.sleep(1)
