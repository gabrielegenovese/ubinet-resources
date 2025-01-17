FROM ubuntu/apache2

RUN apt-get update && \
    apt-get install iproute2 iputils-ping -y

ARG WWWDIR=/var/www/html

COPY ./media.tgz  $WWWDIR/

WORKDIR /var/www/html

RUN tar -v -xzf media.tgz -C /var/www/html/
