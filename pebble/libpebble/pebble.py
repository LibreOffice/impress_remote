#!/usr/bin/env python

import binascii
import glob
import itertools
import json
import logging
import os
import serial
import signal
import stm32_crc
import struct
import threading
import time
import traceback
import uuid
import zipfile

from collections import OrderedDict
from LightBluePebble import LightBluePebble
from struct import pack, unpack

log = logging.getLogger()
logging.basicConfig(format='[%(levelname)-8s] %(message)s')
log.setLevel(logging.DEBUG)

DEFAULT_PEBBLE_ID = None #Triggers autodetection on unix-like systems
DEBUG_PROTOCOL = False	

class EndpointSync():
	timeout = 10

	def __init__(self, pebble, endpoint):
		pebble.register_endpoint(endpoint, self.callback)
		self.marker = threading.Event()

	def callback(self, *args):
		self.data = args
		self.marker.set()

	def get_data(self):
		try:
			self.marker.wait(timeout=self.timeout)
			return self.data[1]
		except:
			return False

class PebbleError(Exception):
	 def __init__(self, id, message):
		self._id = id
		self._message = message

	 def __str__(self):
		return "%s (ID:%s)" % (self._message, self._id)

class Pebble(object):

	"""
	A connection to a Pebble watch; data and commands may be sent
	to the watch through an instance of this class.
	"""

	endpoints = {
		"TIME": 11,
		"VERSION": 16,
		"PHONE_VERSION": 17,
		"SYSTEM_MESSAGE": 18,
		"MUSIC_CONTROL": 32,
		"PHONE_CONTROL": 33,
		"APPLICATION_MESSAGE": 48,
		"LAUNCHER": 49,
		"LOGS": 2000,
		"PING": 2001,
		"LOG_DUMP": 2002,
		"RESET": 2003,
		"APP": 2004,
		"APP_LOGS": 2006,
		"NOTIFICATION": 3000,
		"RESOURCE": 4000,
		"APP_MANAGER": 6000,
		"PUTBYTES": 48879
	}

	log_levels = {
		0: "*",
		1: "E",
		50: "W",
		100: "I",
		200: "D",
		250: "V"
	}


	@staticmethod
	def AutodetectDevice():
		if os.name != "posix": #i.e. Windows
			raise NotImplementedError("Autodetection is only implemented on UNIX-like systems.")

		pebbles = glob.glob("/dev/tty.Pebble????-SerialPortSe")

		if len(pebbles) == 0:
			raise PebbleError(None, "Autodetection could not find any Pebble devices")
		elif len(pebbles) > 1:
			log.warn("Autodetect found %d Pebbles; using most recent" % len(pebbles))
			#NOTE: Not entirely sure if this is the correct approach
			pebbles.sort(key=lambda x: os.stat(x).st_mtime, reverse=True)

		id = pebbles[0][15:19]
		log.info("Autodetect found a Pebble with ID %s" % id)
		return id

	def __init__(self, id = None, using_lightblue = True, pair_first = False):
		if id is None and not using_lightblue:
			id = Pebble.AutodetectDevice()
		self.id = id
		self.using_lightblue = using_lightblue
		self._alive = True
		self._endpoint_handlers = {}
		self._internal_endpoint_handlers = {
			self.endpoints["TIME"]: self._get_time_response,
			self.endpoints["VERSION"]: self._version_response,
			self.endpoints["PHONE_VERSION"]: self._phone_version_response,
			self.endpoints["SYSTEM_MESSAGE"]: self._system_message_response,
			self.endpoints["MUSIC_CONTROL"]: self._music_control_response,
			self.endpoints["APPLICATION_MESSAGE"]: self._application_message_response,
			self.endpoints["LAUNCHER"]: self._application_message_response,
			self.endpoints["LOGS"]: self._log_response,
			self.endpoints["PING"]: self._ping_response,
			self.endpoints["APP_LOGS"]: self._app_log_response,
			self.endpoints["APP_MANAGER"]: self._appbank_status_response
		}

		try:
			if using_lightblue:
				self._ser = LightBluePebble(self.id, pair_first)
				signal.signal(signal.SIGINT, self._exit_signal_handler)
			else:
				devicefile = "/dev/tty.Pebble"+id+"-SerialPortSe"
				log.debug("Attempting to open %s as Pebble device %s" % (devicefile, id))
				self._ser = serial.Serial(devicefile, 115200, timeout=1)

			log.debug("Initializing reader thread")
			self._read_thread = threading.Thread(target=self._reader)
			self._read_thread.setDaemon(True)
			self._read_thread.start()
			log.debug("Reader thread loaded on tid %s" % self._read_thread.name)
		except PebbleError:
			raise PebbleError(id, "Failed to connect to Pebble")
		except:
			raise

	def _exit_signal_handler(self, signum, frame):
		print "Disconnecting before exiting..."
		self.disconnect()
		time.sleep(1)
		os._exit(0)

	def __del__(self):
		try:
			self._ser.close()
		except:
			pass

	def _reader(self):
		try:
			while self._alive:
				endpoint, resp = self._recv_message()
				if resp == None:
					continue

				if endpoint in self._internal_endpoint_handlers:
					resp = self._internal_endpoint_handlers[endpoint](endpoint, resp)

				if endpoint in self._endpoint_handlers and resp:
					self._endpoint_handlers[endpoint](endpoint, resp)
		except:
			traceback.print_exc()
			raise PebbleError(self.id, "Lost connection to Pebble")
			self._alive = False

	def _pack_message_data(self, lead, parts):
		pascal = map(lambda x: x[:255], parts)
		d = pack("b" + reduce(lambda x,y: str(x) + "p" + str(y), map(lambda x: len(x) + 1, pascal)) + "p", lead, *pascal)
		return d

	def _build_message(self, endpoint, data):
		return pack("!HH", len(data), endpoint)+data

	def _send_message(self, endpoint, data, callback = None):
		if endpoint not in self.endpoints:
			raise PebbleError(self.id, "Invalid endpoint specified")

		msg = self._build_message(self.endpoints[endpoint], data)

		if DEBUG_PROTOCOL:
			log.debug('>>> ' + msg.encode('hex'))
		self._ser.write(msg)

	def _recv_message(self):
		if self.using_lightblue:
			try:
				endpoint, resp, data = self._ser.read()
				if resp is None:
					return None, None
			except TypeError:
				# the lightblue process has likely shutdown and cannot be read from
				self.alive = False
				return None, None
		else:
			data = self._ser.read(4)
			if len(data) == 0:
				return (None, None)
			elif len(data) < 4:
				raise PebbleError(self.id, "Malformed response with length "+str(len(data)))
			size, endpoint = unpack("!HH", data)
			resp = self._ser.read(size)

		if DEBUG_PROTOCOL:
			log.debug("Got message for endpoint %s of length %d" % (endpoint, len(resp)))
			log.debug('<<< ' + (data + resp).encode('hex'))

		return (endpoint, resp)

	def register_endpoint(self, endpoint_name, func):
		if endpoint_name not in self.endpoints:
			raise PebbleError(self.id, "Invalid endpoint specified")

		endpoint = self.endpoints[endpoint_name]
		self._endpoint_handlers[endpoint] = func

	def set_nowplaying_metadata(self, track, album, artist):

		"""Update the song metadata displayed in Pebble's music app."""

		parts = [artist[:30], album[:30], track[:30]]
		self._send_message("MUSIC_CONTROL", self._pack_message_data(16, parts))

	def system_message(self, command):

		"""
		Send a 'system message' to the watch.

		These messages are used to signal important events/state-changes to the watch firmware.
		"""

		commands = {
			"FIRMWARE_AVAILABLE": 0,
			"FIRMWARE_START": 1,
			"FIRMWARE_COMPLETE": 2,
			"FIRMWARE_FAIL": 3,
			"FIRMWARE_UP_TO_DATE": 4,
			"FIRMWARE_OUT_OF_DATE": 5,
			"BLUETOOTH_START_DISCOVERABLE": 6,
			"BLUETOOTH_END_DISCOVERABLE": 7
		}
		if command not in commands:
			raise PebbleError(self.id, "Invalid command \"%s\"" % command)
		data = pack("!bb", 0, commands[command])
		log.debug("Sending command %s (code %d)" % (command, commands[command]))
		self._send_message("SYSTEM_MESSAGE", data)

	def disconnect(self):

		"""Disconnect from the target Pebble."""

		self._alive = False
		self._ser.close()

	def _ping_response(self, endpoint, data):
		restype, retcookie = unpack("!bL", data)
		return retcookie

	def _get_time_response(self, endpoint, data):
		restype, timestamp = unpack("!bL", data)
		return timestamp

	def _system_message_response(self, endpoint, data):
		if len(data) == 2:
			log.info("Got system message %s" % repr(unpack('!bb', data)))
		else:
			log.info("Got 'unknown' system message...")

	def _log_response(self, endpoint, data):
		if (len(data) < 8):
			log.warn("Unable to decode log message (length %d is less than 8)" % len(data))
			return

		timestamp, level, msgsize, linenumber = unpack("!IBBH", data[:8])
		filename = data[8:24].decode('utf-8')
		message = data[24:24+msgsize].decode('utf-8')

		str_level = self.log_levels[level] if level in self.log_levels else "?"

		print timestamp, str_level, filename, linenumber, message

	def _app_log_response(self, endpoint, data):
		if (len(data) < 8):
			log.warn("Unable to decode log message (length %d is less than 8)" % len(data))
			return

		app_uuid = uuid.UUID(bytes=data[0:16])
		timestamp, level, msgsize, linenumber = unpack("!IBBH", data[16:24])
		filename = data[24:40].decode('utf-8')
		message = data[40:40+msgsize].decode('utf-8')

		str_level = self.log_levels[level] if level in self.log_levels else "?"

		print timestamp, str_level, app_uuid, filename, linenumber, message

	def _appbank_status_response(self, endpoint, data):
		apps = {}
		restype, = unpack("!b", data[0])

		app_install_message = {
			0: "app available",
			1: "app removed",
			2: "app updated"
		}

		if restype == 1:
			apps["banks"], apps_installed = unpack("!II", data[1:9])
			apps["apps"] = []

			appinfo_size = 78
			offset = 9
			for i in xrange(apps_installed):
				app = {}
				try:
					app["id"], app["index"], app["name"], app["company"], app["flags"], app["version"] = \
						unpack("!II32s32sIH", data[offset:offset+appinfo_size])
					app["name"] = app["name"].replace("\x00", "")
					app["company"] = app["company"].replace("\x00", "")
					apps["apps"] += [app]
				except:
					if offset+appinfo_size > len(data):
						log.warn("Couldn't load bank %d; remaining data = %s" % (i,repr(data[offset:])))
					else:
						raise
				offset += appinfo_size

			return apps

		elif restype == 2:
			message_id = unpack("!I", data[1:])
			message_id = int(''.join(map(str, message_id)))
			return app_install_message[message_id]

	def _version_response(self, endpoint, data):
		fw_names = {
			0: "normal_fw",
			1: "recovery_fw"
		}

		resp = {}
		for i in xrange(2):
			fwver_size = 47
			offset = i*fwver_size+1
			fw = {}
			fw["timestamp"],fw["version"],fw["commit"],fw["is_recovery"], \
				fw["hardware_platform"],fw["metadata_ver"] = \
				unpack("!i32s8s?bb", data[offset:offset+fwver_size])

			fw["version"] = fw["version"].replace("\x00", "")
			fw["commit"] = fw["commit"].replace("\x00", "")

			fw_name = fw_names[i]
			resp[fw_name] = fw

		resp["bootloader_timestamp"],resp["hw_version"],resp["serial"] = \
			unpack("!L9s12s", data[95:120])

		resp["hw_version"] = resp["hw_version"].replace("\x00","")

		btmac_hex = binascii.hexlify(data[120:126])
		resp["btmac"] = ":".join([btmac_hex[i:i+2].upper() for i in reversed(xrange(0, 12, 2))])

		return resp

	def _application_message_response(self, endpoint, data):
		app_messages = {
			b'\x01': "PUSH",
			b'\x02': "REQUEST",
			b'\xFF': "ACK",
			b'\x7F': "NACK"
		}

		if len(data) > 1:
			rest = data[1:]
		else:
			rest = ''
		if data[0] in app_messages:
			return app_messages[data[0]] + rest


	def _phone_version_response(self, endpoint, data):
		session_cap = {
			"GAMMA_RAY" : 0x80000000,
		}
		remote_cap = {
			"TELEPHONY" : 16,
			"SMS" : 32,
			"GPS" : 64,
			"BTLE" : 128,
			"CAMERA_REAR" : 256,
			"ACCEL" : 512,
			"GYRO" : 1024,
			"COMPASS" : 2048,
		}
		os = {
			"UNKNOWN" : 0,
			"IOS" : 1,
			"ANDROID" : 2,
			"OSX" : 3,
			"LINUX" : 4,
			"WINDOWS" : 5,
		}

		# Then session capabilities, android adds GAMMA_RAY and it's
		# the only session flag so far
		session = session_cap["GAMMA_RAY"]

		# Then phone capabilities, android app adds TELEPHONY and SMS,
		# and the phone type (we know android works for now)
		remote = remote_cap["TELEPHONY"] | remote_cap["SMS"] | os["ANDROID"]

		msg = pack("!biII", 1, -1, session, remote)
		self._send_message("PHONE_VERSION", msg);

	def _music_control_response(self, endpoint, data):
		event, = unpack("!b", data)

		event_names = {
			1: "PLAYPAUSE",
			4: "NEXT",
			5: "PREVIOUS",
		}

		return event_names[event] if event in event_names else None

class PutBytesClient(object):
	states = {
		"NOT_STARTED": 0,
		"WAIT_FOR_TOKEN": 1,
		"IN_PROGRESS": 2,
		"COMMIT": 3,
		"COMPLETE": 4,
		"FAILED": 5
	}

	transfer_types = {
		"FIRMWARE": 1,
		"RECOVERY": 2,
		"SYS_RESOURCES": 3,
		"RESOURCES": 4,
		"BINARY": 5
	}

	def __init__(self, pebble, index, transfer_type, buffer):
		self._pebble = pebble
		self._state = self.states["NOT_STARTED"]
		self._transfer_type = self.transfer_types[transfer_type]
		self._buffer = buffer
		self._index = index
		self._done = False
		self._error = False

	def init(self):
		data = pack("!bIbb", 1, len(self._buffer), self._transfer_type, self._index)
		self._pebble._send_message("PUTBYTES", data)
		self._state = self.states["WAIT_FOR_TOKEN"]

	def wait_for_token(self, resp):
		res, = unpack("!b", resp[0])
		if res != 1:
			log.error("init failed with code %d" % res)
			self._error = True
			return
		self._token, = unpack("!I", resp[1:])
		self._left = len(self._buffer)
		self._state = self.states["IN_PROGRESS"]
		self.send()

	def in_progress(self, resp):
		res, = unpack("!b", resp[0])
		if res != 1:
			self.abort()
			return
		if self._left > 0:
			self.send()
			log.debug("Sent %d of %d bytes" % (len(self._buffer)-self._left, len(self._buffer)))
		else:
			self._state = self.states["COMMIT"]
			self.commit()

	def commit(self):
		data = pack("!bII", 3, self._token & 0xFFFFFFFF, stm32_crc.crc32(self._buffer))
		self._pebble._send_message("PUTBYTES", data)

	def handle_commit(self, resp):
		res, = unpack("!b", resp[0])
		if res != 1:
			self.abort()
			return
		self._state = self.states["COMPLETE"]
		self.complete()

	def complete(self):
		data = pack("!bI", 5, self._token & 0xFFFFFFFF)
		self._pebble._send_message("PUTBYTES", data)

	def handle_complete(self, resp):
		res, = unpack("!b", resp[0])
		if res != 1:
			self.abort()
			return
		self._done = True

	def abort(self):
		msgdata = pack("!bI", 4, self._token & 0xFFFFFFFF)
		self._pebble.send_message("PUTBYTES", msgdata)
		self._error = True

	def send(self):
		datalen =  min(self._left, 2000)
		rg = len(self._buffer)-self._left
		msgdata = pack("!bII", 2, self._token & 0xFFFFFFFF, datalen)
		msgdata += self._buffer[rg:rg+datalen]
		self._pebble._send_message("PUTBYTES", msgdata)
		self._left -= datalen

	def handle_message(self, endpoint, resp):
		if self._state == self.states["WAIT_FOR_TOKEN"]:
			self.wait_for_token(resp)
		elif self._state == self.states["IN_PROGRESS"]:
			self.in_progress(resp)
		elif self._state == self.states["COMMIT"]:
			self.handle_commit(resp)
		elif self._state == self.states["COMPLETE"]:
			self.handle_complete(resp)
