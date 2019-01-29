#!/usr/bin/env bash

set -e

if [ -n "$OS_NAME" ]; then
    case "${OS_NAME}" in
        'centos') OS_ARTIFACT_EXTENSION="rpm" ;;
        'ubuntu') OS_ARTIFACT_EXTENSION="deb" ;;
        *) echo "Unsupported OS name!" && exit 1 ;;
    esac

    mkdir -p "docker/${OS_NAME}/artifacts"

    if [ -n "${OS_ARTIFACT_EXTENSION}" ]; then
        find "$(pwd)" -type f -name 'skil-*.tar.gz' -not -path "$(pwd)/docker/${OS_NAME}/artifacts*" -print0 | xargs -0 cp -u -t "$(pwd)/docker/${OS_NAME}/artifacts"
        find "$(pwd)" -type f -name "skil-*.${OS_ARTIFACT_EXTENSION}" -not -path "$(pwd)/docker/${OS_NAME}/artifacts*" -print0 | xargs -0 cp -u -t "$(pwd)/docker/${OS_NAME}/artifacts"
    fi
else
    echo "\$OS_NAME variable is empty"
fi
