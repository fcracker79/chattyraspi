import json
import os
from urllib.parse import urlencode
import aiohttp
import aiohttp_jinja2
import jinja2
from aiohttp import web

from raspi_alexa.device import DevicesConfiguration

_ROOT_URL = 'https://raspberry.alexa.mirko.io/production'
_LOGIN_URL = '{}/login'.format(_ROOT_URL)
_SUBSCRIBE_URL = '{}/subscribe'.format(_ROOT_URL)

VAR = {}


async def fetch(session, url):
    async with session.get(url, raise_for_status=True) as response:
        return await response.text()


async def get_amazon_user_id(access_token: str) -> str:
    async with aiohttp.ClientSession() as session:
        async with session.get(
                'https://api.amazon.com/user/profile',
                headers={'x-amz-access-token': access_token},
                raise_for_status=True) as response:
            response_text = await response.text()
            return json.loads(response_text)['user_id']


async def login(request):
    arguments = {
        'subscribe_url': 'http://{}/subscription'.format(request.headers.get('Host')),
        'delete_url': 'http://{}/delete_device'.format(request.headers.get('Host')),
    }
    login_url = '{login_url}?{arguments}'.format(
            login_url=_LOGIN_URL,
            arguments=urlencode(arguments)
    )
    raise web.HTTPFound(login_url)


@aiohttp_jinja2.template('subscribed.jinja2')
def subscribed(request):
    return {
        'device_id': request.rel_url.query['device_id']
    }


@aiohttp_jinja2.template('deleted.jinja2')
def deleted(request):
    return {
        'device_id': request.rel_url.query['device_id']
    }


async def subscription(request):
    form_data = await request.post()
    device_id = form_data['device_id']
    openid_token = json.loads(form_data['data'])['data']
    aws_user = await get_amazon_user_id(form_data['aws_token'])
    VAR['devices_configuration'].add_configuration(device_id, aws_user, openid_token)
    raise web.HTTPFound('/subscribed?{}'.format(urlencode({'device_id': device_id})))


async def delete_device(request):
    form_data = await request.post()
    device_id = form_data['device_id']
    VAR['devices_configuration'].delete_configuration(device_id)
    raise web.HTTPFound('/deleted?{}'.format(urlencode({'device_id': device_id})))


def start_server(devices_configuration: DevicesConfiguration, http_port: int = 8080):
    VAR['devices_configuration'] = devices_configuration

    app = web.Application()
    app.add_routes(
        [
            web.get('/', login),
            web.post('/subscription', subscription),
            web.post('/delete_device', delete_device),
            web.get('/subscribed', subscribed),
            web.get('/deleted', deleted)
        ]
    )
    aiohttp_jinja2.setup(
        app, loader=jinja2.FileSystemLoader(os.path.join(os.path.dirname(__file__), '..', 'templates'))
    )

    web.run_app(app, port=http_port)
