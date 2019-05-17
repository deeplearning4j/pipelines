package skymind.pipelines.projects

import groovy.transform.InheritConstructors

@InheritConstructors
class SkilServerProject extends Project {
    private String mavenBaseCommand = [
            'mvn -U'
    ].findAll().join(' ')

    String zeppelinBranchName = 'skymind-0.7-skil-1.2.0'
    String zeppelinGitUrl = 'https://github.com/SkymindIO/zeppelin.git'
    // Release branch pattern examples: release/1.0.0 or v1.0.0
    protected static releaseBranchPattern = /^(release\/[\d.]+?[-]?[\w]+|v[\d.]+?[-]?[\w]+)$/
    String buildArtifactsPath = 'skil-distro-parent/skil-distro-docker/build-artifacts'

    protected def getBuildMappings() {
        if (release) {
            return [
                [
                    platforms: [
                        [
                                name         : 'linux-x86_64-generic',
                                osName       : 'centos',
                                osVersion    : '7',
                                backend      : 'cpu',
                                sparkVersion : 'spark-1.6',
                                scalaVersion : '2.10',
                                hadoopVersion: 'hadoop-2.7',
                                condaVersion : '4.3.27',
                                pythonVersion: '2'
                        ],
                        [
                                name         : 'linux-x86_64-generic',
                                osName       : 'centos',
                                osVersion    : '7',
                                backend      : 'cpu',
                                sparkVersion : 'spark-2.2',
                                scalaVersion : '2.11',
                                hadoopVersion: 'hadoop-2.7',
                                condaVersion : '4.3.27',
                                pythonVersion: '3'
                        ],
                        [
                                name         : 'linux-x86_64-generic',
                                osName       : 'centos',
                                osVersion    : '7',
                                backend      : 'cuda-10.0',
                                cudnnVersion : '7',
                                sparkVersion : 'spark-1.6',
                                scalaVersion : '2.10',
                                hadoopVersion: 'hadoop-2.7',
                                condaVersion : '4.3.27',
                                pythonVersion: '2'
                        ],
                        [
                                name         : 'linux-x86_64-generic',
                                osName       : 'centos',
                                osVersion    : '7',
                                backend      : 'cuda-10.0',
                                cudnnVersion : '7',
                                sparkVersion : 'spark-2.2',
                                scalaVersion : '2.11',
                                hadoopVersion: 'hadoop-2.7',
                                condaVersion : '4.3.27',
                                pythonVersion: '3'
                        ],
                        [
                                name         : 'linux-x86_64-generic',
                                osName       : 'ubuntu',
                                osVersion    : '16.04',
                                backend      : 'cpu',
                                sparkVersion : 'spark-1.6',
                                scalaVersion : '2.10',
                                hadoopVersion: 'hadoop-2.7',
                                condaVersion : '4.3.27',
                                pythonVersion: '2'
                        ],
//                      [
//                              name         : 'linux-x86_64-generic',
//                              osName       : 'ubuntu',
//                              osVersion    : '16.04',
//                              backend      : 'cpu',
//                              sparkVersion : 'spark-2.2',
//                              scalaVersion : '2.11',
//                              hadoopVersion: 'hadoop-2.7',
//                              condaVersion : '4.3.27',
//                              pythonVersion: '3'
//                      ],
                        [
                                name         : 'linux-x86_64-generic',
                                osName       : 'ubuntu',
                                osVersion    : '16.04',
                                backend      : 'cuda-10.0',
                                cudnnVersion : '7',
                                sparkVersion : 'spark-1.6',
                                scalaVersion : '2.10',
                                hadoopVersion: 'hadoop-2.7',
                                condaVersion : '4.3.27',
                                pythonVersion: '2'
                        ],
//                      [
//                              name         : 'linux-x86_64-generic',
//                              osName       : 'ubuntu',
//                              osVersion    : '16.04',
//                              backend      : 'cuda-10.0',
//                              cudnnVersion : '7',
//                              sparkVersion : 'spark-2.2',
//                              scalaVersion : '2.11',
//                              hadoopVersion: 'hadoop-2.7',
//                              condaVersion : '4.3.27',
//                              pythonVersion: '3'
//                      ],
                        [
                                name         : 'linux-x86_64-generic',
                                osName       : 'ubuntu',
                                osVersion    : '18.04',
                                backend      : 'cpu',
                                sparkVersion : 'spark-1.6',
                                scalaVersion : '2.10',
                                hadoopVersion: 'hadoop-2.7',
                                condaVersion : '4.3.27',
                                pythonVersion: '2'
                        ],
//                      [
//                              name         : 'linux-x86_64-generic',
//                              osName       : 'ubuntu',
//                              osVersion    : '18.04',
//                              backend      : 'cpu',
//                              sparkVersion : 'spark-2.2',
//                              scalaVersion : '2.11',
//                              hadoopVersion: 'hadoop-2.7',
//                              condaVersion : '4.3.27',
//                              pythonVersion: '3'
//                      ],
                        [
                                name         : 'linux-x86_64-generic',
                                osName       : 'ubuntu',
                                osVersion    : '18.04',
                                backend      : 'cuda-10.0',
                                cudnnVersion : '7',
                                sparkVersion : 'spark-1.6',
                                scalaVersion : '2.10',
                                hadoopVersion: 'hadoop-2.7',
                                condaVersion : '4.3.27',
                                pythonVersion: '2'
                        ],
//                      [
//                              name         : 'linux-x86_64-generic',
//                              osName       : 'ubuntu',
//                              osVersion    : '18.04',
//                              backend      : 'cuda-10.0',
//                              cudnnVersion : '7',
//                              sparkVersion : 'spark-2.2',
//                              scalaVersion : '2.11',
//                              hadoopVersion: 'hadoop-2.7',
//                              condaVersion : '4.3.27',
//                              pythonVersion: '3'
//                      ],
                        [
                                name         : 'windows-x86_64-cpu',
                                osName       : 'windows',
                                osVersion    : 'server-2016',
                                backend      : 'cpu',
                                sparkVersion : 'spark-1.6',
                                scalaVersion : '2.10',
                                hadoopVersion: 'hadoop-2.7',
                                condaVersion : '4.3.27',
                                pythonVersion: '2'
                        ],
//                      [
//                              name         : 'windows-x86_64-cuda-10.0',
//                              osName       : 'windows',
//                              osVersion    : 'server-2016',
//                              backend      : 'cuda-10.0',
//                              cudnnVersion : '7',
//                              sparkVersion : 'spark-1.6',
//                              scalaVersion : '2.10',
//                              hadoopVersion: 'hadoop-2.7',
//                              condaVersion : '4.3.27',
//                              pythonVersion: '2'
//                      ]
                    ]
                ]
            ]
        } else {
            return [
                [
                    platforms: [
                        [
                                name         : 'linux-x86_64-generic',
                                osName       : 'centos',
                                osVersion    : '7',
                                backend      : 'cpu',
                                sparkVersion : 'spark-1.6',
                                scalaVersion : '2.10',
                                hadoopVersion: 'hadoop-2.7',
                                condaVersion : '4.3.27',
                                pythonVersion: '2'
                        ],
                        [
                                name         : 'linux-x86_64-generic',
                                osName       : 'centos',
                                osVersion    : '7',
                                backend      : 'cpu',
                                sparkVersion : 'spark-2.2',
                                scalaVersion : '2.11',
                                hadoopVersion: 'hadoop-2.7',
                                condaVersion : '4.3.27',
                                pythonVersion: '3'
                        ],
                        [
                                name         : 'linux-x86_64-generic',
                                osName       : 'centos',
                                osVersion    : '7',
                                backend      : 'cuda-10.0',
                                cudnnVersion : '7',
                                sparkVersion : 'spark-1.6',
                                scalaVersion : '2.10',
                                hadoopVersion: 'hadoop-2.7',
                                condaVersion : '4.3.27',
                                pythonVersion: '2'
                        ],
                        [
                                name         : 'linux-x86_64-generic',
                                osName       : 'centos',
                                osVersion    : '7',
                                backend      : 'cuda-10.0',
                                cudnnVersion : '7',
                                sparkVersion : 'spark-2.2',
                                scalaVersion : '2.11',
                                hadoopVersion: 'hadoop-2.7',
                                condaVersion : '4.3.27',
                                pythonVersion: '3'
                        ],
                        [
                                name         : 'windows-x86_64-cpu',
                                osName       : 'windows',
                                osVersion    : '-server-2016',
                                backend      : 'cpu',
                                sparkVersion : 'spark-1.6',
                                scalaVersion : '2.10',
                                hadoopVersion: 'hadoop-2.7',
                                condaVersion : '4.3.27',
                                pythonVersion: '2'
                        ]
                    ]
                ]
            ]
        }
    }

    void initPipeline() {
        try {
            script.node('linux-x86_64-generic') {
                script.stage('Prepare run') {
                    runCheckout('skymindio')
                }
            }

            for (def m : buildMappings) {
                def mapping = m
                def platforms = mapping.platforms

                script.parallel getBuildStreams(platforms)
            }
        }
        catch (error) {
            if (script.currentBuild.rawBuild.getAction(jenkins.model.InterruptedBuildAction.class) ||
                    error instanceof org.jenkinsci.plugins.workflow.steps.FlowInterruptedException ||
                    error instanceof java.lang.InterruptedException ||
                    (error instanceof hudson.AbortException &&
                            (error?.message?.contains('script returned exit code 143') ||
                                    error?.message?.contains('Queue task was cancelled')))
            ) {
                script.currentBuild.result = 'ABORTED'
            } else {
                script.currentBuild.result = 'FAILURE'
            }

            script.echo "[ERROR] ${error}" +
                    (error.cause ? '\n' + "Cause is ${error.cause}" : '') +
                    (error.stackTrace ? '\n' + 'StackTrace: ' + error.stackTrace.join('\n') : '')
        }
        finally {
//            script.cleanWs deleteDirs: true

            script.notifier.sendSlackNotification jobResult: script.currentBuild.result,
                    checkoutDetails: checkoutDetails, isMember: isMember, testResults: testResults
        }
    }

    private getBuildStreams(platforms) {
        Map streams = [failFast: false]

        for (def p : platforms) {
            def platform = p
            def platformName = platform.get('name')
            def osName = platform.get('osName') ?: script.error('Missing osName argument!')
            def osVersion = platform.get('osVersion') ?: script.error('Missing osVersion argument!')
            String backend = platform.get('backend')

            def pythonVersion = platform.get('pythonVersion')
            def pythonPackageBuild = release ? true : false

            def cudaVersion = backend?.contains('cuda') ? backend.split('-')[1] : ''
            def cudnnVersion = platform.get('cudnnVersion')
            def sparkVersion = platform.get('sparkVersion')
            def scalaVersion = platform.get('scalaVersion')
            def hadoopVersion = platform.get('hadoopVersion')
            def condaVersion = platform.get('condaVersion')

            def scienceLibrariesInstall = platform.get('scienceLibrariesInstall') ?: true
            def staticPackageBuild = release ? true : false
            def buildZeppelin = release ? true : false

            def dockerhubProxy = "docker.ci.skymind.io/"

            def skilVersion
            def skilDockerBaseImageTag = dockerhubProxy + ((cudaVersion && cudnnVersion) ?
                    "nvidia/cuda:${cudaVersion}-cudnn${cudnnVersion}-devel-${osName}${osVersion}" :
                    "${osName}:${osVersion}")
            def skilDockerImageTag = [
                    (cudaVersion && cudnnVersion) ? "cuda${cudaVersion}" : "cpu",
                    sparkVersion.replaceAll('-', ''),
                    (pythonVersion) ? "python${pythonVersion}" : '',
                    "${osName}${osVersion}"
            ].findAll().join('-')

            def streamName = skilDockerImageTag

            /* Create stream body */
            streams["$streamName"] = {
                script.stage(streamName) {
                    script.node(platformName) {
                        try {
                            script.stage('Checkout') {
                                script.checkout script.scm

                                def skilPom = script.readMavenPom(file: 'pom.xml')
                                skilVersion = skilPom?.version

                                // Expose gathered values as environment variables
                                script.env.SKIL_VERSION = skilVersion
                                script.env.SKIL_DOCKER_IMAGE_REVISION_NUMBER = "0.0.1"

                                if (release) {
                                    script.echo "[INFO] Releasing new version of skil-server (v${skilVersion})..."
                                }
                            }

                            if (release) {
                                script.stage('Fetch zeppelin') {
                                    script.dir('zeppelin') {
                                        script.git branch: zeppelinBranchName,
                                                changelog: false,
                                                poll: false,
                                                url: zeppelinGitUrl
                                    }
                                }
                            }

                            script.configFileProvider([
                                script.configFile(
                                        fileId: 'global_mvn_settings_xml',
                                        targetLocation: "${script.env.HOME}/.m2/settings.xml",
                                        variable: ''
                                )
                            ]) {
                                script.withEnv([
                                        "OS_NAME=${osName}",
                                        "OS_VERSION=${osVersion}",
                                        "GIT_COMMIT=${script.env.GIT_COMMIT}",
                                        "RELEASE=${release}",
                                        "SKIL_BASE_IMAGE_NAME=${skilDockerBaseImageTag}",
                                        "SKIL_DOCKER_IMAGE_TAG=${skilDockerImageTag}",
                                        "SKIL_DOCKER_IMAGE_REVISION=${script.env.SKIL_DOCKER_IMAGE_REVISION_NUMBER}-${script.env.GIT_COMMIT[0..7]}",
                                        "CUDA_VERSION=${cudaVersion}",
                                        "CONDA_VERSION=${condaVersion}",
                                        "HADOOP_VERSION=${hadoopVersion}",
                                        "PYTHON_VERSION=${pythonVersion}",
                                        "PYTHON_PACKAGE_BUILD=${pythonPackageBuild}",
                                        "SCALA_VERSION=${scalaVersion}",
                                        "SCIENCE_LIBRARIES_INSTALL=${scienceLibrariesInstall}",
                                        "SPARK_VERSION=${sparkVersion}",
                                        "BUILD_ZEPPELIN=${buildZeppelin}"
                                ]) {
                                    script.stage('Build SKIL and its dependencies') {
                                        script.withCredentials([script.file(credentialsId: 'jenkins-gpg-private-key', variable: 'GPG_KEYRING_PATH')]) {
                                            if (osName in ['centos', 'ubuntu']) {
                                                script.sh """\
                                                    docker-compose \
                                                     -f skil-build/docker/docker-compose.yml \
                                                     run \
                                                     -v \${GPG_KEYRING_PATH}:/home/skil/.gnupg/secring.gpg \
                                                     -v \${HOME}/.m2:/home/skil/.m2 \
                                                     -v \$(pwd):/opt/skil/build \
                                                     --rm \
                                                     skil-build
                                                """
                                            } else {
                                                script.bat "bash -c ./build-skil.sh"
                                            }
                                        }
                                    }

                                    if (release) {
                                        if (osName in ['centos', 'ubuntu']) {
                                            script.stage('Build SKIL docker image') {
                                                script.sh """\
                                                    docker-compose -f skil-distro-parent/skil-distro-docker/docker-compose.yml build skil
                                                """.stripIndent()

                                                if (staticPackageBuild) {
                                                    if (osName == 'centos' && backend == 'cpu') {
                                                        script.sh """\
                                                           docker-compose -f skil-distro-parent/skil-distro-docker/docker-compose.yml \
                                                            run \
                                                            -u root \
                                                            -v \${HOME}/.m2:/root/.m2 \
                                                            -v \$(pwd):/opt/skil/build \
                                                            -e OS_NAME=${osName} \
                                                            -e OS_VERSION=${osVersion} \
                                                            -e STATIC_PACKAGE_BUILD=${staticPackageBuild} \
                                                            -e PYTHON_VERSION=${pythonVersion} \
                                                            -e CUDA_VERSION=${cudaVersion} \
                                                            -e CONDA_VERSION=${condaVersion} \
                                                            -e HADOOP_VERSION=${hadoopVersion} \
                                                            -e SCALA_VERSION=${scalaVersion} \
                                                            -e RELEASE=${release} \
                                                            -e PYTHON_PACKAGE_BUILD=${pythonPackageBuild} \
                                                            -e SCIENCE_LIBRARIES_INSTALL=${scienceLibrariesInstall} \
                                                            --rm \
                                                            --entrypoint='/bin/sh -c /opt/skil/build/build-skil.sh' \
                                                            --workdir=/opt/skil/build \
                                                            skil
                                                        """

                                                        script.sh """\
                                                            docker-compose -f skil-distro-parent/skil-distro-docker/docker-compose.yml build skil
                                                        """.stripIndent()
                                                    } else {
                                                        script.echo "[WARNING] OS is not supported."
                                                    }
                                                }

                                                script.sh "ls -lRa ${buildArtifactsPath}/${osName}"
                                                script.sh 'docker images'
                                                script.sh 'docker inspect skil:${SKIL_VERSION}-${SKIL_DOCKER_IMAGE_TAG}'

                                                // Workaround for archiving artifacts by Jenkins
                                                script.sh """\
                                                    mv ${buildArtifactsPath}/${osName} ${buildArtifactsPath}/${skilDockerImageTag}
                                                """.stripIndent()
                                            }
                                        }

                                        script.stage('Publish artifacts') {
                                            publishArtifacts(osName, osVersion, skilDockerImageTag)

                                            if (osName == 'centos') {
                                                // Tarball upload
                                                publishTarball(skilVersion, backend)
                                            }

                                            if (osName in ['centos', 'ubuntu']) {
                                                def dockerRegistryUrl = "https://docker-ci.skymind.io"
                                                def dockerRegistryCredentialsId = "skymind-docker-registry"
                                                def skilDockerImageName = "skil:${skilVersion}-${skilDockerImageTag}"

                                                script.docker.withRegistry(
                                                        "${dockerRegistryUrl}",
                                                        "${dockerRegistryCredentialsId}"
                                                ) {
                                                    def skilDockerImage = script.docker.image(skilDockerImageName)

                                                    skilDockerImage.push()
                                                }
                                            }
                                        }
                                    } else {
//                                        script.stage('Install test resources') {
//                                            script.dir('skil-test-resources') {
//                                                String installTestResourcesMavenArguments = [
//                                                        mavenBaseCommand,
//                                                        'clean',
//                                                        'install',
//                                                        '-DskipTests',
//                                                        '-Dmaven.test.skip=true',
//                                                        '-Dmaven.javadoc.skip=true'
//                                                ].findAll().join(' ')
//
//                                                if (osName in ['centos', 'ubuntu']) {
//                                                    script.sh """\
//                                                        docker-compose \
//                                                        -f ../skil-build/docker/docker-compose.yml \
//                                                        --project-directory ../skil-build/docker run \
//                                                        -v \${HOME}/.m2:/home/skil/.m2 \
//                                                        -v \$(pwd):/opt/skil/build \
//                                                        --rm \
//                                                        skil-build \
//                                                        sh -c '${installTestResourcesMavenArguments}'
//                                                    """
//                                                } else {
//                                                    script.sh "${installTestResourcesMavenArguments}"
//                                                }
//                                            }
//                                        }
//
//                                        script.stage('Run tests') {
//                                            String runTestsMavenArguments = [
//                                                    mavenBaseCommand,
//                                                    'test',
//                                                    '-P ci',
//                                                    '-P ci-nexus',
//                                                    "-P ${sparkVersion}",
//                                                    "-P modelhistoryserver",
//                                                    '-P test',
//                                                    '-P test-nd4j-native'
//                                            ].findAll().join(' ')
//
//                                            if (osName in ['centos', 'ubuntu']) {
//                                                script.sh """\
//                                                    docker-compose \
//                                                    -f ./skil-build/docker/docker-compose.yml \
//                                                    --project-directory ./skil-build/docker run \
//                                                    -v \${HOME}/.m2:/home/skil/.m2 \
//                                                    -v \$(pwd):/opt/skil/build \
//                                                    --rm \
//                                                    skil-build \
//                                                    sh -c '${runTestsMavenArguments}'
//                                                """
//                                            } else {
//                                                script.sh "${runTestsMavenArguments}"
//                                            }
//                                        }

                                        if (osName == 'centos' && backend == 'cpu' && sparkVersion == 'spark-1.6') {
                                            script.dir('skil-ui-modules/src/main/typescript/dashboard') {
                                                script.stage('Clear cache and build docker image from scratch') {
                                                    script.sh '''\
                                                        docker-compose rm -f
                                                        docker-compose build
                                                    '''.stripIndent()
                                                }

                                                script.stage('SKIL Dashboard Unit Tests') {
                                                    script.sh '''\
                                                        docker-compose run --rm dev yarn run test-teamcity
                                                    '''.stripIndent()
                                                }

                                                script.stage('SKIL Dashboard E2E Tests') {
                                                    script.sh '''\
                                                        docker-compose run --rm dev yarn run e2e-teamcity
                                                    '''.stripIndent()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        finally {
                            def tr = script.junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'

                            testResults.add([
                                    platform   : streamName,
                                    testResults: parseTestResults(tr)
                            ])

                            script.cleanWs deleteDirs: true

                            // FIXME: Workaround to clean workspace
                            script.dir("${script.env.WORKSPACE}@tmp") {
                                script.deleteDir()
                            }
                            script.dir("${script.env.WORKSPACE}@script") {
                                script.deleteDir()
                            }
                            script.dir("${script.env.WORKSPACE}@script@tmp") {
                                script.deleteDir()
                            }
                        }
                    }
                }
            }
        }

        streams
    }

    private void publishTarball(String skilVersion, String backend) {
        String repoUrl = 'https://nexus-ci.skymind.io/repository/tarballs'
        String repoPath
        def tarballs = script.findFiles glob: "skil-distro-parent/skildistro/target/*.tar.gz"

        switch (branchName) {
            case ~releaseBranchPattern:
            case 'master':
                repoPath = [
                    'releases',
                    skilVersion,
                    backend
                ].findAll().join('/')
            break
            default:
                repoPath = [
                    'snapshots',
                    skilVersion,
                    backend
                ].findAll().join('/')
            break
        }

        script.withCredentials([
                script.usernameColonPassword(
                        credentialsId: 'skymind-docker-registry',
                        variable: 'RPM_REPO_CREDS'
                )
        ]) {
            for (def tr in tarballs) {
                def tarball = tr
                def tarballName = tarball.name
                def tarballPath = tarball.path

                script.sh "curl --user \${RPM_REPO_CREDS} --upload-file ./${tarballPath} ${repoUrl}/${repoPath}/${tarballName}"
            }
        }
    }

    private void publishArtifacts(String osName, String osVersion, String skilDockerImageTag) {
        def publishParameters = getPublishParameters(osName, osVersion, skilDockerImageTag)
        def artifacts = script.findFiles glob: publishParameters.searchPattern
        def repoUrl = publishParameters.repoUrl
        def repoPath = publishParameters.repoPath

        script.withCredentials([
                script.usernameColonPassword(
                        credentialsId: 'skymind-docker-registry',
                        variable: 'RPM_REPO_CREDS'
                )
        ]) {
            for (def art in artifacts) {
                def artifact = art
                String artifactName = artifact.name
                String artifactPath = artifact.path
                String uploadUrl = [
                        repoUrl,
                        repoPath,
                        artifactName
                ].findAll().join('/')

                if (publishParameters.packageExtension == 'deb') {
                    script.sh "curl --user \${RPM_REPO_CREDS} -X POST -H \"Content-Type: multipart/form-data\" --data-binary \"@${artifactPath}\" ${repoUrl}"
                } else {
                    script.sh "curl --user \${RPM_REPO_CREDS} --upload-file ./${artifactPath} ${uploadUrl}"
                }
            }
        }
    }

    private getPublishParameters(String osName, String osVersion, String skilDockerImageTag) {
        String baseArch = 'x86_64'
        String platform = [osName, osVersion].join('-')
        def publishParameters = [:]

        switch (platform) {
            case ~/^centos.*/:
                String repoPath

                switch (branchName) {
                    case ~releaseBranchPattern:
                    case 'master':
                        repoPath = [
                                osName,
                                '7',
                                'latest',
                                'os', // a.k.a base
                                baseArch,
                                'Packages'
                        ].findAll().join('/')
                        break
                    default:
                        repoPath = [
                                osName,
                                '7',
                                'dev',
                                'os', // a.k.a base
                                baseArch,
                                'Packages'
                        ].findAll().join('/')
                        break
                }

                publishParameters.put('packageExtension', 'rpm')
                publishParameters.put('repoUrl', 'https://nexus-ci.skymind.io/repository/rpms')
                publishParameters.put('repoPath', repoPath)
                publishParameters.put('searchPattern', "${buildArtifactsPath}/${skilDockerImageTag}/*.rpm")
                break
            case 'ubuntu-16.04':
                publishParameters.put('packageExtension','deb')
                publishParameters.put('repoUrl', 'https://nexus-ci.skymind.io/repository/deb-xenial/')
                publishParameters.put('repoPath', '')
                publishParameters.put('searchPattern', "${buildArtifactsPath}/${skilDockerImageTag}/*.deb")
                break
            case 'ubuntu-18.04':
                publishParameters.put('packageExtension','deb')
                publishParameters.put('repoUrl', 'https://nexus-ci.skymind.io/repository/deb-bionic/')
                publishParameters.put('repoPath', '')
                publishParameters.put('searchPattern', "${buildArtifactsPath}/${skilDockerImageTag}/*.deb")
                break
            case ~/^windows.*/:
                String repoPath

                switch (branchName) {
                    case ~releaseBranchPattern:
                    case 'master':
                        repoPath = [
                                osName,
                                'server-2016',
                                'latest',
                                'os', // a.k.a base
                                baseArch,
                                'Packages'
                        ].findAll().join('/')
                        break
                    default:
                        repoPath = [
                                osName,
                                'server-2016',
                                'dev',
                                'os', // a.k.a base
                                baseArch,
                                'Packages'
                        ].findAll().join('/')
                        break
                }

                publishParameters.put('packageExtension', 'zip')
                publishParameters.put('repoUrl', 'https://nexus-ci.skymind.io/repository/tarballs')
                publishParameters.put('repoPath', repoPath)
                publishParameters.put('searchPattern', "${buildArtifactsPath}/*.zip")
                break
            default:
                script.error('Unsupported OS name!')
                break
        }

        return publishParameters
    }
}
