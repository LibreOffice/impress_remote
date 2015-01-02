#!/usr/bin/env python

import argparse
import os
import pebble as libpebble
import time
import pexpect

MAX_ATTEMPTS = 5

def cmd_remote(pebble, args):
    path=args.odp_file_path
    runodp = args.app_name+" --impress "+path
    pebble.set_nowplaying_metadata("LibreOffice Remote Control ", "Next", "Previous")

    try:
        pexpect.run(runodp, timeout=5)
        window_id = pexpect.run("xdotool search --sync --onlyvisible --class \"libreoffice\"")
	fullscreen = "xdotool key --window " +window_id+" F5"
	pexpect.run(fullscreen) 
    except Exception:
        print "Somethings are going bad"
        return False

    def libreoffice_event_handler(event):
        right_click = "xdotool key --window "+ window_id + "Right"
        left_click = "xdotool key --window "+ window_id + "Left"

        if event == "next":
            pexpect.run(right_click)

        if event == "previous":
            pexpect.run(left_click)

    def music_control_handler(endpoint, resp):
        events = {
            "PLAYPAUSE": "playpause",
            "PREVIOUS": "previous",
            "NEXT": "next"
        }

        libreoffice_event_handler(events[resp])

    print "waiting for events"
    while True:
        try:
            pebble.register_endpoint("MUSIC_CONTROL", music_control_handler)
            time.sleep(5)
        except KeyboardInterrupt:
            return

def main():
    parser = argparse.ArgumentParser(description='a utility belt for pebble development')
    parser.add_argument('--pebble_id', type=str, help='the last 4 digits of the target Pebble\'s MAC address. \nNOTE: if \
                        --lightblue is set, providing a full MAC address (ex: "A0:1B:C0:D3:DC:93") won\'t require the pebble \
                        to be discoverable and will be faster')

    parser.add_argument('--lightblue', action="store_true", help='use LightBlue bluetooth API')
    parser.add_argument('--pair', action="store_true", help='pair to the pebble from LightBlue bluetooth API before connecting.')

    subparsers = parser.add_subparsers(help='commands', dest='which')

    remote_parser = subparsers.add_parser('remote', help='control LibreOffice Impress app with music app on Pebble')
    remote_parser.add_argument('app_name', type=str, help='title of application to be controlled')
    remote_parser.add_argument('odp_file_path', type=str, help='path for libreoffice impress presentation file')
    remote_parser.set_defaults(func=cmd_remote)


    args = parser.parse_args()

    attempts = 0
    while True:
        if attempts > MAX_ATTEMPTS:
            raise 'Could not connect to Pebble'
        try:
            pebble_id = args.pebble_id
            if pebble_id is None and "PEBBLE_ID" in os.environ:
                pebble_id = os.environ["PEBBLE_ID"]
            pebble = libpebble.Pebble(pebble_id, args.lightblue, args.pair)
            break
        except:
            time.sleep(5)
            attempts += 1

    try:
        args.func(pebble, args)
    except Exception as e:
        pebble.disconnect()
        raise e
        return

    pebble.disconnect()

if __name__ == '__main__':
    main()
