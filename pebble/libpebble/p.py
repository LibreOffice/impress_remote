#!/usr/bin/env python

import argparse
import os
import libpebble
import time
import pexpect
import tkMessageBox
from pebble_remote import i18n

from Tkinter import *
_ = i18n.language.gettext

MAX_ATTEMPTS = 5

window = Tk()
window.wm_withdraw()
window.geometry("1x1+200+200") #remember its .geometry("WidthxHeight(+or-)X(+or-)Y")

LightBluePebbleError = libpebble.LightBluePebble.LightBluePebbleError
PebbleError = libpebble.pebble.PebbleError

def cmd_remote(pebble, args):
    path=args.odp_file_path
    runodp = args.app_name+" "+path
    pebble.set_nowplaying_metadata(_("LibreOffice Remote Control "), _("Next"), _("Previous"))

    try:
        pexpect.run(runodp, timeout=5)
        window_id = pexpect.run("xdotool search --sync --onlyvisible --class \"libreoffice\"", timeout=5)

        if window_id == "":
            window_id = pexpect.run("xdotool search --sync --onlyvisible --class \"openoffice\"").split('\r\n')
            productname = "openoffice"
            fullscreen = "xdotool key --window " +window_id[0]+" F5"
        else:
            productname = "libreoffice"
            fullscreen = "xdotool windowactivate --sync "+window_id+" key F5"

        pexpect.run(fullscreen)
    except Exception:
        print _("Something's wrong")
        raise
        return False

    def libreoffice_event_handler(event):
        if productname == "libreoffice":
            window_id = pexpect.run("xdotool getactivewindow")
            right_click = "xdotool windowactivate --sync "+window_id+" key Right"
            left_click = "xdotool windowactivate --sync "+window_id+" key Left"
        else:
            window_ids = pexpect.run("xdotool search --sync --onlyvisible --class \"openoffice\"").split('\r\n')
            right_click = "xdotool windowactivate --sync "+window_ids[1]+" key Right"
            left_click = "xdotool windowactivate --sync "+window_ids[1]+" key Left"

        if event == "next":
            pexpect.run(right_click)

        if event == "previous":
            pexpect.run(left_click)

        if event == "exit":
            try:
                if productname == "libreoffice":
                    window_ids = pexpect.run("xdotool search --sync --onlyvisible --name \"libreoffice\"").split("\r\n")
                else:
                    window_ids = pexpect.run("xdotool search --sync --onlyvisible --name \"openoffice\"").split("\r\n")
                window_ids.pop()
                window_ids.reverse()
                if len(window_ids)>=2:
                    window_ids[0:2]
                    altf4_presentation = "xdotool windowactivate --sync "+window_ids[0]+" key --clearmodifiers --delay 100 alt+F4"
                    altf4_edit = "xdotool windowactivate --sync "+window_ids[1]+" key --clearmodifiers --delay 100 alt+F4"
                    pexpect.run(altf4_presentation)
                    pexpect.run(altf4_edit)
                if len(window_ids)<2:
                    altf4_edit = "xdotool windowactivate --sync "+window_ids[0]+" key --clearmodifiers --delay 100 alt+F4"
                    pexpect.run(altf4_edit)

                pexpect.run("exit_click.sh")
            except Exception as e:
                raise e
        print event


    def music_control_handler(endpoint, resp):
        events = {
            "PLAYPAUSE": "exit",
            "PREVIOUS": "previous",
            "NEXT": "next"
        }

        libreoffice_event_handler(events[resp])

    print _("waiting for events")
    while True:
        try:
            pebble.register_endpoint("MUSIC_CONTROL", music_control_handler)
            time.sleep(5)
        except LightBluePebbleError as e:
            tkMessageBox.showerror(title="Error", message=e._message, parent=window)
            raise e
            raise KeyboardInterrupt
        except PebbleError as e:
            tkMessageBox.showerror(title="Error", message=e._message, parent=window)
            raise e
            raise KeyboardInterrupt
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
            raise _('Could not connect to Pebble')
        try:
            pebble_id = args.pebble_id
            if pebble_id is None and "PEBBLE_ID" in os.environ:
                pebble_id = os.environ["PEBBLE_ID"]
            pebble = libpebble.Pebble(pebble_id, args.lightblue, args.pair)
            break
        except LightBluePebbleError as e:
            tkMessageBox.showerror(title="Error", message=_("Bluetooth connection error"), parent=window)
            raise KeyboardInterrupt
        except PebbleError as e:
            tkMessageBox.showerror(title="Error", message=e._message, parent=window)
            raise e
            raise KeyboardInterrupt
        except:
            time.sleep(5)
            attempts += 1

    try:
        args.func(pebble, args)
    except LightBluePebbleError as e:
        tkMessageBox.showerror(title="Error", message=e._message, parent=window)
        raise e
        raise KeyboardInterrupt
    except PebbleError as e:
        tkMessageBox.showerror(title="Error", message=e._message, parent=window)
        raise e
        raise KeyboardInterrupt
    except Exception as e:
        pebble.disconnect()
        raise e
        return

    pebble.disconnect()

if __name__ == '__main__':
    main()
