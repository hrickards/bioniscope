#!/bin/sh
sudo hciconfig hci0 up
sudo rfcomm connect 0 00:13:12:18:62:59
