#!/usr/bin/env bash
set -e

source /etc/skil/skil-env.sh

/opt/skil/sbin/start-skil-daemon "$@"
