#!/bin/sh

top -p `ps -edalf | grep [s]ipstack.yaml | awk '{print $4}'`,`pidof sipp`
