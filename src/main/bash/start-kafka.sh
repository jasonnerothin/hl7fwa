#!/bin/bash -x

# Build and start from Confluent's stack

git clone https://github.com/confluentinc/cp-docker-images

cd cp-docker-images/examples/cp-all-in-one

# note: allocate 8GB RAM from within Docker preferences

docker-compose up -d --build

