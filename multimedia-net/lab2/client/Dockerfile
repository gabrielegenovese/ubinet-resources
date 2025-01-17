FROM ubuntu:jammy

RUN apt-get update && \
    apt-get install iproute2 iputils-ping python3 python3-six pip ffmpeg unzip -y

COPY ./AStream-master.zip  /home/

WORKDIR /home

RUN unzip AStream-master.zip
RUN 
