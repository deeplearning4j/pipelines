#!/usr/bin/env bash

set -euo pipefail

source /etc/skil/skil-env.sh

${SKIL_HOME}/sbin/install-python.sh

# Cleanup
rm -rf ${SKIL_HOME}/.condarepo
${SKIL_HOME}/miniconda/bin/conda clean --all --yes
yum clean all && rm -rf /tmp/* && rm -rf /var/cache/yum
