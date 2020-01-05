#!/bin/bash

if (( $# > 1 )); then
    bin/kafka-console-consumer.sh --bootstrap-server localhost:19092 --topic "$2"
else
    bin/kafka-console-consumer.sh --bootstrap-server 10.50.5.1:9092 --topic "$1"
fi
