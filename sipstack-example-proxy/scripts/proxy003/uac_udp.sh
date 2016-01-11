#!/bin/sh

# Script for starting a SIPp instance using TCP
# to our proxy server and then requesting the
# second hop, the "proxied" request, to go over
# UDP. That is indicated by the transport parameter
# in the request-uri as you can see in the uac_udp.xml
# scenario file

# The -rsa parameter is pointing to where you have started
# the sipstack.io Simple Proxy 003 sample app. If you have
# started it on something else than 127.0.0.1:5060 then you
# have to change that line here.
#
# Also, the last argument is where the UAS is running and
# you may have to adjust that as well if you start the two
# UAS:s on different addresses.
sipp -sf uac_udp.xml -t t1 -max_socket 200 -r 1 -rsa 127.0.0.1:5060  127.0.0.1:5080
