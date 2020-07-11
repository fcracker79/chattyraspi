import logging
import time

from chattyraspi.client import ThermostatMode
import RPi.GPIO as GPIO

_LOGGER = logging.getLogger('servomotor')

_MOTOR_PINS = (1, 4, 5, 6)
_CCW_STEP = (0x01, 0x02, 0x04, 0x08)
_CW_STEP = (0x08, 0x04, 0x02, 0x01)


def init_camera():
    _LOGGER.info('GPIO.setmode(GPIO.BOARD)')
    # noinspection PyUnresolvedReferences
    GPIO.setmode(GPIO.BOARD)
    # use PHYSICAL GPIO Numbering
    for pin in _MOTOR_PINS:
        # noinspection PyUnresolvedReferences
        GPIO.setup(pin, GPIO.OUT)


def start_camera():
    set_degree(0)


def stop_camera():
    _LOGGER.info('GPIO.cleanup()')
    # noinspection PyUnresolvedReferences
    GPIO.cleanup()


def set_degree(degree: float):
    _move_one_period(1, _map_angle(int(min(360.0, max(0.0, degree))), 0, 360, 0, 512))


def set_mode(mode: ThermostatMode):
    _LOGGER.info('TODO IMPLEMENT MODE for %s', mode)
    pass


def _map_angle(value: int, from_low: int, from_high: int, to_low: int, to_high: int) -> int:
    return (to_high - to_low) * (value - from_low) // (from_high - from_low) + to_low


def _move_one_period(direction: int, ms: int) -> None:
    _LOGGER.info('Move one period(direction=%s, ms=%s', direction, ms)
    for j in range(4):
        for i in range(4):
            if direction == 1:
                _LOGGER.info('GPIO.output(_MOTOR_PINS[i], GPIO.HIGH if _CCW_STEP[j] == (1 << i) else GPIO.LOW)')
                # noinspection PyUnresolvedReferences
                GPIO.output(_MOTOR_PINS[i], GPIO.HIGH if _CCW_STEP[j] == (1 << i) else GPIO.LOW)
            else:
                _LOGGER.info('GPIO.output(_MOTOR_PINS[i], GPIO.HIGH if _CW_STEP[j] == (1 << i) else GPIO.LOW)')
                # noinspection PyUnresolvedReferences
                GPIO.output(_MOTOR_PINS[i], GPIO.HIGH if _CW_STEP[j] == (1 << i) else GPIO.LOW)
        _LOGGER.info("Step cycle!")
        if ms < 3:
            ms = 3
            time.sleep(ms * 0.001)


def _move_steps(direction: int, ms: int, steps: int):
    for i in range(steps):
        _move_one_period(direction, ms)


def motor_stop():
    for i in range(4):
        _LOGGER.info('GPIO.output(_MOTOR_PINS[%s], GPIO.LOW)', i)
        # noinspection PyUnresolvedReferences
        GPIO.output(_MOTOR_PINS[i], GPIO.LOW)
