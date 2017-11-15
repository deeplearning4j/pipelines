#!/usr/bin/env groovy
import groovy.transform.Field

/**
 * Implements logic for setting up pipeline environment
 */

@Field String jenkinsDockerM2Folder = '/srv/jenkins/storage/docker_m2'
@Field String jenkinsDockerSbtFolder = '/srv/jenkins/storage/docker_ivy2'
/* Temporary set local nexus as primary nexus storage */
@Field String mvnProfileActivationName = env.MAVEN_PROFILE_ACTIVATION_NAME ?: 'nexus'
//        error('MAVEN_PROFILE_ACTIVATION_NAME build parameter was not provided')
@Field String jenkinsDockerM2Mount = jenkinsDockerM2Folder + '/' + mvnProfileActivationName
@Field String localRepositoryPath = "localRepository/${mvnProfileActivationName}"
@Field String mvnSettingsId = 'global_mvn_settings_xml'
@Field List buildDisplayName = []

Map getDockerConfig(String streamName) {
    streamName ?: error("streamName argument can't be null.")

    switch (streamName) {
        case ['android-arm-cpu', 'android-x86-cpu']:
            return [image : 'skymindops/pipelines:android']
            break

        case ['linux-x86_64', 'linux-x86_64-cpu', 'linux-x86_64-cuda-8.0']:
            String dockerParams = nvidiaDockerParams()
            return [image: 'skymindops/pipelines:centos6cuda80', params: dockerParams]
            break

        case ['linux-x86_64-cuda-9.0']:
            String dockerParams = nvidiaDockerParams()
            return [image: 'skymindops/pipelines:centos6cuda90', params: dockerParams]
            break

        case ['linux-ppc64le-cpu', 'linux-ppc64le-cuda-8.0']:
            return [image : 'skymindops/pipelines:ubuntu16cuda80-ppc64le']
            break

        case ['linux-ppc64le-cuda-9.0']:
            return [image : 'skymindops/pipelines:ubuntu16cuda90-ppc64le']
            break

        case ~/^macosx-x86_64.*$/:
        case ~/^windows-x86_64.*$/:
            /* Platforms that don't use docker */
            return [:]
            break

        default:
            throw new IllegalArgumentException("Stream (combination of platform and backend) is not supported.")
            break
    }
}

String getNvidiaDockerParams() {
    String nvidiaDockerGetVolumeScript = "docker volume ls -f DRIVER=nvidia-docker -q | tail -1"
    String nvidiaDockerVolume = sh(script: nvidiaDockerGetVolumeScript, returnStdout: true).trim()
    String dockerInspectScript = "ls -A `docker volume inspect -f \"{{.Mountpoint}}\" ${nvidiaDockerVolume}` " +
            "&& true || false"
    String dockerInspectResult = sh(script: dockerInspectScript, returnStdout: true).trim()
    String dockerParamsTmpfsNvidia = [
            "-v ${jenkinsDockerM2Mount}:/home/jenkins/.m2:z",
            "-v ${jenkinsDockerSbtFolder}:/home/jenkins/.ivy2:z",
            "--device=/dev/nvidiactl",
            "--device=/dev/nvidia-uvm",
            "--device=/dev/nvidia0",
            "--tmpfs /tmp:size=8g"
    ].join(' ')

    return dockerInspectResult ?
            [dockerParamsTmpfsNvidia, "-v=" + nvidiaDockerVolume + ":/usr/local/nvidia:ro"].join(' ') :
            dockerParamsTmpfsNvidia
}

Map getNexusConfig(String mvnProfileActivationName) {
    switch(mvnProfileActivationName) {
        case 'nexus':
            return [url: 'http://master-jenkins.skymind.io:8088/repository/snapshots', profileName: 'local-nexus']
            break
        case 'sonatype':
            return [url: 'https://oss.sonatype.org/content/repositories/snapshots',
                    profileName: 'sonatype-nexus-snapshots']
            break
        case 'bintray':
            return [url: 'https://oss.jfrog.org/artifactory/oss-snapshot-local',
                    profileName: 'bintray-deeplearning4j-maven']
            break
        case 'jfrog':
            return [url: 'https://oss.jfrog.org/artifactory/oss-snapshot-local', profileName: 'local-jfrog']
            break
        default:
            throw new IllegalArgumentException("Profile type is not supported.")
            break
    }
}