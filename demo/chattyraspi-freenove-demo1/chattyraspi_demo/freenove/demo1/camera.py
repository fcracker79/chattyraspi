import wiringpi
from chattyraspi.client import ThermostatMode


_SERVO_PIN = 1


def start_camera():
    wiringpi.softPwmCreate(_SERVO_PIN, 0, 200)


def stop_camera():
    pass


def set_degree(degree: float):
    degree = min(180.0, max(0.0, int(degree)))
    wiringpi.softPwmWrite(_SERVO_PIN, degree)


def set_mode(mode: ThermostatMode):
    pass
