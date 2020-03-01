#!/usr/bin/env python
from logging.config import fileConfig

import click
import typing

from raspy_alexa import server
from raspy_alexa.device import get_machine_id, DevicesConfiguration
import socket

@click.command()
@click.option('--logging_conf', prompt='logging cnfiguration', help='Full path of logging configuration file')
@click.option('--device_id', prompt='your device id', help='A unique device id associated to your Home Alexa device')
@click.option(
    '--config', prompt='the path of your devices configuration YAML file',
    help='the path of your devices configuration YAML file',
    required=True
)
@click.option(
    '--http_port', default=8080, prompt='HTTP port',
    help='The HTTP port to connect via web to initiate a device registration'
)
def register_device(
        logging_conf: typing.Optional[str],
        device_id: typing.Optional[str],
        config: typing.Optional[str],
        http_port: int):
    if logging_conf:
        fileConfig(logging_conf, disable_existing_loggers=False)

    print(
        'Please connect to http://{hostname}:{port} to register your device with ID {device_id}'.format(
            hostname=socket.gethostname(), port=http_port, device_id=device_id
        )
    )
    server.start_server(DevicesConfiguration(config), device_id or get_machine_id(), http_port=http_port)
