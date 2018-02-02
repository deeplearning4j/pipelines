package skymind.pipelines.projects

class Libnd4jProject extends Project {
    private final List artifacts = []
    private final String projectGroupId = 'org.nd4j'
    private final String projectVersion = script.env.PROJECT_VERSION ?: '0.9.2-SNAPSHOT'
    private final List artifactNamesPattern = ['blas', 'blasbuild', 'include']
    private final String libnd4jTestsFilter

    /* Overwrite default platforms */
    static {
        platforms = [
                [backends  : ['cpu'],
                 compillers: [],
                 name      : 'android-x86'],

                [backends  : ['cpu'],
                 compillers: [],
                 name      : 'android-arm'],

                [backends  : ['cpu'],
                 compillers: [],
                 name      : 'linux-ppc64le'],

                [backends  : ['cpu', 'cuda-8.0', 'cuda-9.0'],
                 compillers: [],
                 name      : 'linux-x86_64'],

//                        [backends  : ['cpu'],
//                         compillers: [],
//                         name      : 'macosx-x86_64'],

                [backends  : ['cpu', 'cuda-8.0', 'cuda-9.0'],
                 compillers: [],
                 name      : 'windows-x86_64']
        ]
    }

    Libnd4jProject(script, String projectName, Map jobConfig) {
        super(script, projectName, jobConfig)
        /* Get filters for Google tests, provided by developer in Jenkinsfile */
        libnd4jTestsFilter = jobConfig?.getAt('libnd4jTestsFilter')
    }

    @NonCPS
    protected void setBuildParameters() {
        super.setBuildParameters([
                script.parameters([
                        script.string(
                        defaultValue: '0.9.2-SNAPSHOT',
                        description: 'Libnd4j component version',
                        name: 'PROJECT_VERSION'
                ),
//                [$class: 'com.cwctravel.hudson.plugins.extended_choice_parameter.ExtendedChoiceParameterDefinition'],
                        script.string(
                        defaultValue: '',
                        description: '''\
                                CUDA build parameters that will be added to defaults (it doesn\'t apply for CPU builds).
    
                                Defauls:
                                linux: -c cuda -v [8.0, 9.0]
                                macosx: -c cuda -v [8.0, 9.0]
                                windows: -c cuda -v [8.0, 9.0]
                        '''.stripMargin().stripIndent(),
                        name: 'CUDA_BUILD_PARAMETERS'
                )
            ])
        ])
    }

    void initPipeline() {
        script.node('master') {
            pipelineWrapper {
                script.lock(resource: "libnd4jTestAndBuild-${branchName}", inversePrecedence: true) {
                    script.stage("Test and Build") {
                        script.milestone()
                        script.parallel buildStreams
                    }
                }

                /* At the moment, we need to publish only master artifacts to local nexus */
                if (branchName == 'master') {
                    script.stage('Publish artifacts') {
                        script.milestone()
                        runPublish(artifacts)
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

                        /* Redefine default workspace to fix Windows path length limitation */
                        script.ws(wsFolderName) {
                            script.dir(streamName) {
                                script.deleteDir()

                                String mavenLocalRepositoryPath = script.pipelineEnv.jenkinsDockerM2Mount

                                script.dir(projectName) {
                                    script.unstash 'sourceCode'

                                    Map dockerConf = script.pipelineEnv.getDockerConfig(streamName)

                                    if (dockerConf) {
                                        String createFoldersCommand = [
                                                'mkdir -p',
                                                script.pipelineEnv.jenkinsDockerSbtFolder,
                                                mavenLocalRepositoryPath
                                        ].findAll().join(' ')

                                        script.sh createFoldersCommand

                                        /* Mount point of required folders for Docker container.
                                        Because by default Jenkins mounts current working folder in Docker container,
                                        we need to add custom mount.
                                        */
                                        String mavenLocalRepositoryMount = "-v ${mavenLocalRepositoryPath}:/home/jenkins/.m2:z"
//                                        String mavenLocalRepositoryMount = "-v ${mavenLocalRepositoryPath}:${mavenLocalRepositoryPath}:rw,z"

                                        String dockerImageName = dockerConf['image'] ?:
                                                script.error('Docker image name is missing.')
                                        String dockerImageParams = [
                                                dockerConf?.params, mavenLocalRepositoryMount
                                        ].findAll().join(' ')

                                        script.docker.image(dockerImageName).inside(dockerImageParams) {
                                            script.stage('Test') {
                                                /* Run tests only for CPU backend,
                                                while CUDA tests are under development */
                                                if (backend == 'cpu') {
                                                    runtTests(platformName, backend)
                                                }
                                            }

                                            script.stage('Build') {
                                                runBuild(platformName, backend)
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
                                            runBuild(platformName, backend)
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

    private void runtTests(String platform, String backend) {
        Boolean unixNode = script.isUnix()
        String shell = unixNode ? 'sh' : 'bat'
        String separator = unixNode ? '/' : '\\'
        String testFolderName = "tests_${backend}"
        String testCommand = [
                "cd ${testFolderName}",
                'cmake -G "Unix Makefiles"',
                'make -j4',
                "layers_tests${separator}runtests --gtest_output=\"xml:cpu_test_results.xml\"" +
                        /* Add posibility to provide addtional params to gtests */
                        (libnd4jTestsFilter ? ' ' + libnd4jTestsFilter : '')
        ].join(' && ')

        switch (platform) {
            case ['linux-ppc64le', 'windows-x86_64']:
                break
            case ['linux-x86_64', 'android-arm', 'android-x86']:
                testCommand = """\
                    if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                    ${testCommand}
                """.stripIndent()
                break
            case ['macosx-x86_64']:
                /* export CC, CXX, CPP, LD required for switching compiler from clang (default for Mac) to gcc */
                testCommand = """\
                    export CC=/usr/local/bin/gcc
                    export CXX=/usr/local/bin/g++
                    export CPP=/usr/local/bin/cpp
                    export LD=/usr/local/bin/gcc
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
            int testCommandExitCode = script."${shell}" script: "${testCommand}", returnStatus: true

            /* Archiving test results */
            script.junit "**/${backend}_test_results.xml"

            /* Check test results */
            if (testCommandExitCode != 0) {
                script.error "Test stage failed with exit code ${testCommandExitCode}."
            }
            else {
                script.echo "[INFO] Finished to run tests on ${platform}, for ${backend} backend"
            }
        } else {
            script.error "${testFolderName} was not found."
        }
    }

    private void runBuild(String platform, String backend) {
        /* Build libn4j project */
        buildNativeOperations(platform, backend, script.env.CUDA_BUILD_PARAMETERS)

        String stashName = ["${projectName}", "${backend}", "${projectVersion}", "${platform}"].join('-')
        String stashIncludes = artifactNamesPattern.collect({ it + '/**' }).join(',')

        /* Stash artifacts for publishing */
        script.stash name: "$stashName", includes: "$stashIncludes"

        /* Collect artifact names */
        artifacts.push([platform: "${platform}", backend: "${backend}", name: "${stashName}"])
    }

    private runPublish(List artifacts) {
        script.dir("publish") {
            script.deleteDir()

            for (Map arct : artifacts) {
                Map artifact = arct
                String artifactFileName = "${artifact.name}.zip"
                String backendName = "${artifact.backend}"
                String platformName = "${artifact.platform}"

                script.dir("${artifact.name}") {
                    script.unstash name: "${artifact.name}"
                    script.zip zipFile: "${artifact.name}.zip", archive: false

                    publishArtifact(artifactFileName, backendName, platformName)
                }
            }
        }
    }

    private buildNativeOperations(String platform, String backend, String cudaParams = '') {
        Boolean unixNode = script.isUnix()
        String shell = unixNode ? 'sh' : 'bat'
        String buildCommand = unixNode ?
                ['if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable; fi ;',
                 'bash buildnativeoperations.sh'].join(' ') :
                ['vcvars64.bat', 'bash buildnativeoperations.sh'].join(' && ')
        List buildOptions = []

        switch (backend) {
            case 'cpu':
                buildOptions.push('-c cpu')
                (platform in ['android-x86', 'android-arm']) ? buildOptions.push("-platform ${platform}") : ''
                break
            case 'cuda-8.0':
                buildOptions.push('-c cuda -v 8.0')
                /* Hardcode compute capability (-cc) to 30 for testing */
                buildOptions.push('-cc 30')
                break
            case 'cuda-9.0':
                buildOptions.push('-c cuda -v 9.0')
                /* Hardcode compute capability (-cc) to 30 for testing */
                buildOptions.push('-cc 30')
                break
            default:
                throw new IllegalArgumentException('Backend is not supported.')
                break
        }

        /* Append user-provided buildNativeOperations options */
        buildOptions.push(cudaParams)

        script.echo "[INFO] Running buildNativeOperations on ${platform}, for ${backend} backend"
        script."$shell" script: "$buildCommand ${buildOptions.findAll().join(' ')}"
        script.echo "[INFO] Finished to run buildNativeOperations on ${platform}, for ${backend} backend"
    }

    private publishArtifact(String artifactFileName, String backend, String platform) {
        artifactFileName ?: script.error("artifactFileName argument can't be null.")
        backend ?: script.error("backend argument can't be null.")
        platform ?: script.error("platform argument can't be null.")

        String shell = script.isUnix() ? 'sh' : 'bat'
        String artifactId = [projectName, backend].join('-')
        Map nexusConfig = script.pipelineEnv.getNexusConfig(script.pipelineEnv.mvnProfileActivationName)

        script.withEnv(["PATH+MAVEN=${script.tool 'M339'}/bin"]) {
            script.configFileProvider([
                    script.configFile(fileId: "${script.pipelineEnv.mvnSettingsId}", variable: 'MAVEN_SETTINGS')
            ]) {
                /*
                   Uploaded artifact name has following format: <groupId>.<artifactId>-<version>-<classifier>
                   Example: org.nd4j.libnd4j-cuda-8.0-1.0.0-SNAPSHOT-android-x86
                 */
                String mvnCommand = [
                        'mvn -U -B',
                        "-s ${script.env.MAVEN_SETTINGS}",
                        'deploy:deploy-file',
                        "-Durl=${nexusConfig.url}",
                        "-DgroupId=${projectGroupId}",
                        "-DartifactId=${artifactId}",
                        "-Dversion=${projectVersion}",
                        '-Dpackaging=zip',
                        "-DrepositoryId=${nexusConfig.profileName}",
                        "-Dclassifier=${platform}",
                        "-Dfile=${artifactFileName}"
                ].join(' ')

                script."${shell}" script: mvnCommand
            }
        }
    }
}
