![ChattyRaspi](https://www.mirko.io/assets/img/portfolio/chattyraspi.png =250x250)

ChattyRaspi
===========

[ChattyRaspi](https://www.github.com/fcracker79/chattyraspi) is a projects that aims at using your RaspBerry Pi as an Alexa Smart Home Device. The main focus is to let the end user customize his Raspberry Pi action upon Alexa commands without the burden of dealing with Alexa Smart Home Skill implementation

Project structure
-----------------

The project is made of the following modules:

1. [The Alexa Smart Home Skill](https://www.github.com/fcracker79/chattyraspi/skill): the skill enables the user to interact with his Raspberry Pi just like any Smart Home device
2. [The Python clients](https://www.github.com/fcracker79/chattyraspi/clients): clients are the boilerplate code that allows your Raspberry Pi to receive commands from Alexa. Currently only Python 3.5+ is available
3. [The Support Backend](https://www.github.com/fcracker79/chattyraspi/backend): backend support to manage your devices

Usage
-----

In order to install and use the skill, you need an Alexa device and a hardware connected to the internet with Python 3.5 installed. That's it. The you proceed as follows:

1. Install the client in your hardware
2. Install the skill in your Alexa device
3. Create your device(s)
4. Upload the configuration file to your hardware
5. Start the example script
6. Detect your devices

Install the client in your hardware
-----------------------------------

Since there is only a Python client, all you need is to install [Chattyraspi](https://pypi.org/project/chattyraspi/) in your Python3.5+ environment.

Install the skill in your Alexa device
--------------------------------------

Well, this is the most annoying part: I am currently working to have the skill certified by Amazon, which is pretty tricky.
As soon as it is certified, you wil just need to add that skill to your Alexa and bind it to your Amazon account.

Alternatively, I might include you into the beta testers list, which entitles you into installing that skill, at least for some months (Amazon automatically removes beta testers after a while). Please contact me if you want to proceed to this direction.

As a desperate alternative, you might try creating a skill on your own using my project (and I encourage you to do), but configuring all the stuff both for the Alexa Skill and the Backend component may be really frustrating, especially if you do not have previous experience with AWS Lambda, Alexa Home Skill, OAuth, AWS AppSync....
Creating your own skill using this codebase is out of scope for this document, but please contact me if you intend to proceed.

Create your device(s)
---------------------

Given that you have my skill up-and-running in your Alexa device, now you have to add your devices.

Just go to the [authentication page](https://chattyraspi.mirko.io), log in with your Amazon credentials and use the Web UI to add as many devices as you want (well, 10 at most. I had to limit the number of devices to prevent misuse).

As you might see, you have to decide the name of your devices: that is the name you will use when interacting with them through your Alexa device.

Upload the configuration file to your hardware
----------------------------------------------

Just download YAML the configuration from the [web UI](https://chattyraspi.mirko.io) and upload it in your hardware.
In what follows, we will refer it as `<devices_configuration.yaml>`.

Start the example script
------------------------

I have included an example script in the ChattyRaspi client to verify that your device is correctly receiving commands from Alexa.

Just run `test_chattyraspi --config <devices_configuration.yaml>` in your hardware to have your client listening to your commands. You should see appropriate print messages upon sending commands from Alexa to your device(s), but you need to detect your devices on Alexa before.

If you need further info on how to implement your own behaviours, please refer to the [client documentation](https://github.com/fcracker79/chattyraspi/blob/master/clients/generic/README.md).

Detect your devices
-------------------

Just ask Alexa to detect your devices: it should find your Raspberry Pis you have configured. Now you can freely send commands to your devices ... well, currently just Turn On and Turn Off commands, but more commands are to come, provided that Amazon clears the certification path.

Troubleshooting
---------------
Have you got issues that may be bugs on my codebase? Please [open an issue](https://github.com/fcracker79/chattyraspi/issues), or, even better, issue a pull request.

For anything else, including issues with using this skill, please contact me!

Disclaimer
----------
This software is provided "as is" and "with all faults." I make no representations or warranties of any kind concerning the safety, suitability, lack of viruses, inaccuracies, typographical errors, or other harmful components of this software. There are inherent dangers in the use of any software, and you are solely responsible for determining whether this software is compatible with your equipment and other software installed on your equipment. You are also solely responsible for the protection of your equipment and backup of your data, and I will not be liable for any damages you may suffer in connection with using, modifying, or distributing this software.