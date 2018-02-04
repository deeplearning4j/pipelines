package skymind.pipelines.projects

import groovy.transform.InheritConstructors

@InheritConstructors
class Nd4jProject extends Project {
    private pom
    private String projectGroupId
    private final String nd4jDependencyName = 'libnd4j'

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

    void initPipeline() {
        script.node('master') {
            pipelineWrapper {
                script.milestone()
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

                                String libnd4jHomePath = [
                                        script.pwd(), nd4jDependencyName
                                ].join(separator)
//                                String mavenLocalRepositoryPath = [
//                                        script.pwd(), script.pipelineEnv.localRepositoryPath
//                                ].join(separator)
                                String mavenLocalRepositoryPath = script.pipelineEnv.jenkinsDockerM2Mount

                                script.stage('Set up environment') {
                                    script.dir(projectName) {
                                        script.unstash 'sourceCode'

                                        pom = projectObjectModel
                                        projectVersion = pom?.version
                                        projectGroupId = pom?.groupId
                                    }
                                }

                                script.stage('Resolve backend dependency') {
                                    resolveBackendDependency(platformName, 'cpu', nd4jDependencyName)

                                    if (backend != 'cpu') {
                                        resolveBackendDependency(platformName, backend, nd4jDependencyName)
                                    }
                                }

                                script.dir(projectName) {
                                    /* Get docker container configuration */
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
                                        String libnd4jHomeMount = "-v ${libnd4jHomePath}:${libnd4jHomePath}:rw,z"
                                        String mavenLocalRepositoryMount = "-v ${mavenLocalRepositoryPath}:/home/jenkins/.m2:z"
//                                        String mavenLocalRepositoryMount = "-v ${mavenLocalRepositoryPath}:${mavenLocalRepositoryPath}:rw,z"

                                        String dockerImageName = dockerConf['image'] ?:
                                                script.error('Docker image name is missing.')
                                        String dockerImageParams = [
                                                dockerConf?.params, libnd4jHomeMount, mavenLocalRepositoryMount
                                        ].findAll().join(' ')

                                        script.docker.image(dockerImageName).inside(dockerImageParams) {
                                            script.stage('Build') {
                                                runBuild(platformName, backend, libnd4jHomePath, mavenLocalRepositoryPath)
                                            }
                                        }
                                    } else {
                                        script.stage('Build') {
                                            runBuild(platformName, backend, libnd4jHomePath, mavenLocalRepositoryPath)
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

    private void runBuild(String platform, String backend, String libnd4jHomePath, String mavenLocalRepositoryPath) {
        Boolean unixNode = script.isUnix()
        String shell = unixNode ? 'sh' : 'bat'
        String separator = script.isUnix() ? '/' : '\\'
        String cudaVersion = backend.tokenize('-')[1]
        String cudaBackendPath = [libnd4jHomePath, 'blasbuild', 'cuda'].join(separator)

        script.isVersionReleased(projectName, projectVersion)
        script.setProjectVersion(projectVersion, true)

        /* Workaround for protobuf */
        if (platform in ['linux-x86_64', 'android-arm', 'android-x86']) {
            script.env.PROTOBUF_VERSION = '3.5.1'
            getAndBuildProtobuf("${script.env.PROTOBUF_VERSION}")
        }

        for (String sclVer : scalaVersions) {
            String scalaVersion = sclVer
            String mvnCommand

            script.echo "[INFO] Setting Scala version to: $scalaVersion"
            String updateScalaCommand = [
                    (unixNode ? 'bash' : "\"C:\\Program Files\\Git\\bin\\bash.exe\" -c"),
                    (unixNode ? "./change-scala-versions.sh $scalaVersion" :
                            "\"./change-scala-versions.sh $scalaVersion\"")
            ].findAll().join(' ')
            script."$shell" updateScalaCommand

            /* Nd4j build for libn4j CPU backend */
            if (backend == 'cpu') {
                mvnCommand = getMvnCommand('build',
                        [
                                '-pl ' +
                                        '\'' +
                                        '!nd4j-backends/nd4j-backend-impls/nd4j-cuda,' +
                                        '!nd4j-backends/nd4j-backend-impls/nd4j-cuda-platform,' +
                                        '!nd4j-backends/nd4j-tests' +
                                        '\'',
//                                '-Dmaven.repo.local=' + (unixNode ?
//                                        mavenLocalRepositoryPath :
//                                        mavenLocalRepositoryPath.replaceAll('\\\\', '/')) + '/repository',
                                /* Nd4j requires LIBND4J_HOME path provided in UNIX style, even for Windows builds */
                                "-Denv.LIBND4J_HOME=" + (unixNode ?
                                        libnd4jHomePath :
                                        libnd4jHomePath.replaceAll('\\\\', '/')),
                                (platform in ['android-x86', 'android-arm']) ? "-Djavacpp.platform=${platform}" : '',
                                script.env.PROTOBUF_VERSION ?
                                        '-DprotocCommand=protobuf-${PROTOBUF_VERSION}/src/protoc' :
                                        ''
                        ]
                )
                script.echo "[INFO] Building nd4j with Scala ${scalaVersion} versions"
            }
            /* Nd4j build for libn4j CUDA backend */
            else {
                script.echo "[INFO] Updating CUDA $cudaVersion path"
                String updateBackendCommand = unixNode ? [
                        "if [ -L ${cudaBackendPath} ] ;",
                        "then rm -f ${cudaBackendPath} &&",
                        "ln -sf ${cudaBackendPath}-${cudaVersion} ${cudaBackendPath};",
                        "else ln -sf ${cudaBackendPath}-${cudaVersion} ${cudaBackendPath};",
                        'fi'
                ].join(' ') : [
                        "IF EXIST ${cudaBackendPath}",
                        "(RD /q /s ${cudaBackendPath} &&",
                        "XCOPY /E /Q /I ${cudaBackendPath}-${cudaVersion} ${cudaBackendPath})",
                        "ELSE (XCOPY /E /Q /I ${cudaBackendPath}-${cudaVersion} ${cudaBackendPath})"
                ].join(' ')
                script."$shell" updateBackendCommand

                script.echo "[INFO] Setting CUDA version to: $cudaVersion"
                String updateCudaCommand = [
                        (unixNode ? 'bash' : "\"C:\\Program Files\\Git\\bin\\bash.exe\" -c"),
                        (unixNode ? "./change-cuda-versions.sh $cudaVersion" :
                                "\"./change-cuda-versions.sh $cudaVersion\"")
                ].join(' ')
                script."$shell" updateCudaCommand

                mvnCommand = getMvnCommand(
                        'build',
                        ["-Dscala.binary.version=${scalaVersion}",
//                         '-Dmaven.repo.local=' + (unixNode ?
//                                 mavenLocalRepositoryPath :
//                                 mavenLocalRepositoryPath.replaceAll('\\\\', '/')) + '/repository',
                         /* Nd4j requires LIBND4J_HOME path provided in UNIX style, even for Windows builds */
                         "-Denv.LIBND4J_HOME=" + (unixNode ?
                                 libnd4jHomePath :
                                 libnd4jHomePath.replaceAll('\\\\', '/')),
                         script.env.PROTOBUF_VERSION ? '-DprotocCommand=protobuf-${PROTOBUF_VERSION}/src/protoc' : '']
                )

                script.echo "[INFO] Building nd4j with CUDA ${cudaVersion} and Scala ${scalaVersion} versions"
            }

            script.lock('build-with-maven-3.3.9') {
                script.mvn "$mvnCommand"
            }
        }
    }

    private void resolveBackendDependency(String platform, String backend, String dependencyName) {
        platform ?: script.error("platform argument can't be null.")
        backend ?: script.error("backend argument can't be null.")
        dependencyName ?: script.error("dependencyName argument can't be null.")

        String separator = script.isUnix() ? '/' : '\\'
        String dependencyPath = [dependencyName, 'blasbuild', backend].join(separator)

        if (script.fileExists(dependencyPath)) {
            script.echo "[INFO] backend dependency folder already exists"
        } else {
            script.dir(dependencyName) {
                String dependencyFileName = getDependency(dependencyName, platform, backend)
                script.unzip zipFile: dependencyFileName
            }

            if (!script.fileExists(dependencyPath)) {
                script.error "Backend dependency folder is missing"
            }
        }
    }

    private String getDependency(String dependencyName, String platform, String backend) {
        String shell = script.isUnix() ? 'sh' : 'bat'
        String artifactId = [dependencyName, backend].join('-')
        String dependencyFileName = [artifactId, projectVersion, platform].join('-') + '.zip'
        Map nexusConfig = script.pipelineEnv.getNexusConfig(script.pipelineEnv.mvnProfileActivationName)

        String mvnCommand = [
                'mvn -U -B',
                'dependency:get',
                "-DremoteRepositories=${nexusConfig.url}",
                "-DgroupId=${projectGroupId}",
                "-DartifactId=${artifactId}",
                "-Dversion=${projectVersion}",
                '-Dpackaging=zip',
                "-Dclassifier=${platform}",
                '-Dtransitive=false',
                "-Ddest=${dependencyFileName}"
        ].join(' ')

        script."${shell}" script: mvnCommand

        return dependencyFileName
    }

    private void getAndBuildProtobuf(String protobufVersion) {
        String packageName = "protobuf"
        String archiveName = "${packageName}-cpp-${protobufVersion}.tar.gz"

        script.sh """\
            echo "[INFO] Fetching ${archiveName}..."
            curl --retry 10 -L https://github.com/google/protobuf/releases/download/v${protobufVersion}/${archiveName} \
            -o ${archiveName} && \
            tar --totals -xf ${archiveName}
            
            echo "[INFO] Starting protobuf build..."
            cd ${packageName}-${protobufVersion}/ && ./configure && make -j2
        """.stripIndent()
    }
}