import logging

import wiringpi
from chattyraspi.client import ThermostatMode

_SERVO_MIN_MS = 3
_SERVO_MAX_MS = 25
_SERVO_PIN = 1
_LOGGER = logging.getLogger('servomotor')


def init_camera():
    _LOGGER.info('wiringpi.softPwmCreate(%s, 0, 200)', _SERVO_PIN)
    wiringpi.softPwmCreate(_SERVO_PIN, 0, 200)


def start_camera():
    set_degree(0)


def stop_camera():
    pass


def set_degree(degree: float):
    degree = int(min(180.0, max(0.0, degree)))
    _LOGGER.info('wiringpi.softPwmWrite(%s, %s)', _SERVO_PIN, degree)
    degree = map_angle(degree, 0, 200, _SERVO_MIN_MS, _SERVO_MAX_MS)
    wiringpi.softPwmWrite(_SERVO_PIN, degree)


def set_mode(mode: ThermostatMode):
    _LOGGER.info('TODO IMPLEMENT MODE for %s', mode)
    pass


def map_angle(value: int, from_low: int, from_high: int, to_low: int, to_high: int) -> int:
    return (to_high-to_low)*(value-from_low) // (from_high-from_low) + to_low
