#!/usr/bin/env groovy

def call(String project, String version) {
    String releaseTag = [project, version].join('-')

    echo "[INFO] Check if $releaseTag has been already released..."
    isTagExists = sh(returnStdout: true, script: "git tag -l ${releaseTag}").trim()

    if (isTagExists) {
        error "[ERROR] Version ${releaseTag} alread released"
    }
    else {
        echo "[INFO] Tag ${releaseTag} was not found"
    }
}
