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

Map getDockerConfig(String streamName) {
    streamName ?: error("streamName argument can't be null.")

    switch (streamName) {
        case ['linux-x86_64-cuda-9.2']:
            String dockerParams = nvidiaDockerParams()
            return [image: 'docker.ci.skymind.io/skymindops/jenkins-agent:amd64-centos7-cuda9.2-cudnn7', params: dockerParams]
            break

        case ['linux-x86_64-cuda-10.0']:
            String dockerParams = nvidiaDockerParams()
            return [image: 'docker.ci.skymind.io/skymindops/jenkins-agent:amd64-centos7-cuda10.0-cudnn7', params: dockerParams]
            break

        case ['linux-x86_64-cuda-10.1']:
            String dockerParams = nvidiaDockerParams()
            return [image: 'docker.ci.skymind.io/skymindops/jenkins-agent:amd64-centos7-cuda10.1-cudnn7', params: dockerParams]
            break

        // --init docker argument required to properly shutdown a container with multiple processes inside it
        case ['linux-ppc64le-cpu']:
            return [image : 'skymindops/jenkins-agent:ppc64le-ubuntu16.04-cuda9.2-cudnn7', params: '--init --shm-size=8g --tmpfs /tmp:size=8g']
            break

        // --init docker argument required to properly shutdown a container with multiple processes inside it
        case ['linux-ppc64le-cuda-9.2']:
            return [image : 'skymindops/jenkins-agent:ppc64le-ubuntu16.04-cuda9.2-cudnn7', params: '--init --shm-size=8g --tmpfs /tmp:size=8g']
            break

        // --init docker argument required to properly shutdown a container with multiple processes inside it
        case ['linux-ppc64le-cuda-10.0']:
            return [image : 'skymindops/jenkins-agent:ppc64le-ubuntu18.04-cuda10.0-cudnn7', params: '--init --shm-size=8g --tmpfs /tmp:size=8g']
            break

        // --init docker argument required to properly shutdown a container with multiple processes inside it
        case ['linux-ppc64le-cuda-10.1']:
            return [image : 'skymindops/jenkins-agent:ppc64le-ubuntu18.04-cuda10.1-cudnn7', params: '--init --shm-size=8g --tmpfs /tmp:size=8g']
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
    String dockerParamsTmpfsNvidia = [
            // FIXME: Change user and group id to match Jenkins user on host, because of permissions issue with protoc-jar-maven-plugin
            "--tmpfs /tmp:uid=1001,gid=1001,mode=1777,size=16g",
            '--shm-size=8g',
            '--runtime=nvidia'
    ].join(' ')

    return dockerParamsTmpfsNvidia
}