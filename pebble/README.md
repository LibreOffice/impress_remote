pebble-remote
=========

This project provides remote control to Libreoffice Impress with Pebble. The directives are all for Ubuntu 14.04, but should be easily adaptable to other platforms. 

## Installation

* To install lightblue clone `lightblue-0.4` from `https://github.com/pebble/lightblue-0.4` and then:
    * `cd lightblue-0.4`
    * `sudo python setup.py install`

* Install pebble-remote-1.1.deb package's dependencies.

    * `sudo apt-get install python-dev libopenobex1-dev python-tk python-lightblue python-pexpect xdotool python-bluez`

* Download debian package from this address[1] and then

    * `sudo dpkg -i pebble-remote-1.1.deb`


## Usage

* Disconnect between the pebble by phone.

* Pair pebble and with your computer on bluetooth.

* Run this command:
    
    * `pebble-remote /full/path/to/file_name.odp`

* Open music app on your pebble. You can remote presentation by using up and down buttons on Pebble.


## Pictures

![1](https://github.com/COMU/pebble-remote/blob/master/pictures/Screenshot%202015-01-02%20at%2001.33.35.png)

---
[1] http://tinyurl.com/pebble-remote-1-1


