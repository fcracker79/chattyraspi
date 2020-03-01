import json

import aiohttp
from aiohttp import web

from raspy_alexa.device import DevicesConfiguration

_ROOT_URL = 'https://raspberry.alexa.mirko.io/production'
_LOGIN_URL = '{}/login'.format(_ROOT_URL)
_SUBSCRIBE_URL = '{}/subscribe'.format(_ROOT_URL)

VAR = {}


async def fetch(session, url):
    async with session.get(url) as response:
        user = await response.text()


async def get_amazon_user_id(session, access_token: str) -> str:
    async with session.get(
            'https://api.amazon.com/user/profile',
            headers={'x-amz-access-token': access_token},
            raise_for_status=True) as response:
        return json.loads(response.text)['user_id']


async def login(request):
    async with aiohttp.ClientSession() as session:
        html = await fetch(
            session, '{login_url}?device_id={device_id}'.format(
                login_url=_LOGIN_URL,
                device_id=VAR['device_id'])
        )
        html = html.replace(_ROOT_URL, '')
    return web.Response(text=html, content_type='text/html')


async def subscription(request):
    json_data = await request.post()
    async with aiohttp.ClientSession() as session:
        async with session.post(_SUBSCRIBE_URL, data=json_data, raise_for_status=False) as response:
            response_text = await response.text()
            content_type = response.content_type
            status_code = response.status
            if status_code == 200:
                VAR['devices_configuration'].add_configuration(
                    VAR['device_id'],
                    get_amazon_user_id(session, json_data['access_token'])
                )
            return web.Response(text=response_text, content_type=content_type, status=status_code)
app = web.Application()
app.add_routes(
    [
        web.get('/', login),
        web.post('/subscribe', subscription)
    ]
)


def start_server(devices_configuration: DevicesConfiguration, device_id: str, http_port: int = 8080):
    VAR['device_id'] = device_id
    VAR['devices_configuration'] = devices_configuration
    web.run_app(app, port=http_port)
