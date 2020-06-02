Chattyraspi
===========

Chattyraspi is a support library to connect your Raspberry Pi to Amazon
Alexa.

Quick start
-----------

1. Connect to `the configuration
   console <https://raspberry.alexa.mirko.io/login>`__
2. Logon using your Amazon credentials
3. Add your device(s)
4. Download your configuration file
5. Start the the example script:
   ``test_chattyraspi --config devices_configuration.yaml``
6. Install Chattyraspy skill on your Alexa
7. Detect your new device(s) on Alexa
8. Turn on/off your device(s) using Alexa

Custom script
-------------

Chattyraspi allows you to intercept the following Alexa interface
commands:

1. All the turn on/off requests
2. Temperature sensor commands
3. Thermostat control commands

upon which you can freely react as you wish.

For instance, you might decide to:

-  Power control devices attached on your Raspi using Alexa interface
-  Receive on Alexa temperature info from a sensor connected to your
   Raspi
-  Hack the Alexa thermostat interface to control step motors rotation
-  Hack the Alexa thermostat interface to control brushless motors speed
-  Associate the Tun On/Off command to sysadmin tasks, such as starting
   networking services

This snippet of code comes from the ``test_chattyraspi`` script, as an
example of how to implement custom logics when receiving Alexa commands:

.. code:: python

    #!/usr/bin/env python
    from logging.config import fileConfig

    import click
    import typing

    from chattyraspi.client import Client, ThermostatMode
    from chattyraspi.device import DevicesConfiguration


    def test_devices():
        # Optionally configure you logging system
        logging_conf_file = '<youg logging configuration>'
        fileConfig(logging_conf_file, disable_existing_loggers=False)
        
        config = DevicesConfiguration('devices_configuration.yaml')
        client = Client(config)

        statuses = dict()
        temperatures = dict()
        thermostat_modes = dict()

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
            # Here Alexa is asking for the power status of your device.
            # Please be consistent with what you have done before
            status = statuses[device_id]
            print('Returning', status)
            return status
        
        def _fetch_temperature(device_id: str) -> float:
            # Here Alexa is asking for the current temperature.
            # This may happen both on status report or explicit temperature request.
            print('Device {} requested temperature'.format(device_id))
            temperature = temperatures[device_id]
            # Simulate a temperature that has not been reached yet
            if temperature is not None:
                temperature -= 10
            print('Returning', temperature)
            return temperature

        def _fetch_thermostat_mode(device_id: str) -> ThermostatMode:
             # Here Alexa is asking for the thermostat mode.
            # This may happen both on status report or explicit thermostat mode request.
            print('Device {} requested thermostat mode'.format(device_id))
            thermostat_mode = thermostat_modes[device_id]
            print('Returning', thermostat_mode)
            return thermostat_mode

        def _fetch_thermostat_target_setpoing(device_id: str) -> float:
            # Here Alexa is asking for the current temperature.
            # This may happen both on status report or explicit temperature request.
            print('Device {} requested target setpoint'.format(device_id))
            temperature = temperatures[device_id]
            print('Returning', temperature)
            return temperature

        def _on_set_temperature(device_id: str, temperature: float):
            # Here Alexa is asking to set the target temperature.
            # Please be consistent with what you have done before
            print('Device {} set temperature at {}'.format(device_id, temperature))
            temperatures[device_id] = temperature

        def _on_adjust_temperature(device_id: str, temperature: float):
            # Here Alexa is asking to adjust the target temperature by a delta..
            # Please be consistent with what you have done before        
            print('Device {} adjust temperature by {}'.format(device_id, temperature))
            temperatures[device_id] += temperature

        def _on_set_thermostat_mode(device_id: str, thermostat_mode: ThermostatMode):
            # Here Alexa is asking to set the thermostat mode.
            # Please be consistent with what you have done before
            print('Device {} set thermostat_mode {}'.format(device_id, thermostat_mode))
            thermostat_modes[device_id] = thermostat_mode
        
        # Some boilerplate code: here we add the same callbacks for each configured
        # device.
        for device_id in map(lambda d: d['device_id'], config.get_configuration()['Devices']):
            statuses[device_id] = False
            client.set_on_turn_on(device_id, _turn_on)
            client.set_on_turn_off(device_id, _turn_off)
            client.set_fetch_is_power_on(device_id, _fetch_is_power_on)
            client.set_fetch_temperature(device_id, _fetch_temperature)
            client.set_fetch_thermostat_mode(device_id, _fetch_thermostat_mode)
            client.set_fetch_thermostat_target_setpoint(device_id, _fetch_thermostat_target_setpoing)
            client.set_on_set_temperature(device_id, _on_set_temperature)
            client.set_on_adjust_temperature(device_id, _on_adjust_temperature)
            client.set_on_set_thermostat_mode(device_id, _on_set_thermostat_mode)
        client.listen()


    if __name__ == '__main__':
        test_devices()

Disclaimer
----------

This software is provided "as is" and "with all faults." I make no
representations or warranties of any kind concerning the safety,
suitability, lack of viruses, inaccuracies, typographical errors, or
other harmful components of this software. There are inherent dangers in
the use of any software, and you are solely responsible for determining
whether this software is compatible with your equipment and other
software installed on your equipment. You are also solely responsible
for the protection of your equipment and backup of your data, and I will
not be liable for any damages you may suffer in connection with using,
modifying, or distributing this software.
