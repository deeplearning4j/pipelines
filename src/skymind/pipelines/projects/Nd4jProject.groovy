package skymind.pipelines.projects

import groovy.transform.InheritConstructors

@InheritConstructors
class Nd4jProject extends Project {
    /* Override default platforms */
    static {
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
//                 cpuExtensions: ['avx2', 'avx512'],
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
            String platformName = platform.name
            List backends = platform.backends
            List compilers = platform.compilers
            /* List with empty element was added to build for CPU without extension */
            List cpuExtensions = platform.cpuExtensions ?: ['']

            for (List bckd : backends) {
                String backend = bckd

                if (backend == 'cpu') {
                    for (String cpuExt : cpuExtensions) {
                        String cpuExtension = cpuExt

                        String streamName = ["${platformName}", "${backend}", "${cpuExtension}"].findAll().join('-')
                        /* Add steam to build name */
//                    script.pipelineEnv.buildDisplayName.push("${streamName}")

                        /* Create stream body */
                        streams["$streamName"] = {
                            script.node(platformName) {
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

//                                script.stage('Get project version from pom.xml') {
//                                    script.dir(projectName) {
//                                        projectVersion = projectObjectModel?.version
//                                    }
//                                }

                                        script.dir(projectName) {
                                            /* Get docker container configuration */
                                            Map dockerConf = script.pipelineEnv.getDockerConfig(streamName)

                                            if (dockerConf) {
                                                String dockerImageName = dockerConf['image'] ?:
                                                        script.error('Docker image name is missing.')
                                                String dockerImageParams = dockerConf?.params

                                                script.docker.image(dockerImageName).inside(dockerImageParams) {
                                                    if (branchName.contains(releaseBranchPattern)) {
                                                        script.stage("Prepare for Release") {
                                                            setupEnvForRelease()
                                                        }
                                                    }

                                                    script.stage('Build') {
                                                        runStageLogic('build', platformName, backend, cpuExtension)
                                                    }

                                                    /* FIXME: ATM test are running only for master branch */
                                                    if (branchName == 'master') {
                                                        /* Workaround to exclude test for cpu/cpu extensions that are not supported by Jenkins agents */
                                                        if (platformName.contains('ios') || platformName.contains('android')) {
                                                            script.echo "Skipping tests for ${backend} on ${platformName}, " +
                                                                    "because of lack of target device..."
                                                        } else if (platformName.contains('macosx') && cpuExtension != '') {
                                                            script.echo "Skipping tests for ${backend} on ${platformName} with ${cpuExtension}, " +
                                                                    "because of lack of extension support on Jenkins agent..."
                                                        } else if (platformName.contains('linux-x86_64') && cpuExtension == 'avx512') {
                                                            script.echo "Skipping tests for ${backend} on ${platformName} with ${cpuExtension}, " +
                                                                    "because of lack of extension support on Jenkins agent..."
                                                        } else {
                                                            script.stage('Build Test Resources') {
                                                                runBuildTestResources(platformName)
                                                            }

                                                            script.stage('Test') {
                                                                runStageLogic('test', platformName, backend, cpuExtension)
                                                            }
                                                        }
                                                    }

                                                    if (branchName == 'master' || branchName.contains(releaseBranchPattern)) {
                                                        script.stage('Deploy') {
                                                            runStageLogic('deploy', platformName, backend, cpuExtension)
                                                        }
                                                    }
                                                }
                                            } else {
                                                if (branchName.contains(releaseBranchPattern)) {
                                                    script.stage("Prepare for Release") {
                                                        setupEnvForRelease()
                                                    }
                                                }

                                                script.stage('Build') {
                                                    runStageLogic('build', platformName, backend, cpuExtension)
                                                }

                                                /* FIXME: ATM test are running only for master branch */
                                                if (branchName == 'master' && !branchName.contains(releaseBranchPattern)) {
                                                    /* Workaround to exclude test for cpu/cpu extensions that are not supported by Jenkins agents */
                                                    if (platformName.contains('ios') || platformName.contains('android')) {
                                                        script.echo "Skipping tests for ${backend} on ${platformName}, " +
                                                                "because of lack of target device..."
                                                    } else if (platformName.contains('macosx') && cpuExtension != '') {
                                                        script.echo "Skipping tests for ${backend} on ${platformName} with ${cpuExtension}, " +
                                                                "because of lack of extension support on Jenkins agent..."
                                                    } else if (platformName.contains('linux-x86_64') && cpuExtension == 'avx512') {
                                                        script.echo "Skipping tests for ${backend} on ${platformName} with ${cpuExtension}, " +
                                                                "because of lack of extension support on Jenkins agent..."
                                                    } else {
                                                        script.stage('Build Test Resources') {
                                                            runBuildTestResources(platformName)
                                                        }

                                                        script.stage('Test') {
                                                            runStageLogic('test', platformName, backend, cpuExtension)
                                                        }
                                                    }
                                                }

                                                if (branchName == 'master' || branchName.contains(releaseBranchPattern)) {
                                                    script.stage('Deploy') {
                                                        runStageLogic('deploy', platformName, backend, cpuExtension)
                                                    }
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
                } else {
                    String streamName = ["${platformName}", "${backend}"].findAll().join('-')
                    /* Add steam to build name */
//                    script.pipelineEnv.buildDisplayName.push("${streamName}")

                    /* Create stream body */
                    streams["$streamName"] = {
                        script.node(platformName) {
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

//                                script.stage('Get project version from pom.xml') {
//                                    script.dir(projectName) {
//                                        projectVersion = projectObjectModel?.version
//                                    }
//                                }

                                    script.dir(projectName) {
                                        /* Get docker container configuration */
                                        Map dockerConf = script.pipelineEnv.getDockerConfig(streamName)

                                        if (dockerConf) {
                                            String dockerImageName = dockerConf['image'] ?:
                                                    script.error('Docker image name is missing.')
                                            String dockerImageParams = dockerConf?.params

                                            script.docker.image(dockerImageName).inside(dockerImageParams) {
                                                if (branchName.contains(releaseBranchPattern)) {
                                                    script.stage("Prepare for Release") {
                                                        setupEnvForRelease()
                                                    }
                                                }

                                                script.stage('Build') {
                                                    runStageLogic('build', platformName, backend)
                                                }

                                                /* FIXME: ATM test are running only for master branch */
                                                if (branchName == 'master') {
                                                    /* Workaround to exclude test for backends that are not supported by Jenkins agents */
                                                    if (platformName.contains('macosx') && backend.contains('cuda')) {
                                                        script.echo "Skipping tests for ${backend} on ${platformName}, " +
                                                                "because of lack of GPU..."
                                                    } else {
                                                        script.stage('Build Test Resources') {
                                                            runBuildTestResources(platformName)
                                                        }

                                                        script.stage('Test') {
                                                            runStageLogic('test', platformName, backend)
                                                        }
                                                    }
                                                }

                                                if (branchName == 'master' || branchName.contains(releaseBranchPattern)) {
                                                    script.stage('Deploy') {
                                                        runStageLogic('deploy', platformName, backend)
                                                    }
                                                }
                                            }
                                        } else {
                                            if (branchName.contains(releaseBranchPattern)) {
                                                script.stage("Prepare for Release") {
                                                    setupEnvForRelease()
                                                }
                                            }

                                            script.stage('Build') {
                                                runStageLogic('build', platformName, backend)
                                            }

                                            /* FIXME: ATM test are running only for master branch */
                                            if (branchName == 'master') {
                                                /* Workaround to exclude test for backends that are not supported by Jenkins agents */
                                                if (platformName.contains('macosx') && backend.contains('cuda')) {
                                                    script.echo "Skipping tests for ${backend} on ${platformName}, " +
                                                            "because of lack of GPU..."
                                                } else {
                                                    script.stage('Build Test Resources') {
                                                        runBuildTestResources(platformName)
                                                    }

                                                    script.stage('Test') {
                                                        runStageLogic('test', platformName, backend)
                                                    }
                                                }
                                            }

                                            if (branchName == 'master' || branchName.contains(releaseBranchPattern)) {
                                                script.stage("Deploy") {
                                                    runStageLogic('deploy', platformName, backend)
                                                }
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
            }
        }

        streams
    }

    private void runStageLogic(String stageName, String platform, String backend, String cpuExtension = '') {
        String mvnCommand
        Boolean unixNode = script.isUnix()
        String shell = unixNode ? 'sh' : 'bat'
        Closure updateScalaCommand = { String version ->
            return [
                    (unixNode ? 'bash' : "\"C:\\Program Files\\Git\\bin\\bash.exe\" -c"),
                    (unixNode ? "./change-scala-versions.sh $version" :
                            "\"./change-scala-versions.sh $version\"")
            ].join(' ')
        }
        Closure updateCudaCommand = { String version ->
            return [
                    (unixNode ? 'bash' : "\"C:\\Program Files\\Git\\bin\\bash.exe\" -c"),
                    (unixNode ? "./change-cuda-versions.sh $version" :
                            "\"./change-cuda-versions.sh $version\"")
            ].join(' ')
        }
        String mavenExcludesForCpu = '-pl ' +
                '\'' +
                ((platform.contains('ios')) ? '!nd4j-backends/nd4j-backend-impls/nd4j-native-platform,' : '') +
                '!nd4j-backends/nd4j-backend-impls/nd4j-cuda,' +
                '!nd4j-backends/nd4j-backend-impls/nd4j-cuda-platform,' +
                '!nd4j-backends/nd4j-tests' +
                '\''
        String mavenExcludesForCuda = '-pl ' +
                '\'' +
                '!nd4j-backends/nd4j-backend-impls/nd4j-native,' +
                '!nd4j-backends/nd4j-backend-impls/nd4j-native-platform,' +
                '!nd4j-backends/nd4j-tests' +
                '\''

//        script.isVersionReleased(projectName, projectVersion)
//        script.setProjectVersion(projectVersion, true)
//        if (branchName.contains(releaseBranchPattern)) {
//            updateGitCredentials()
//            script.setProjectVersion(releaseVersion, true)
//        }

        /* Nd4j build with libn4j CPU backend and/or specific extension */
        if (backend == 'cpu') {
            /* Workaround to set scala version */
            String scalaVersion = (platform in ['android-arm', 'android-x86', 'ios-arm64']) ?
                    '2.10' :
                    '2.11'

            script.echo "[INFO] Setting Scala version to: $scalaVersion"
            script."$shell" script: updateScalaCommand(scalaVersion)

            mvnCommand = getMvnCommand(stageName, (cpuExtension != ''), [
                    "-Djavacpp.platform=${platform}",
                    (cpuExtension) ? "-Djavacpp.extension=${cpuExtension}" : '',
                    (platform.contains('linux') || platform.contains('android')) ?
                            '-DprotocCommand=protoc' :
                            '',
//                    (!(platform.contains('linux') || platform.contains('windows'))) ?
//                            "-Dmaven.javadoc.skip=true" :
//                            '',
                    (platform.contains('ios')) ? '-Djavacpp.platform.compiler=clang++' : '',
                    (platform == 'ios-arm64') ?
                            '-Djavacpp.platform.sysroot=$(xcrun --sdk iphoneos --show-sdk-path)' : '',
                    (platform == 'ios-x86_64') ?
                            '-Djavacpp.platform.sysroot=$(xcrun --sdk iphonesimulator --show-sdk-path)' : '',
                    (platform.contains('macosx') || platform.contains('ios')) ?
                            "-Dmaven.repo.local=${script.env.WORKSPACE}/${script.pipelineEnv.localRepositoryPath}" :
                            '',
                    (stageName != 'test') ?
                            mavenExcludesForCpu :
                            '-pl \'nd4j-backends/nd4j-backend-impls/nd4j-native\''
            ])

            script.echo "[INFO] ${stageName.capitalize()}ing nd4j ${backend} backend with " +
                    "Scala ${scalaVersion} versions and ${cpuExtension} extension"

            script.mvn "$mvnCommand"
        }
        /* Nd4j build with libn4j CUDA backend */
        else {
            String cudaVersion = backend.tokenize('-')[1]

            script.echo "[INFO] Setting CUDA version to: $cudaVersion"
            script."$shell" script: updateCudaCommand(cudaVersion)

            /* Workaround to set scala version */
            String scalaVersion = (backend.contains('8.0')) ? '2.10' : '2.11'

            script.echo "[INFO] Setting Scala version to: $scalaVersion"
            script."$shell" script: updateScalaCommand(scalaVersion)

            mvnCommand = getMvnCommand(stageName, false, [
                    (platform.contains('linux')) ?
                            '-DprotocCommand=protoc' :
                            '',
                    (platform.contains('macosx') || platform.contains('ios')) ?
                            "-Dmaven.repo.local=${script.env.WORKSPACE}/${script.pipelineEnv.localRepositoryPath}" :
                            '',
                    (stageName != 'test') ? mavenExcludesForCuda : '-pl \'nd4j-backends/nd4j-backend-impls/nd4j-cuda\''
            ])

            script.echo "[INFO] ${stageName.capitalize()}ing nd4j with CUDA ${cudaVersion} and Scala ${scalaVersion} versions"

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
                            'install',
                            "-Dlocal.software.repository=${script.pipelineEnv.mvnProfileActivationName}",
                            '-Dmaven.test.skip=true',
                            '-P libnd4j-assembly',
                            (releaseApproved) ? "-P staging" : '',
                            '-P native-snapshots',
                            '-P uberjar'
                    ].plus(mvnArguments).findAll().join(' ')
                } else {
                    return [
                            'vcvars64.bat',
                            '&&',
                            'bash -c',
                            '"' + 'export PATH=$PATH:/c/msys64/mingw64/bin &&',
                            'mvn -U -B',
                            'clean',
                            'install',
                            "-Dlocal.software.repository=${script.pipelineEnv.mvnProfileActivationName}",
                            '-Dmaven.test.skip=true',
                            /* Workaround for Windows which doesn't honour withMaven options */
                            '-s ${MAVEN_SETTINGS}',
                            "-Dmaven.repo.local=" +
                                    "${script.env.WORKSPACE.replaceAll('\\\\', '/')}/" +
                                    "${script.pipelineEnv.localRepositoryPath}",
                            '-P libnd4j-assembly',
                            (releaseApproved) ? "-P staging" : '',
                            '-P native-snapshots',
                            '-P uberjar'
                    ].plus(mvnArguments).findAll().join(' ') + '"'
                }
                break
            case 'test':
                if (unixNode) {
                    return [
                            "if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-${devtoolsetVersion}/enable; fi;",
                            /* Pipeline withMaven step requires this line if it runs in Docker container */
                            'export PATH=$MVN_CMD_DIR:$PATH &&',
                            'mvn -B',
                            'test',
                            /* FIXME: Temporary ignoring test results */
                            '-Dmaven.test.failure.ignore=true',
                            "-Dlocal.software.repository=${script.pipelineEnv.mvnProfileActivationName}",
                            '-P libnd4j-assembly',
                            '-P testresources',
                            (releaseApproved) ? "-P staging" : '',
                            '-P native-snapshots',
                            '-P uberjar'
                    ].plus(mvnArguments).findAll().join(' ') + ' || true'
                } else {
                    return [
                            'vcvars64.bat',
                            '&&',
                            'bash -c',
                            '"' + 'export PATH=$PATH:/c/msys64/mingw64/bin &&',
                            'mvn -B',
                            'test',
                            /* FIXME: Temporary ignoring test results */
                            '-Dmaven.test.failure.ignore=true',
                            "-Dlocal.software.repository=${script.pipelineEnv.mvnProfileActivationName}",
                            /* Workaround for Windows which doesn't honour withMaven options */
                            '-s ${MAVEN_SETTINGS}',
                            "-Dmaven.repo.local=" +
                                    "${script.env.WORKSPACE.replaceAll('\\\\', '/')}/" +
                                    "${script.pipelineEnv.localRepositoryPath}",
                            '-P libnd4j-assembly',
                            '-P testresources',
                            (releaseApproved) ? "-P staging" : '',
                            '-P native-snapshots',
                            '-P uberjar'
                    ].plus(mvnArguments).findAll().join(' ') + ' || true"'
                }
                break
            case 'deploy':
                if (unixNode) {
                    return [
                            "if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-${devtoolsetVersion}/enable; fi;",
                            /* Pipeline withMaven step requires this line if it runs in Docker container */
                            'export PATH=$MVN_CMD_DIR:$PATH &&',
                            'mvn -B',
                            'deploy',
                            "-Dlocal.software.repository=${script.pipelineEnv.mvnProfileActivationName}",
                            (releaseApproved) ? "-DstagingRepositoryId=${script.env.STAGING_REPOSITORY}" : '',
                            (releaseApproved) ? "-DperformRelease" : '',
                            (releaseApproved) ? "-P staging" : '',
                            '-Dmaven.test.skip=true',
                            '-P libnd4j-assembly',
                            '-P native-snapshots',
                            '-P uberjar'
                    ].plus(mvnArguments).findAll().join(' ')
                } else {
                    return [
                            'vcvars64.bat',
                            '&&',
                            'bash -c',
                            '"' + 'export PATH=$PATH:/c/msys64/mingw64/bin &&',
                            'mvn -B',
                            'deploy',
                            "-Dlocal.software.repository=${script.pipelineEnv.mvnProfileActivationName}",
                            (releaseApproved) ? "-DstagingRepositoryId=${script.env.STAGING_REPOSITORY}" : '',
                            (releaseApproved) ? "-DperformRelease" : '',
                            (releaseApproved) ? "-P staging" : '',
                            '-Dmaven.test.skip=true',
                            /* Workaround for Windows which doesn't honour withMaven options */
                            '-s ${MAVEN_SETTINGS}',
                            "-Dmaven.repo.local=" +
                                    "${script.env.WORKSPACE.replaceAll('\\\\', '/')}/" +
                                    "${script.pipelineEnv.localRepositoryPath}",
                            '-P libnd4j-assembly',
                            '-P native-snapshots',
                            '-P uberjar'
                    ].plus(mvnArguments).findAll().join(' ') + '"'
                }
                break
            default:
                throw new IllegalArgumentException('Stage is not supported yet')
                break
        }
    }
}
