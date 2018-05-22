#!/usr/bin/env bash

set -euo pipefail

module_name() {
    local path=${1%/*}
    local prev_path=${2:-$1}
    local contains_pom=`find "${path}" -type f -name 'pom.xml' 2>/dev/null`

    if [[ ${contains_pom} = '' && ${path} != ${prev_path} ]]; then
        module_name "${path}" "${path}"
    else
        echo "${path}"
    fi
}

module_name "${1}"