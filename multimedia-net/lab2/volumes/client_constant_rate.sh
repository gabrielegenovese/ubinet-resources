#!/bin/bash
echo "fixing bw to 1 Mbps from the beginning"
tc qdisc del dev eth0 root
tc qdisc add dev eth0 root netem rate 10mbit delay 10ms