import logging

import wiringpi
from chattyraspi.client import ThermostatMode


_SERVO_PIN = 1
_LOGGER = logging.getLogger('servomotor')


def start_camera():
    _LOGGER.info('wiringpi.softPwmCreate(%s, 0, 200)', _SERVO_PIN)
    wiringpi.softPwmCreate(_SERVO_PIN, 0, 200)


def stop_camera():
    pass


def set_degree(degree: float):
    degree = int(min(180.0, max(0.0, degree)))
    _LOGGER.info('wiringpi.softPwmWrite(%s, %s)', _SERVO_PIN, degree)
    wiringpi.softPwmWrite(_SERVO_PIN, degree)


def set_mode(mode: ThermostatMode):
    _LOGGER.info('TODO IMPLEMENT MODE for %s', mode)
    pass
