#!/bin/bash

if (( $# > 1 )); then
    bin/kafka-console-producer.sh --broker-list localhost:19092 --topic "$2"
else
    bin/kafka-console-producer.sh --broker-list 10.50.5.1:9092 --topic "$1"
fi
