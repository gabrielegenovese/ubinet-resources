#!/bin/bash
echo "fixing bw to 1 Mbps from the beginning"
tc qdisc del dev eth0 root
tc qdisc add dev eth0 root netem rate 1mbit delay 400ms
echo "sleeping 10s ..."
sleep 10
echo "fixing bw to 1 Mbps for 60s"
tc qdisc del dev eth0 root
tc qdisc add dev eth0 root netem rate 1mbit delay 400ms
sleep 60
echo "fixing bw to 100 kbps for 60s"
tc qdisc del dev eth0 root
tc qdisc add dev eth0 root netem rate 100kbit delay 400ms
sleep 60
echo "fixing bw to 1 Mbps till the end"
tc qdisc del dev eth0 root
tc qdisc add dev eth0 root netem rate 1mbit delay 400ms