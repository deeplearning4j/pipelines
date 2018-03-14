#!/usr/bin/env groovy
import groovy.transform.Field

/**
 * Implements logic for setting up pipeline environment
 */

@Field String jenkinsDockerM2Folder = '/srv/jenkins/storage/docker_m2'
@Field String jenkinsDockerSbtFolder = '/srv/jenkins/storage/docker_ivy2'
/* Temporary set local nexus as primary nexus storage */
@Field String mvnProfileActivationName = env.MAVEN_PROFILE_ACTIVATION_NAME ?: 'sonatype'
@Field String jenkinsDockerM2Mount = jenkinsDockerM2Folder + '/' + mvnProfileActivationName
@Field String localRepositoryPath = ".m2/${mvnProfileActivationName}/repository"
@Field String mvnSettingsId = 'global_mvn_settings_xml'
//@Field List buildDisplayName = []

Map getDockerConfig(String streamName) {
    streamName ?: error("streamName argument can't be null.")

    switch (streamName) {
        case ~/^android.*$/:
            return [image : 'skymindops/pipelines:android']
            break

        case ['linux-x86_64']:
            return [image : 'skymindops/pipelines:centos6cuda80']
            break

        case ['linux-x86_64-cpu']:
            return [image : 'skymindops/pipelines:centos6cuda80', params: '--shm-size=4g --tmpfs /tmp:size=4g']
            break

        case ['linux-x86_64-cuda-8.0']:
            String dockerParams = nvidiaDockerParams()
            return [image: 'skymindops/pipelines:centos6cuda80', params: dockerParams]
            break

        case ['linux-x86_64-cuda-9.0']:
            String dockerParams = nvidiaDockerParams()
            return [image: 'skymindops/pipelines:centos6cuda90', params: dockerParams]
            break

        case ['linux-x86_64-cuda-9.1']:
            String dockerParams = nvidiaDockerParams()
            return [image: 'skymindops/pipelines:centos6cuda91', params: dockerParams]
            break

        case ['linux-ppc64le-cpu']:
            return [image : 'skymindops/pipelines:ubuntu16cuda80-ppc64le', params: '--shm-size=4g --tmpfs /tmp:size=4g']
            break

        case ['linux-ppc64le-cuda-8.0']:
            return [image : 'skymindops/pipelines:ubuntu16cuda80-ppc64le']
            break

        case ['linux-ppc64le-cuda-9.0']:
            return [image : 'skymindops/pipelines:ubuntu16cuda90-ppc64le']
            break

        case ['linux-ppc64le-cuda-9.1']:
            return [image : 'skymindops/pipelines:ubuntu16cuda91-ppc64le']
            break

        case ~/^ios.*$/:
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
            "-v ${jenkinsDockerSbtFolder}:/home/jenkins/.ivy2:z",
            "--device=/dev/nvidiactl",
            "--device=/dev/nvidia-uvm",
            "--device=/dev/nvidia0",
            "--tmpfs /tmp:size=8g",
            '--shm-size=4g'
    ].join(' ')

    return dockerInspectResult ?
            [dockerParamsTmpfsNvidia, "-v=" + nvidiaDockerVolume + ":/usr/local/nvidia:ro"].join(' ') :
            dockerParamsTmpfsNvidia
}