import logging

import wiringpi
from chattyraspi.client import ThermostatMode
import multiprocessing as mp


_SERVO_MIN_MS = 3
_SERVO_MAX_MS = 25
_SERVO_PIN = 1
_LOGGER = logging.getLogger('servomotor')


_FUNCTIONS = dict()


def _get_channels():
    if _get_channels.channels is None:
        _get_channels.channels = mp.Queue(1), mp.Queue(1)
    return _get_channels.channels


_get_channels.channels = None


def _remote_call(function_name: str, *args):
    req, resp = _get_channels()
    req.put((function_name, args))
    result = resp.get()
    if isinstance(result, Exception):
        raise result
    return result


def init_camera():
    p = mp.Process(name='PWMWorker', daemon=True, target=_execute_remote_call, args=_get_channels())
    p.start()
    _remote_call('_init_camera_mp')


def _init_camera_mp():
    _LOGGER.info('wiringpi.wiringPiSetup()', _SERVO_PIN)
    wiringpi.wiringPiSetup()
    _LOGGER.info('wiringpi.softPwmCreate(%s, 0, 200)', _SERVO_PIN)
    wiringpi.softPwmCreate(_SERVO_PIN, 0, 200)


_FUNCTIONS['_init_camera_mp'] = _init_camera_mp


def start_camera():
    return _remote_call('_start_camera_mp')


def _start_camera_mp():
    set_degree(0)


_FUNCTIONS['_start_camera_mp'] = _start_camera_mp


def stop_camera():
    return _remote_call('_stop_camera_mp')


def _stop_camera_mp():
    pass


_FUNCTIONS['_stop_camera_mp'] = _stop_camera_mp


def set_degree(degree: float):
    return _remote_call('_set_degree_mp', degree)


def _set_degree_mp(degree: float):
    degree = int(min(180.0, max(0.0, degree)))
    degree = _map_angle(degree, 0, 200, _SERVO_MIN_MS, _SERVO_MAX_MS)
    _LOGGER.info('wiringpi.softPwmWrite(%s, %s)', _SERVO_PIN, degree)
    wiringpi.softPwmWrite(_SERVO_PIN, degree)


_FUNCTIONS['_set_degree_mp'] = _set_degree_mp


def set_mode(mode: ThermostatMode):
    return _remote_call('_set_mode_mp', mode)


def _set_mode_mp(mode: ThermostatMode):
    _LOGGER.info('TODO IMPLEMENT MODE for %s', mode)
    pass


_FUNCTIONS['_set_mode_mp'] = _set_mode_mp


def _map_angle(value: int, from_low: int, from_high: int, to_low: int, to_high: int) -> int:
    return (to_high - to_low) * (value - from_low) // (from_high - from_low) + to_low


def _function(function_name: str):
    return _FUNCTIONS[function_name]


def _execute_remote_call(req: mp.Queue, resp: mp.Queue):
    while True:
        data = req.get()
        function_name, args = data
        try:
            result = _function(function_name)(*args)
        except Exception as e:
            result = e
        resp.put(result)
