package skymind.pipelines.projects

import groovy.transform.InheritConstructors

@InheritConstructors
class Nd4jProject extends Project {
    private final String lockableResourceName = "nd4jTestAndBuild-${branchName}"
    private String mavenLocalRepositoryPath

    /* Override default platforms */
    static {
        defaultPlatforms = [
                [backends  : ['cpu'],
                 compillers: [],
                 name      : 'android-x86'],

                [backends  : ['cpu'],
                 compillers: [],
                 name      : 'android-arm'],

                [backends  : ['cpu'],
                 compillers: [],
                 name      : 'linux-ppc64le'],

                /*
                    FIXME: Disable CUDA 9.1 build because of mismatching dependencies version.
                 */
//                [backends     : ['cpu', 'cuda-8.0', 'cuda-9.0', 'cuda-9.1'],
                [backends  : ['cpu', 'cuda-8.0', 'cuda-9.0'],
                 cpuExtensions: ['avx2', 'avx512'],
                 compillers   : [],
                 name         : 'linux-x86_64'],

                /*
                    FIXME: Disable Mac builds at all, because of Mac slave instability.
                 */
//                [backends     : ['cpu'],
//                 cpuExtensions: ['avx2'],
//                 compillers   : [],
//                 name         : 'macosx-x86_64'],

                [backends  : ['cpu', 'cuda-8.0', 'cuda-9.0'],
                 cpuExtensions: ['avx2'],
                 compillers: [],
                 name      : 'windows-x86_64']
        ]
    }

    void initPipeline() {
        script.node('master') {
            pipelineWrapper {
                script.lock(resource: lockableResourceName, inversePrecedence: true) {
                    script.stage("Test and Build") {
                        script.milestone()
                        script.parallel buildStreams
                    }
                }
            }
        }
    }

    private getBuildStreams() {
        Map streams = [failFast: false]

        for (Map pltm : platforms) {
            Map platform = pltm
            String platformName = platform.name
            List backends = platform.backends
            List compilers = platform.compilers
            List cpuExtensions = platform.cpuExtensions

            for (List bckd : backends) {
                String backend = bckd
                String streamName = ["${platformName}", "${backend}"].findAll().join('-')
                /* Set add steam to build name */
                script.pipelineEnv.buildDisplayName.push("${streamName}")

                /* Create stream body */
                streams["$streamName"] = {
                    script.node(platformName) {
                        Boolean isUnix = script.isUnix()
                        String separator = isUnix ? '/' : '\\'
                        String wsFolderName = 'workspace' +
                                separator +
                                [projectName, script.env.BRANCH_NAME].join('_').replaceAll('/', '_')
                        mavenLocalRepositoryPath = "${wsFolderName}/${script.pipelineEnv.localRepositoryPath}"

                        /* Redefine default workspace to fix Windows path length limitation */
                        script.ws(wsFolderName) {
                            script.dir(streamName) {
                                script.deleteDir()

                                script.stage('Set up environment') {
                                    script.dir(projectName) {
                                        script.unstash 'sourceCode'

                                        def pom = projectObjectModel
                                        projectVersion = pom?.version
                                    }
                                }

                                script.dir(projectName) {
                                    /* Get docker container configuration */
                                    Map dockerConf = script.pipelineEnv.getDockerConfig(streamName)
                                    /*
                                        Mount point of required folders for Docker container.
                                        Because by default Jenkins mounts current working folder in Docker container,
                                        we need to add custom mount.
                                    */
                                    String mavenLocalRepositoryMount = "-v ${mavenLocalRepositoryPath}:" +
                                            "/home/jenkins/${script.pipelineEnv.localRepositoryPath}:z"

                                    if (dockerConf) {
                                        String dockerImageName = dockerConf['image'] ?:
                                                script.error('Docker image name is missing.')
                                        String dockerImageParams = [
                                                dockerConf?.params, mavenLocalRepositoryMount
                                        ].findAll().join(' ')

                                        script.docker.image(dockerImageName).inside(dockerImageParams) {
                                            script.stage('Build') {
                                                runBuild(platformName, backend, cpuExtensions)
                                            }
                                        }
                                    } else {
                                        script.stage('Build') {
                                            runBuild(platformName, backend, cpuExtensions)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        streams
    }

    private void runBuild(String platform, String backend, List cpuExtensions) {
        Boolean unixNode = script.isUnix()
        String shell = unixNode ? 'sh' : 'bat'

        script.isVersionReleased(projectName, projectVersion)
        script.setProjectVersion(projectVersion, true)

        for (String sclVer : scalaVersions) {
            String scalaVersion = sclVer
            String mvnCommand

            script.echo "[INFO] Setting Scala version to: $scalaVersion"

            String updateScalaCommand = [
                    (unixNode ? 'bash' : "\"C:\\Program Files\\Git\\bin\\bash.exe\" -c"),
                    (unixNode ? "./change-scala-versions.sh $scalaVersion" :
                            "\"./change-scala-versions.sh $scalaVersion\"")
            ].findAll().join(' ')

            script."$shell" "$updateScalaCommand"

            if (backend == 'cpu') {
                /* Nd4j build with libn4j CPU backend and specific extension */
                if (cpuExtensions) {
                    for (String item : cpuExtensions) {
                        String cpuExtension = item

                        mvnCommand = getMvnCommand("build", true, [
                                '-P libnd4j-assembly',
                                "-Djavacpp.extension=${cpuExtension}",
                                (platform in ['linux-x86_64', 'android-arm', 'android-x86']) ?
                                        '-DprotocCommand=protoc' :
                                        '',
                                '-pl ' +
                                        '\'' +
                                        '!nd4j-backends/nd4j-backend-impls/nd4j-cuda,' +
                                        '!nd4j-backends/nd4j-backend-impls/nd4j-cuda-platform,' +
                                        '!nd4j-backends/nd4j-tests' +
                                        '\''
                        ])

                        script.echo "[INFO] Building nd4j ${backend} backend with " +
                                "Scala ${scalaVersion} versions and ${cpuExtension} extension"

                        script.mvn "$mvnCommand"
                    }
                }

                /* Nd4j build with libn4j CPU backend */
                mvnCommand = getMvnCommand("build", false, [
                        '-P libnd4j-assembly',
                        (platform in ['android-x86', 'android-arm']) ? "-Djavacpp.platform=${platform}" : '',
                        (platform.contains('windows')) ? '-s ${MAVEN_SETTINGS}' : '',
                        (platform in ['linux-x86_64', 'android-arm', 'android-x86']) ? '-DprotocCommand=protoc' : '',
                        '-pl ' +
                                '\'' +
                                '!nd4j-backends/nd4j-backend-impls/nd4j-cuda,' +
                                '!nd4j-backends/nd4j-backend-impls/nd4j-cuda-platform,' +
                                '!nd4j-backends/nd4j-tests' +
                                '\''
                ])

                script.echo "[INFO] Building nd4j ${backend} backend with Scala ${scalaVersion} versions"
            }
            /* Nd4j build with libn4j CUDA backend */
            else {
                String cudaVersion = backend.tokenize('-')[1]

                script.echo "[INFO] Setting CUDA version to: $cudaVersion"

                String updateCudaCommand = [
                        (unixNode ? 'bash' : "\"C:\\Program Files\\Git\\bin\\bash.exe\" -c"),
                        (unixNode ? "./change-cuda-versions.sh $cudaVersion" :
                                "\"./change-cuda-versions.sh $cudaVersion\"")
                ].join(' ')

                script."$shell" updateCudaCommand

                mvnCommand = getMvnCommand("build", false, [
                        (platform.contains('windows')) ? '-s ${MAVEN_SETTINGS}' : '',
                        '-P libnd4j-assembly',
                        (platform in ['linux-x86_64', 'android-arm', 'android-x86']) ? '-DprotocCommand=protoc' : ''
                ])

                script.echo "[INFO] Building nd4j with CUDA ${cudaVersion} and Scala ${scalaVersion} versions"
            }

            script.mvn "$mvnCommand"
        }
    }

    protected String getMvnCommand(String stageName, Boolean isCpuWithExtension, List mvnArguments = []) {
        Boolean unixNode = script.isUnix()
        String devtoolsetVersion = isCpuWithExtension ? '6' : '4'

        switch (stageName) {
            case 'build':
                if (unixNode) {
                    return [
                            "if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-${devtoolsetVersion}/enable; fi;",
                            /* Pipeline withMaven step requires this line if it runs in Docker container */
                            'export PATH=$MVN_CMD_DIR:$PATH &&',
                            'mvn -U -B',
                            'clean',
                            branchName == 'master' ? 'deploy' : 'install',
                            '-P trimSnapshots',
                            "-Dlocal.software.repository=${script.pipelineEnv.mvnProfileActivationName}",
                            '-Dmaven.test.skip=true'
                    ].plus(mvnArguments).findAll().join(' ')
                } else {
                    return [
                            'vcvars64.bat',
                            '&&',
                            'bash -c',
                            '"' + 'export PATH=$PATH:/c/msys64/mingw64/bin &&',
                            'mvn -U -B',
                            'clean',
                            branchName == 'master' ? 'deploy' : 'install',
                            '-P trimSnapshots',
                            "-Dlocal.software.repository=${script.pipelineEnv.mvnProfileActivationName}",
                            /* Workaround for Windows which doesn't honour withMaven options */
                            "-Dmaven.repo.local=${script.pipelineEnv.localRepositoryPath}",
                            '-Dmaven.test.skip=true'
                    ].plus(mvnArguments).findAll().join(' ') + '"'
                }
                break
            default:
                throw new IllegalArgumentException('Stage is not supported yet')
                break
        }
    }
}