package skymind.pipelines.projects

class Libnd4jProject extends Project {
    private final String libnd4jTestsFilter

    static {
        /* Override default platforms */
        defaultPlatforms = [
                [name: 'android-arm', backend: 'cpu'],
                [name: 'android-arm64', backend: 'cpu'],
                [name: 'android-x86', backend: 'cpu'],
                [name: 'android-x86_64', backend: 'cpu'],

                [name: 'ios-arm64', backend: 'cpu'],
                [name: 'ios-x86_64', backend: 'cpu'],

                [name: 'linux-ppc64le', backend: 'cpu'],
                [name: 'linux-ppc64le', backend: 'cuda-8.0'],
                [name: 'linux-ppc64le', backend: 'cuda-9.0'],
                [name: 'linux-ppc64le', backend: 'cuda-9.1'],

                [name: 'linux-x86_64', backend: 'cpu'],
                [name: 'linux-x86_64', backend: 'cpu', cpuExtension: 'avx2'],
                [name: 'linux-x86_64', backend: 'cpu', cpuExtension: 'avx512'],
                [name: 'linux-x86_64', backend: 'cuda-8.0'],
                [name: 'linux-x86_64', backend: 'cuda-9.0'],
                [name: 'linux-x86_64', backend: 'cuda-9.1'],

                [name: 'macosx-x86_64', backend: 'cpu'],
                [name: 'macosx-x86_64', backend: 'cpu', cpuExtension: 'avx2'],
                /*
                     FIXME: avx512 required Xcode 9.2 to be installed on Mac slave,
                     at the same time for CUDA - Xcode 8 required,
                     which means that we can't enable avx512 builds at the moment
                  */
//                [name: 'macosx-x86_64', backend: 'cpu', cpuExtension: 'avx512'],
                [name: 'macosx-x86_64', backend: 'cuda-8.0'],
                [name: 'macosx-x86_64', backend: 'cuda-9.0'],
                [name: 'macosx-x86_64', backend: 'cuda-9.1'],

                [name: 'windows-x86_64', backend: 'cpu'],
                [name: 'windows-x86_64', backend: 'cpu', cpuExtension: 'avx2'],
                /* FIXME: avx512 */
//                [name: 'windows-x86_64', backend: 'cpu', cpuExtension: 'avx512'],
                [name: 'windows-x86_64', backend: 'cuda-8.0'],
                [name: 'windows-x86_64', backend: 'cuda-9.0'],
                [name: 'windows-x86_64', backend: 'cuda-9.1']
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
        pipelineWrapper {
            if (branchName.contains(releaseBranchPattern)) {
                script.stage("Perform Release") {
                    getReleaseParameters()
                }
            }

            script.stage("Test and Build") {
                script.parallel buildStreams
            }
        }
    }

    private getBuildStreams() {
        Map streams = [failFast: false]

        for (Map pltm : platforms) {
            Map platform = pltm

            String platformName = platform.get('name')
            String backend = platform.get('backend')
            String cpuExtension = platform.get('cpuExtension')

            String streamName = [platformName, backend, cpuExtension].findAll().join('-')

            /* Create stream body */
            streams["$streamName"] = {
                script.node(streamName) {
                    Boolean isUnix = script.isUnix()
                    String separator = isUnix ? '/' : '\\'
                    String wsFolderName = 'workspace' +
                            separator +
                            [projectName, script.env.BRANCH_NAME, streamName].join('_').replaceAll('/', '_')

                    /* Redefine default workspace to fix Windows path length limitation */
                    script.ws(wsFolderName) {
                        try {
                            script.stage('Checkout') {
                                script.deleteDir()

                                script.dir(projectName) {
                                    script.checkout script.scm
                                }
                            }

                            script.dir(projectName) {
                                if (platformName.contains('ppc64') || platformName.contains('linux') && backend.contains('cuda')) {
                                    /* Get docker container configuration */
                                    Map dockerConf = script.pipelineEnv.getDockerConfig(streamName)

                                    String dockerImageName = dockerConf['image'] ?:
                                            script.error('Docker image name is missing.')
                                    String dockerImageParams = dockerConf?.params

                                    script.docker.image(dockerImageName).inside(dockerImageParams) {
                                        if (branchName.contains(releaseBranchPattern)) {
                                            script.stage("Prepare for Release") {
                                                setupEnvForRelease()
                                            }
                                        }

                                        if (!branchName.contains(releaseBranchPattern)) {
                                            script.stage('Test') {
                                                /* Run tests only for CPU backend, while CUDA tests are under development */
                                                if (backend == 'cpu') {
                                                    runtTests(platformName, backend)
                                                }
                                            }
                                        }

                                        script.stage('Build') {
                                            runStageLogic('build', platformName, backend, cpuExtension)
                                        }
                                    }
                                } else {
                                    if (branchName.contains(releaseBranchPattern)) {
                                        script.stage("Prepare for Release") {
                                            setupEnvForRelease()
                                        }
                                    }

                                    if (!branchName.contains(releaseBranchPattern)) {
                                        script.stage('Test') {
                                            /* Run tests only for CPU backend, while CUDA tests are under development */
                                            if (backend == 'cpu') {
                                                runtTests(platformName, backend)
                                            }
                                        }
                                    }

                                    script.stage('Build') {
                                        runStageLogic('build', platformName, backend, cpuExtension)
                                    }
                                }
                            }
                        }
                        finally {
                            /* FIXME: cleanWs step doesn't clean custom workspace, whereas deleteDir does */
                            script.deleteDir()
                        }
                    }
                }
            }
        }

        streams
    }

    private void runtTests(String platformName, String backend) {
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

        switch (platformName) {
            case ['linux-ppc64le', 'windows-x86_64']:
                break
            case ~/^android.*$/:
            case ['linux-x86_64', 'linux-x86_64-generic']:
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
            script.echo "[INFO] Running tests on ${platformName}, for ${backend} backend"

            script."${shell}" "${testCommand}"

            /* Archiving test results */
            script.junit testResults: "**/${backend}_test_results.xml", allowEmptyResults: true

            script.echo "[INFO] Finished to run tests on ${platformName}, for ${backend} backend"
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
     * @param platformName
     * @param backend
     * @param cpuExtension
     */
    private void runStageLogic(String stageName, String platformName, String backend, String cpuExtension) {
        String mvnCommand

        /* Build libnd4j for CPU backend */
        if (backend == 'cpu') {
            mvnCommand = getMvnCommand(stageName, cpuExtension, [
                    "-Dlibnd4j.platform=${platformName}",
                    (cpuExtension) ? "-Dlibnd4j.extension=${cpuExtension}" : '',
                    (platformName.contains('macosx') || platformName.contains('ios')) ?
                            "-Dmaven.repo.local=${script.env.WORKSPACE}/${script.pipelineEnv.localRepositoryPath}" :
                            ''
            ])

            script.echo "[INFO] ${stageName.capitalize()}ing libnd4j ${backend} backend"  + (cpuExtension ? " with ${cpuExtension} extension" : '')
            script.mvn "$mvnCommand"
        }
        /* Build libnd4j for CUDA backend */
        else {
            String cudaVersion = backend.tokenize('-')[1]

            mvnCommand = getMvnCommand(stageName, cpuExtension, [
                    "-Dlibnd4j.platform=${platformName}",
                    "-Dlibnd4j.cuda=${cudaVersion}",
                    (branchName != 'master' && !branchName.contains(releaseBranchPattern)) ? "-Dlibnd4j.compute=30" : '',
                    (platformName.contains('macosx') || platformName.contains('ios')) ?
                            "-Dmaven.repo.local=${script.env.WORKSPACE}/${script.pipelineEnv.localRepositoryPath}" :
                            ''
            ])

            script.echo "[INFO] ${stageName.capitalize()}ing libnd4j ${backend} backend"
            script.mvn "$mvnCommand"
        }
    }

    protected String getMvnCommand(String stageName, String cpuExtension, List mvnArguments = []) {
        Boolean unixNode = script.isUnix()
        String devtoolsetVersion = cpuExtension ? '6' : '4'

        switch (stageName) {
            case 'build':
                if (unixNode) {
                    return [
                            "if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-${devtoolsetVersion}/enable; fi;",
                            /* Pipeline withMaven step requires this line if it runs in Docker container */
                            'export PATH=$MVN_CMD_DIR:$PATH &&',
                            'mvn -U -B',
                            'clean',
                            (branchName == 'master' || branchName.contains(releaseBranchPattern)) ? 'deploy' : 'install',
                            "-Dlocal.software.repository=${script.pipelineEnv.mvnProfileActivationName}",
                            (releaseApproved) ? "-DstagingRepositoryId=${script.env.STAGING_REPOSITORY}" : '',
                            (releaseApproved) ? "-DperformRelease" : '',
                            (releaseApproved) ? "-P staging" : ''
                    ].plus(mvnArguments).findAll().join(' ')
                } else {
                    return [
                            'vcvars64.bat',
                            '&&',
                            'bash -c',
                            '"' + 'export PATH=$PATH:/c/msys64/mingw64/bin &&',
                            'mvn -U -B',
                            'clean',
                            (branchName == 'master' || branchName.contains(releaseBranchPattern)) ? 'deploy' : 'install',
                            "-Dlocal.software.repository=${script.pipelineEnv.mvnProfileActivationName}",
                            (releaseApproved) ? "-DstagingRepositoryId=${script.env.STAGING_REPOSITORY}" : '',
                            (releaseApproved) ? "-DperformRelease" : '',
                            (releaseApproved) ? "-P staging" : '',
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
