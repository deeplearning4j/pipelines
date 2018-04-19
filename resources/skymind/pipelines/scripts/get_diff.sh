#!/usr/bin/env bash

set -euo pipefail

usage() {
	echo "Usage: $0"
	exit 1
}

[[ $# -eq 0 ]] && usage

LIST_OF_CHANGED_FILES=$(git diff-tree --no-commit-id --name-only -r HEAD)
