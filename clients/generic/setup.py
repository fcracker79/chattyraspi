import os
import sys
from os import listdir
from os.path import isfile, join

from setuptools import find_packages, setup

major, minor1, minor2, release, serial = sys.version_info

readfile_kwargs = {"encoding": "utf-8"} if major >= 3 else {}


def readfile(filename):
    with open(filename, **readfile_kwargs) as fp:
        contents = fp.read()
    return contents


def get_scripts() -> list:
    path = os.path.join(os.path.dirname(__file__), "bin")
    return [
        os.path.join("bin", os.path.basename(f))
        for f in listdir(path)
        if isfile(join(path, f))
    ]


setup(
    name="chattyraspi",
    version="0.1.1",
    description="Allows interaction between Alexa and your Raspberry Pi",
    long_description=readfile("README.rst"),
    url="https://github.com/fcracker79/alexa-home-skill/clients/generic",
    author="fcracker79",
    author_email="fcracker79@gmail.com",
    license="MIT",
    packages=["chattyraspi"],
    install_requires=readfile(
        os.path.join(os.path.dirname(__file__), "requirements.txt")
    ),
    scripts=get_scripts(),
    zip_safe=False,
    test_suite="test",
    classifiers=[
        "Development Status :: 4 - Beta",
        "Intended Audience :: Developers",
        "Topic :: Software Development",
        "License :: OSI Approved :: MIT License",
        "Programming Language :: Python :: 3.5",
    ],
    keywords="alexa alexa-home alexa-skill",
)
