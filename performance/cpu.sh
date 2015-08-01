#!/bin/bash

# thank you http://stackoverflow.com/questions/1527049/bash-join-elements-of-an-array
function join { local IFS="$1"; shift; echo "$*"; }

# top -p `ps -edalf | grep [s]ipstack.yaml | awk '{print $4}'`,$(join , `pidof sipp`)
# top -p `ps -edalf | grep [U]AS | awk '{print $4}'`,$(join , `pidof sipp`)
top -p `ps -edalf | grep [P]roxy | awk '{print $4}'`,$(join , `pidof sipp`)
