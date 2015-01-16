#!/bin/bash

ps aux | grep p.py | sed 's/\s\+/ /g' | cut -d' ' -f2 | xargs kill
