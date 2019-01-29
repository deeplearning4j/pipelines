#!/usr/bin/env bash

set -euo pipefail

source /etc/skil/skil-env.sh

${SKIL_HOME}/sbin/install-python.sh

java -cp "/opt/skil/lib/*:/opt/skil/logback/*"  io.skymind.zeppelin.main.InstallInterpreters

# Cleanup
rm -rf ${SKIL_HOME}/.condarepo
${SKIL_HOME}/miniconda/bin/conda clean --all --yes
apt-get clean
rm -rf /var/lib/apt/lists/* /var/tmp/* /tmp/*
