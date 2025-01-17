#!/bin/bash
echo "fixing bw to 1 Mbps from the beginning"
tc qdisc del dev eth0 root
tc qdisc add dev eth0 root netem rate 1mbit delay 400ms
echo "sleeping 10s ..."
sleep 10
echo "fixing bw to 1 Mbps for 100s"
tc qdisc del dev eth0 root
tc qdisc add dev eth0 root netem rate 1mbit delay 400ms
sleep 100
echo "fixing bw to 350 kbps for 75s"
tc qdisc del dev eth0 root
tc qdisc add dev eth0 root netem rate 350kbit delay 400ms
sleep 75
echo "fixing bw to 2 Mps for 125s"
tc qdisc del dev eth0 root
tc qdisc add dev eth0 root netem rate 2mbit delay 400ms
sleep 125
echo "Stop the DASH client"