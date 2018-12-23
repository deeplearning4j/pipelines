#!/usr/bin/env bash

set -euo

arch=$1
os=$2

docker build -t skymindops/jenkins-agents:${arch}-${os} -f Dockerfile.${arch}-${os} .
