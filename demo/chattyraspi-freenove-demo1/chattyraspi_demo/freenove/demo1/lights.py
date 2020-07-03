import enum

import wiringpi


_LED1_PIN_NUMBER = 0


class Light(enum.IntEnum):
    LUCE0 = 0
    LUCE1 = 1
    LUCE2 = 2


def init_lights():
    for light in Light:
        wiringpi.pinMode(light.value, wiringpi.OUTPUT)


def turn_on_light(light: Light) -> None:
    wiringpi.digitalWrite(light.value, wiringpi.HIGH)


def turn_off_light(light: Light) -> None:
    wiringpi.digitalWrite(light.value, wiringpi.LOW)
