#!/bin/bash

CONFIG_PATH="scales/src/main/resources/conf/config.json"

if [ "$1" = "bridge" ]; then
  EXTERNAL_IP="10.50.0.1"
  ARGS="${*:2}"

  NETWORKING="    networks:
      scalenet:
        ipv4_address: 10.50.1.1"
else
  EXTERNAL_IP="localhost"
  NETWORKING="    network_mode: host"
  ARGS="$@"
fi

# replace kafka host with real host
sed -i "s/\"host\" : \".*\"/\"host\" : \"$EXTERNAL_IP\"/g" $CONFIG_PATH

read -r -d '' DCMPS <<EOM
version: '3.7'

networks:
  scalenet:
    ipam: 
      config: 
        - subnet: 10.50.0.0/16

services:
  config:
    build: .
    hostname: scls
    container_name: scls
    environment: 
      VERTX_CONFIG_PATH: conf/config.json
    env_file: 
      .env
    volumes:
      - ./scales/src/main/resources/conf:/opt/server/conf
    entrypoint: ["sh", "-c"]
    command: ["java -jar scales-0.1.0-all.jar run scales.verticles.MainVerticle -cluster -conf /opt/server/conf/config.json"]
EOM

# fulfil docker-compose
echo "$DCMPS" >docker-compose.yml
echo "$NETWORKING" >>docker-compose.yml

echo "EXTERNAL_HOST IS: $EXTERNAL_IP"

# run docker-compose with its arguments
exec docker-compose $ARGS
