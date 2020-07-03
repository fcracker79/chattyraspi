#!/usr/bin/env python
from logging.config import fileConfig

import click
import typing

from chattyraspi.client import Client, ThermostatMode
from chattyraspi.device import DevicesConfiguration

from chattyraspi_demo.freenove.demo1 import lights, camera, commons

LIGHTS = {
    '4e3c7b4a-fc25-4779-8923-2962aae068d2': lights.Light.LUCE0,
    '76cbaa37-2228-48f3-bb01-ae26033ede5d': lights.Light.LUCE1,
    '940b2d63-94d9-4646-bf3a-d525c9d3a56a': lights.Light.LUCE2
}
CAMERA = '79895ffc-3524-4af0-8233-a5b4277275dd'


@click.command()
@click.option('--logging_conf', help='Full path of logging configuration file')
@click.option('--config', help='the path of your devices configuration YAML file', required=True)
def test_devices(
        logging_conf: typing.Optional[str],
        config: typing.Optional[str]):
    if logging_conf:
        fileConfig(logging_conf, disable_existing_loggers=False)

    config = DevicesConfiguration(config)
    # client = Client(config, dev_environment=True)
    client = Client(config)

    statuses = dict()
    temperatures = dict()
    thermostat_modes = dict()

    def _turn_on(device_id: str):
        print('Device {} turned ON'.format(device_id))
        statuses[device_id] = True
        if device_id in LIGHTS:
            lights.turn_on_light(LIGHTS[device_id])
        else:
            camera.start_camera()

    def _turn_off(device_id: str):
        print('Device {} turned OFF'.format(device_id))
        statuses[device_id] = False
        if device_id in LIGHTS:
            lights.turn_off_light(LIGHTS[device_id])
        else:
            camera.stop_camera()

    def _fetch_is_power_on(device_id: str) -> bool:
        print('Device {} requested power status'.format(device_id))
        status = statuses[device_id]
        print('Returning', status)
        return status

    def _fetch_temperature(device_id: str) -> float:
        print('Device {} requested temperature'.format(device_id))
        temperature = temperatures[device_id]
        # Simulate a temperature that has not been reached yet
        if temperature is not None:
            temperature -= 10
        print('Returning', temperature)
        return temperature

    def _fetch_thermostat_mode(device_id: str) -> ThermostatMode:
        print('Device {} requested thermostat mode'.format(device_id))
        thermostat_mode = thermostat_modes[device_id]
        print('Returning', thermostat_mode)
        return thermostat_mode

    def _fetch_thermostat_target_setpoing(device_id: str) -> float:
        print('Device {} requested target setpoint'.format(device_id))
        temperature = temperatures[device_id]
        print('Returning', temperature)
        return temperature

    def _on_set_temperature(device_id: str, temperature: float):
        print('Device {} set temperature at {}'.format(device_id, temperature))
        temperatures[device_id] = temperature
        if device_id == CAMERA:
            camera.set_degree(temperature)

    def _on_adjust_temperature(device_id: str, temperature: float):
        print('Device {} adjust temperature by {}'.format(device_id, temperature))
        temperatures[device_id] += temperature

    def _on_set_thermostat_mode(device_id: str, thermostat_mode: ThermostatMode):
        print('Device {} set thermostat_mode {}'.format(device_id, thermostat_mode))
        thermostat_modes[device_id] = thermostat_mode
        if device_id == CAMERA:
            camera.set_mode(thermostat_mode)

    for device_id in map(lambda d: d['device_id'], config.get_configuration()['Devices']):
        statuses[device_id] = False
        temperatures[device_id] = 0
        thermostat_modes[device_id] = None
        client.set_on_turn_on(device_id, _turn_on)
        client.set_on_turn_off(device_id, _turn_off)
        client.set_fetch_temperature(device_id, _fetch_temperature)
        client.set_fetch_thermostat_mode(device_id, _fetch_thermostat_mode)
        client.set_fetch_thermostat_target_setpoint(device_id, _fetch_thermostat_target_setpoing)
        client.set_fetch_is_power_on(device_id, _fetch_is_power_on)
        client.set_on_set_temperature(device_id, _on_set_temperature)
        client.set_on_adjust_temperature(device_id, _on_adjust_temperature)
        client.set_on_set_thermostat_mode(device_id, _on_set_thermostat_mode)
    commons.init_wiringpi()
    lights.init_lights()
    camera.init_camera()
    client.listen()


if __name__ == '__main__':
    test_devices()
