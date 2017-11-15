#!/usr/bin/env groovy

def call(String version, Boolean allowSnapshots) {
    String shell = isUnix() ? 'sh' : 'bat'
    String mvnHome = tool 'M339'
    String mvnCommand = [
            'mvn -q',
            'versions:set',
            "-DallowSnapshots=${allowSnapshots}",
            '-DgenerateBackupPoms=false',
            "-DnewVersion=${version}"
    ].join(' ')

    withEnv( ["PATH+MAVEN=${mvnHome}/bin"] ) {
        echo "[INFO] Updating project version"
        "$shell" "$mvnCommand"
    }
}