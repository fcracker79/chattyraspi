# Chattyraspi
Chattyraspi is a support library to connect your Raspberry Pi to Amazon Alexa.

Quick start
-----
1. Connect to [the configuration console][1]
2. Logon using your Amazon credentials
3. Add your device(s)
4. Download your configuration file
5. Start the the example script:
   `test_chattyraspi --config devices_configuration.yaml`
6. Install Chattyraspy skill on your Alexa
7. Detect your new device(s) on Alexa
8. Turn on/off your device(s) using Alexa

Custom script
-------------
Chattyraspi allows you to intercept all the turn on/off requests from Alexa, 
upon which you can freely react as you wish.

This snippet of code comes from the `test_chattyraspi` script:

```python
#!/usr/bin/env python
from logging.config import fileConfig

import click
import typing

from chattyraspi.client import Client
from chattyraspi.device import DevicesConfiguration


def test_devices():
    # Optionally configure you logging system
    logging_conf_file = '<youg logging configuration>'
    fileConfig(logging_conf_file, disable_existing_loggers=False)
    
    config = DevicesConfiguration('devices_configuration.yaml')
    client = Client(config)

    statuses = dict()

    def _turn_on(device_id: str):
        print('Device {} turned ON'.format(device_id))
        # Here you have received a turn on request.
        # Feel free to do whatever you want, but please remember
        # to mark your device as ON somehow
        statuses[device_id] = True

    def _turn_off(device_id: str):
        print('Device {} turned OFF'.format(device_id))
        # Here you have received a turn off request.
        # Here you are free to do whatever you want, but reasonably
        # you would do the opposite as turn on callback.
        # Finally, please remember that your device is OFF.
        statuses[device_id] = False

    def _fetch_is_power_on(device_id: str) -> bool:
        print('Device {} requested power status'.format(device_id))
        # Here Alexa is asking for the the power status of your device.
        # Please be consistent with what you have done before
        status = statuses[device_id]
        print('Returning', status)
        return status
    
    # Some boilerplate code: here we add the same callbacks for each configured
    # device.
    for device_id in map(lambda d: d['device_id'], config.get_configuration()['Devices']):
        statuses[device_id] = False
        client.set_on_turn_on(device_id, _turn_on)
        client.set_on_turn_off(device_id, _turn_off)
        client.set_fetch_is_power_on(device_id, _fetch_is_power_on)
    client.listen()


if __name__ == '__main__':
    test_devices()

```
 
[1]: https://raspberry.alexa.mirko.io/login