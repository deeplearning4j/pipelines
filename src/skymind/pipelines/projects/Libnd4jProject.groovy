package skymind.pipelines.projects

class Libnd4jProject extends Project {
    private final String libnd4jTestsFilter

    static {
        /* Override default platforms */
        defaultPlatforms = [
                [backends  : ['cpu'],
                 compillers: [],
                 name      : 'android-arm'],

                [backends  : ['cpu'],
                 compillers: [],
                 name      : 'android-arm64'],

                [backends  : ['cpu'],
                 compillers: [],
                 name      : 'android-x86'],

                [backends  : ['cpu'],
                 compillers: [],
                 name      : 'android-x86_64'],

                /*
                    FIXME: ppc64le slave is unstable at the moment,
                    when number of parallel threads more than number of CPUs OOM-killer kills all Docker containers.
                    Because of that CUDA couldn't be enabled at the moment.
                 */
//                [backends  : ['cpu', 'cuda-8.0', 'cuda-9.0', 'cuda-9.1'],
                [backends  : ['cpu'],
                 compillers: [],
                 name      : 'linux-ppc64le'],

                [backends     : ['cpu', 'cuda-8.0', 'cuda-9.0', 'cuda-9.1'],
                 /* Empty element was added to build for CPU without extension */
                 cpuExtensions: ['', 'avx2', 'avx512'],
                 compillers   : [],
                 name         : 'linux-x86_64'],

                [backends  : ['cpu'],
                 compillers: [],
                 name      : 'ios-arm64'],

                [backends  : ['cpu'],
                 compillers: [],
                 name      : 'ios-x86_64'],

                [backends     : ['cpu', 'cuda-8.0', 'cuda-9.0', 'cuda-9.1'],
                 /*
                     FIXME: avx512 required Xcode 9.2 to be installed on Mac slave,
                     at the same time for CUDA - Xcode 8 required,
                     which means that we can't enable avx512 builds at the moment
                  */
//                 cpuExtensions: ['', 'avx2', 'avx512'],
                 /* Empty element was added to build for CPU without extension */
                 cpuExtensions: ['', 'avx2'],
                 compillers   : [],
                 name         : 'macosx-x86_64'],

                [backends     : ['cpu', 'cuda-8.0', 'cuda-9.0', 'cuda-9.1'],
                 /* Empty element was added to build for CPU without extension */
                 cpuExtensions: ['', 'avx2'],
                 compillers   : [],
                 name         : 'windows-x86_64']
        ]

        /* Gitter endpoint url for devlibnd4j room */
        gitterEndpointUrl = 'https://webhooks.gitter.im/e/97334c78c3f82c5ad21e'
    }

    Libnd4jProject(script, String projectName, Map jobConfig) {
        super(script, projectName, jobConfig)
        /* Get filters for Google tests, provided by developer in Jenkinsfile */
        libnd4jTestsFilter = jobConfig?.getAt('libnd4jTestsFilter')
    }

    void initPipeline() {
        script.node('master') {
            pipelineWrapper {
                script.stage("Test and Build") {
                    script.parallel buildStreams
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
            /* List with empty element was added to build for CPU without extension */
            List cpuExtensions = platform.cpuExtensions ?: ['']

            for (List bckd : backends) {
                String backend = bckd
                String streamName = ["${platformName}", "${backend}"].findAll().join('-')
                /* Set add steam to build name */
//                script.pipelineEnv.buildDisplayName.push("${streamName}")

                /* Create stream body */
                streams["$streamName"] = {
                    script.node(platformName) {
                        try {
                            Boolean isUnix = script.isUnix()
                            String separator = isUnix ? '/' : '\\'
                            String wsFolderName = 'workspace' +
                                    separator +
                                    [projectName, script.env.BRANCH_NAME, streamName].join('_').replaceAll('/', '_')

                            /* Redefine default workspace to fix Windows path length limitation */
                            script.ws(wsFolderName) {
                                script.stage('Checkout') {
                                    script.deleteDir()

                                    script.dir(projectName) {
                                        script.checkout script.scm
                                    }
                                }

                                script.dir(projectName) {
                                    /* Get docker container configuration */
                                    Map dockerConf = script.pipelineEnv.getDockerConfig(streamName)

                                    if (dockerConf) {
                                        String dockerImageName = dockerConf['image'] ?:
                                                script.error('Docker image name is missing.')
                                        String dockerImageParams = dockerConf?.params

                                        script.docker.image(dockerImageName).inside(dockerImageParams) {
                                            script.stage('Test') {
                                                /* Run tests only for CPU backend, while CUDA tests are under development */
                                                if (backend == 'cpu') {
                                                    runtTests(platformName, backend)
                                                }
                                            }

                                            script.stage('Build') {
                                                runBuild(platformName, backend, cpuExtensions)
                                            }
                                        }
                                    } else {
                                        script.stage('Test') {
                                            /* Run tests only for CPU backend, while CUDA tests are under development */
                                            if (backend == 'cpu') {
                                                runtTests(platformName, backend)
                                            }
                                        }

                                        script.stage('Build') {
                                            runBuild(platformName, backend, cpuExtensions)
                                        }
                                    }
                                }
                            }
                        }
                        finally {
                            script.deleteDir()
                        }
                    }
                }
            }
        }

        streams
    }

    private void runtTests(String platform, String backend) {
        Boolean unixNode = script.isUnix()
        String shell = unixNode ? 'sh' : 'bat'
        String separator = unixNode ? '/' : '\\'
        String testFolderName = "tests_${backend}"
        String testCommand = [
                "cd ${testFolderName}",
                'cmake -G "Unix Makefiles"',
                'make -j5',
                "layers_tests${separator}runtests --gtest_output=\"xml:cpu_test_results.xml\"" +
                        /* Add possibility to provide additional params to Google Tests */
                        (libnd4jTestsFilter ? ' ' + libnd4jTestsFilter : '')
        ].join(' && ')

        switch (platform) {
            case ['linux-ppc64le', 'windows-x86_64']:
                break
            case ~/^android.*$/:
            case ['linux-x86_64']:
                testCommand = """\
                    if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable; fi
                    ${testCommand}
                """.stripIndent()
                break
            case ~/^ios.*$/:
            case ['macosx-x86_64']:
                /* export CC, CXX, CPP, LD required for switching compiler from clang (default for Mac) to gcc */
                testCommand = """\
                    export CC=\$(ls -1 /usr/local/bin/gcc-? | head -n 1)
                    export CXX=\$(ls -1 /usr/local/bin/g++-? | head -n 1)
                    ${testCommand}
                """.stripIndent()
                break
            default:
                throw new IllegalArgumentException("Test logic for provided platform is not supported yet.")
                break
        }

        if (script.fileExists("${testFolderName}")) {
            /* Run tests */
            script.echo "[INFO] Running tests on ${platform}, for ${backend} backend"

            script."${shell}" "${testCommand}"

            /* Archiving test results */
            script.junit testResults: "**/${backend}_test_results.xml", allowEmptyResults: true

            script.echo "[INFO] Finished to run tests on ${platform}, for ${backend} backend"
        } else {
            script.error "${testFolderName} was not found."
        }
    }

    /**
     * Build libn4j project
     * All artifacts that been built not for master branch will be installed in local nexus repository
     * If build is for master branch uploaded artifact name will have following format:
     * <groupId>.<artifactId>-<version>-<classifier>
     * Example: org.nd4j.libnd4j-1.0.0-SNAPSHOT-android-x86-cuda-8.0
     *
     * @param platform
     * @param backend
     * @param cpuExtensions
     */
    private void runBuild(String platform, String backend, List cpuExtensions) {
        String mvnCommand

        /* Build libnd4j for CPU backend */
        if (backend == 'cpu') {
            for (String item : cpuExtensions) {
                String cpuExtension = item

                mvnCommand = getMvnCommand('build', true, [
                        "-Dlibnd4j.platform=${platform}",
                        (cpuExtension) ? "-Dlibnd4j.extension=${cpuExtension}" : '',
                        (platform.contains('macosx') || platform.contains('ios')) ?
                                "-Dmaven.repo.local=${script.env.WORKSPACE}/${script.pipelineEnv.localRepositoryPath}" :
                                ''
                ])

                script.echo "[INFO] Building libnd4j ${backend} backend with ${cpuExtension} extension"

                script.mvn "$mvnCommand"
            }
        }
        /* Build libnd4j for CUDA backend */
        else {
            String cudaVersion = backend.tokenize('-')[1]

            mvnCommand = getMvnCommand('build', false, [
                    "-Dlibnd4j.platform=${platform}",
                    "-Dlibnd4j.cuda=${cudaVersion}",
                    (branchName != 'master') ? "-Dlibnd4j.compute=30" : '',
                    (platform.contains('macosx') || platform.contains('ios')) ?
                            "-Dmaven.repo.local=${script.env.WORKSPACE}/${script.pipelineEnv.localRepositoryPath}" :
                            ''
            ])

            script.echo "[INFO] Building libnd4j ${backend} backend"

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
                            /* Force to build in three threads */
//                            'export MAKEJ=3 &&',
                            'mvn -U',
                            'clean',
                            branchName == 'master' ? 'deploy' : 'install',
                            "-Dlocal.software.repository=${script.pipelineEnv.mvnProfileActivationName}"
                    ].plus(mvnArguments).findAll().join(' ')
                } else {
                    return [
                            'vcvars64.bat',
                            '&&',
                            'bash -c',
                            '"' + 'export PATH=$PATH:/c/msys64/mingw64/bin &&',
                            /* Force to build in three threads */
//                            'export MAKEJ=3 &&',
                            'mvn -U -B',
                            'clean',
                            branchName == 'master' ? 'deploy' : 'install',
                            "-Dlocal.software.repository=${script.pipelineEnv.mvnProfileActivationName}",
                            /* Workaround for Windows which doesn't honour withMaven options */
                            '-s ${MAVEN_SETTINGS}',
                            "-Dmaven.repo.local=" +
                                    "${script.env.WORKSPACE.replaceAll('\\\\', '/')}/" +
                                    "${script.pipelineEnv.localRepositoryPath}"
                    ].plus(mvnArguments).findAll().join(' ') + '"'
                }
                break
            default:
                throw new IllegalArgumentException('Stage is not supported yet')
                break
        }
    }
}
