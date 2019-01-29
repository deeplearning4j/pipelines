#!/usr/bin/env bash

set -euo pipefail

export DEBIAN_FRONTEND=noninteractive

apt-get -qqy update && \
    apt-get -qqy install --no-install-recommends \
        build-essential openjdk-8-jdk-headless git-core curl wget

#if [ "${PYTHON_VERSION}" == "2" ]; then
     # yum -y install python27
#else
     # yum -y install python36
#fi

# Cleanup
apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*