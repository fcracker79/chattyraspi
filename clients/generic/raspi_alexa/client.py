from urllib.parse import urlparse

import paho.mqtt.client as mqtt
import requests

_ROOT_URL = 'https://c7knkzejobbqpnaz4gskh77nmm.appsync-api.eu-west-1.amazonaws.com/graphql'


class Client:
    def __init__(self, openid_token: str):
        self._headers = {
            'Authorization': openid_token
        }

    def _query(self, q: str):
        response = requests.post(_ROOT_URL, json={'query': q}, headers=self._headers)
        response.raise_for_status()
        return response.json()

    def _mutation(self, m: str, v: dict):
        response = requests.post(
            _ROOT_URL, json={'query': m, 'variables': v}, headers=self._headers)
        response.raise_for_status()
        return response.json()

    def _subscribe(self):
        postHeaders = {
            'Content-Type': 'application/json',
            **self._headers
        }

        payload = {
            "query": """
                subscription Meh($schifoId: String!) {
                  onCreateTESTAPPSYNC(schifoId: $schifoId) {
                    firstName
                    lastName
                    schifoId
                  }
                }
              }
            """
        }

        r = requests.post(_ROOT_URL, headers=postHeaders, json=payload)
        try:
            r.raise_for_status()
        except Exception:
            print('ERROR!!!!!!!!!!!!', r.content)
            raise

        client_id = r.json()['extensions']['subscription']['mqttConnections'][0]['client']
        ws_url = r.json()['extensions']['subscription']['mqttConnections'][0]['url']
        topic = r.json()['extensions']['subscription']['mqttConnections'][0]['topics'][0]

        def on_message(client, userdata, msg):
            print(msg.topic + " " + str(msg.payload))

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
