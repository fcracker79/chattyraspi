import uuid

import yaml


def get_machine_id() -> str:
    with open('/etc/machine-id', 'r') as f:
        return str(uuid.UUID(hex=f.readline()))


class DevicesConfiguration:
    def __init__(self, filename: str):
        self._filename = filename
        self._data = None

    def get_configuration(self) -> dict:
        if self._data is None:
            try:
                with open(self._filename, 'r') as f:
                    self._data = yaml.load(f, Loader=yaml.FullLoader) or {}
            except FileNotFoundError:
                with open(self._filename, 'w+') as f:
                    self._data = {}
        return self._data

    def add_configuration(self, device_id: str, device_name: str, amazon_user_id: str, openid_token: str) -> None:
        configuration = self.get_configuration()
        devices = configuration.setdefault('Devices', [])
        found_devices = list(filter(lambda d: d['device_id'] == device_id, devices))
        if found_devices:
            if found_devices[0]['amazon_user_id'] != amazon_user_id:
                raise ValueError('Device %s exists with different AWS id'.format(device_id))
            devices[0]['openid_token'] = openid_token
        else:
            devices.append(
                {
                    'device_id': device_id,
                    'device_name': device_name,
                    'amazon_user_id': amazon_user_id,
                    'openid_token': openid_token
                }
            )
        self._data = None
        with open(self._filename, 'w') as f:
            yaml.dump(configuration, stream=f)
        return

    def delete_configuration(self, device_id: str) -> None:
        configuration = self.get_configuration()
        devices = configuration.setdefault('Devices', [])

        indexes_to_remove = list(
            map(
                lambda i_d: i_d[0],
                filter(
                    lambda i_d_2: i_d_2[1]['device_id'] == device_id,
                    enumerate(devices)
                ))
        )
        for idx in indexes_to_remove:
            devices.pop(idx)
        self._data = None
        with open(self._filename, 'w') as f:
            yaml.dump(configuration, stream=f)
        return
