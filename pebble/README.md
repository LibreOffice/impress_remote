pebble-remote
=========

This project provides remote control to Libreoffice Impress with Pebble. The directives are all for Ubuntu 14.04, but should be easily adaptable to other platforms. Pebble Remote's web site is www.pebbleremote.com


## Installation

* Install pebble-remote-2.0 package's dependencies.

    * `sudo apt-get install python-dev libopenobex1-dev python-tk python-lightblue python-pexpect xdotool python-bluez`

* To install lightblue clone `lightblue-0.4` from `https://github.com/pebble/lightblue-0.4` and then:
    * `cd lightblue-0.4`
    * `sudo python setup.py install`

* Run this command:

    * `pip install pebble-remote`

## Usage

* Disconnect between the pebble by phone.

* Pair pebble and with your computer on bluetooth.

* If you want to use from consol run this command:
    
    * `pebble-remote /full/path/to/file_name.odp`

* If you want to use from desktop
    * Right click on presentation file 
    * Open with
    * Select Pebble Remote:

* Open music app on your pebble. You can remote presentation by using up and down buttons on Pebble. Use middle button of Pebble for exit presentation. 

## Pictures

![1](https://github.com/COMU/pebble-remote/blob/master/pictures/Screenshot%202015-01-02%20at%2001.33.35.png)
