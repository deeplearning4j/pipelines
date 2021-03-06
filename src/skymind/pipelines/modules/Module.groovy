package skymind.pipelines.modules

class Module implements Serializable {
    private script
    private String backend
    private String branchName
    private String cpuExtension
    private String cudaVersion
    private Boolean isUnixNode
    private List modulesToBuild
    private String platformName
    private Boolean releaseApproved
    private String releaseBranchPattern
    private String releaseVersion
    private String scalaVersion
    private String sparkVersion
    private String pythonVersion
    private String streamName
    /* FIXME: Workaround to build and test libnd4j in Debug mode  */
    private String libnd4jBuildMode = 'release'
    /* FIXME: List of platforms on which we can't run tests ATM */
    private List streamsToExclude = [
            'android-arm-cpu',
            'android-arm64-cpu',
            'android-x86-cpu',
            'android-x86_64-cpu',
            'ios-arm64-cpu',
            'ios-x86_64-cpu',
            'macosx-x86_64-cuda-9.2',
            'macosx-x86_64-cuda-10.0',
            'macosx-x86_64-cuda-10.1',
            'linux-ppc64le-cuda-9.2',
            'linux-ppc64le-cuda-10.0',
            'linux-ppc64le-cuda-10.1',
            'linux-x86_64-cuda-9.2',
            'linux-x86_64-cuda-10.0',
            'linux-x86_64-cuda-10.1',
            'windows-x86_64-cuda-9.2',
            'windows-x86_64-cuda-10.0',
            'windows-x86_64-cuda-10.1',
            'linux-armhf-cpu'
    ]
    private String javacppCacheFolder = '.javacpp/cache/'
    private static String localRepositoryPath
    public Map testResults

    /**
     * Module class constructor
     *
     * @param Map args constructor arguments
     * @param pipeline script object
     */
    Module(Map args, script) {
        this.script = script
        backend = args.containsKey('backend') ?
                args.backend : ''
        branchName = args.containsKey('branchName') ?
                args.branchName :
                script.error('Missing branchName argument!')
        cpuExtension = args.containsKey('cpuExtension') ? args.cpuExtension : ''
        cudaVersion = backend?.contains('cuda') ? backend.tokenize('-')[1] : ''
        isUnixNode = args.containsKey('isUnixNode') ?
                args.isUnixNode :
                script.error('Missing isUnixNode argument!')
        modulesToBuild = args.containsKey('modulesToBuild') ?
                args.modulesToBuild :
                script.error('Missing modulesToBuild argument!')
        platformName = args.containsKey('platformName') ?
                args.platformName :
                script.error('Missing platformName argument!')
        releaseApproved = args.containsKey('releaseApproved') ? args.releaseApproved : false
        releaseBranchPattern = args.containsKey('releaseBranchPattern') ?
                args.releaseBranchPattern :
                script.error('Missing releaseBranchPattern argument!')
        releaseVersion = args.containsKey('releaseVersion') ?
                args.releaseVersion :
                script.error('Missing releaseVersion argument!')
        scalaVersion = args.containsKey('scalaVersion') ? args.scalaVersion : ''
        sparkVersion = args.containsKey('sparkVersion') ? args.sparkVersion : ''
        pythonVersion = args.containsKey('pythonVersion') ? args.pythonVersion : ''
        streamName = args.containsKey('streamName') ? args.streamName : ''
        // FIXME: Workaround for master and release builds
        streamName = (streamName == 'linux-x86_64-cpu-centos6') ? 'linux-x86_64-cpu' : streamName
        localRepositoryPath = '.m2/repository'
    }

    private void runBuildLogic() {
        if (platformName in ['linux-x86_64', 'linux-x86_64-generic']) {
            if (modulesToBuild.any { it =~ /^deeplearning4j|^datavec|^arbiter|^scalnet|^nd4j/ }) {
                updateVersion('scala', scalaVersion)
            }
        }

        if (cudaVersion) {
            updateVersion('cuda', cudaVersion)
        }

        script.mvn getMvnCommand('build')
    }

    private void runTestLogic() {
        try {
//            fetchTestResources()

            script.mvn getMvnCommand('test')
        }
        finally {
            def tr = script.junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'

            testResults = [
                    platform: streamName,
                    testResults: parseTestResults(tr)
            ]
        }
    }

    private void runDeployLogic() {
        script.mvn getMvnCommand('deploy')
    }

    protected void stagesToRun() {
//        script.stage('Checkout') {
//            getFancyStageDecorator('Checkout stage')
//            script.checkout script.scm
//        }

        if (modulesToBuild.contains('docs')) {
            script.stage('Release docs') {
                getFancyStageDecorator('Release docs stage')
                releaseDocs()
            }
        } else {
            if (branchName.contains(releaseBranchPattern)) {
                script.stage("Prepare for Release") {
                    getFancyStageDecorator('Prepare for Release stage')
                    setupEnvForRelease()
                }
            }

            if (branchName == 'master' || branchName.contains(releaseBranchPattern)) {
                script.stage('Build') {
                    getFancyStageDecorator('Build stage')
                    runBuildLogic()
                }

                script.stage('Deploy') {
                    getFancyStageDecorator('Deploy stage')
                    runDeployLogic()
                }
            } else {
                if (streamName == 'linux-x86_64-cpu') {
//                      script.stage('Test libnd4j in debug mode') {
//                          libnd4jBuildMode = 'debug'
//                          getFancyStageDecorator('Test libnd4j in debug mode stage')
//                          runTestLogic()
//                          libnd4jBuildMode = 'release'
//                      }

                    script.stage('Build') {
                        getFancyStageDecorator('Build stage')
                        runBuildLogic()
                    }

                    script.stage('Test') {
                        getFancyStageDecorator('Test stage')
                        runTestLogic()
                    }
                } else {
                    script.stage('Build') {
                        getFancyStageDecorator('Build stage')
                        runBuildLogic()
                    }

                    if (!(streamName in streamsToExclude)) {
                        script.stage('Test') {
                            getFancyStageDecorator('Test stage')
                            runTestLogic()
                        }
                    }
                }

//                  script.stage('Static code analysis') {
//                      runStaticCodeAnalysisLogic()
//                  }
//
//                  script.stage('Security scan') {
//                      runSecurityScanLogic()
//                  }
            }
        }
    }

    private List getMvnArguments(String stageName, List modules) {
        List mavenArguments = []

        if (modules.any { it =~ /^libnd4j/ } ||
                (platformName == 'linux-x86_64' && (!cpuExtension || backend?.contains('cuda')))
        ) {
            mavenArguments.push("-Dlibnd4j.platform=${platformName}")
            mavenArguments.push("-Dorg.bytedeco.javacpp.cachedir=${javacppCacheFolder}")

            if (backend == 'cpu') {
                // According to raver119 debug build mode for tests should be enable only for linux-x86_64-cpu
                if (libnd4jBuildMode == 'debug') {
                    mavenArguments.push('-Dlibnd4j.build=debug')
                }

                // Workaround to skip compilation libnd4j for CPU during test and deploy stages
                if (stageName in ['deploy'] && libnd4jBuildMode != 'debug') {
                    mavenArguments.push('-Dlibnd4j.cpu.compile.skip=true')
                }

                if (cpuExtension) {
                    mavenArguments.push("-Dlibnd4j.extension=${cpuExtension}")
                }

                if (stageName in ['test', 'deploy']) {
                    mavenArguments.push('-Djavacpp.parser.skip=true')
                    mavenArguments.push('-Djavacpp.compiler.skip=true')
                }

                if (stageName == 'test') {
                    mavenArguments.push('-P test-nd4j-native')
                    mavenArguments.push('-P nd4j-tests-cpu')
                }

                /*
                    FIXME: Workaround for maven-surefire-plugin,
                    to fix macOS number of threads limitation and linux JVM crashes,
                    during Nd4j tests for CPU

                    Otherwise, getting following exception:
                        java.lang.OutOfMemoryError: unable to create new native thread
                 */
                if (((platformName == 'macosx-x86_64' || platformName == 'linux-x86_64') && backend == 'cpu') &&
                        stageName == 'test'
                ) {
                    mavenArguments.push('-DreuseForks=false')
                }
            }

            if (backend?.contains('cuda')) {
                mavenArguments.push("-Dlibnd4j.chip=cuda")
                mavenArguments.push("-Dlibnd4j.cuda=${cudaVersion}")

                if (platformName != 'linux-x86_64') {
                    mavenArguments.push('-Dlibnd4j.cpu.compile.skip=true')
                }

                // Workaround to skip compilation libnd4j for CUDA during test and deploy stages
                if (stageName in ['deploy']) {
                    mavenArguments.push('-Dlibnd4j.cuda.compile.skip=true')
                }

                // Set CC to 30 to increase build speed for PR and ordinary branches
                if (!(branchName == 'master' || branchName.contains(releaseBranchPattern))) {
                    mavenArguments.push("-Dlibnd4j.compute=37")
                }

                // FIXME: Workaround to skip tests for libnd4j (because we have no libnd4j tests for CUDA backend)
//                mavenArguments.push('-Dlibnd4j.test.skip=true')

                // FIXME: Workaround to fix dependencies problem if there is nd4j, datavec or deeplearning4j in project reactor, but changes were made only for libnd4j
                mavenArguments.push("-Djavacpp.platform=${platformName}")

                if (stageName == 'test') {
                    mavenArguments.push('-P nd4j-tests-cuda')
                    mavenArguments.push('-P test-nd4j-cuda-' + cudaVersion)
                }
            }
        }

        if (modules.any { it =~ /^nd4j/ } ||
                (platformName == 'linux-x86_64' && (!cpuExtension))
        ) {
            if (branchName == 'master' || branchName.contains(releaseBranchPattern)) {
               mavenArguments.push('-P uberjar')
            }

            mavenArguments.push("-Djavacpp.platform=${platformName}")

//          if (!modules.any { it =~ /^libnd4j/ } &&
//                  (platformName != 'linux-x86_64' ||
//                          (platformName == 'linux-x86_64' && cpuExtension))
//          ) {
//              mavenArguments.push('-P libnd4j-assembly')
//          }

            if (backend == 'cpu') {
                if (branchName == 'master' || branchName.contains(releaseBranchPattern)) {
                    mavenArguments.push('-P native')
                } else {
                    mavenArguments.push('-P native-snapshots')
                }

                if (stageName in ['test', 'deploy']) {
                    mavenArguments.push('-Djavacpp.parser.skip=true')
                    mavenArguments.push('-Djavacpp.compiler.skip=true')
                }

                if (stageName == 'test') {
                    mavenArguments.push('-P test-nd4j-native')
                    mavenArguments.push('-P nd4j-tests-cpu')
                    mavenArguments.push('-P tf-cpu')
                    mavenArguments.push('-P nd4j-tf-cpu')
                    mavenArguments.push('-Dorg.bytedeco.javacpp.maxbytes=10G')
                    mavenArguments.push('-Dorg.bytedeco.javacpp.maxphysicalbytes=14G')
                }

                if (cpuExtension) {
                    mavenArguments.push("-Djavacpp.extension=${cpuExtension}")
                    if (stageName == 'test') {
                        mavenArguments.push("-Ddependency.classifier=${platformName}-${cpuExtension}")
                    }
                }

                if (platformName.contains('linux') || platformName.contains('android')) {
                    mavenArguments.push('-DprotocCommand=protoc')
                }

                if (platformName.contains('ios')) {
                    mavenArguments.push('-Djavacpp.platform.compiler=clang++')
                }

                if (platformName == 'ios-arm64') {
                    mavenArguments.push('-Djavacpp.platform.sysroot=' +
                            '$(xcrun --sdk iphoneos --show-sdk-path)')
                }

                if (platformName == 'ios-x86_64') {
                    mavenArguments.push('-Djavacpp.platform.sysroot=' +
                            '$(xcrun --sdk iphonesimulator --show-sdk-path)')
                }

                /*
                    FIXME: Workaround for maven-surefire-plugin,
                    to fix macOS number of threads limitation and linux JVM crashes,
                    during Nd4j tests for CPU

                    Otherwise, getting following exception:
                        java.lang.OutOfMemoryError: unable to create new native thread
                 */
                if (((platformName == 'macosx-x86_64' || platformName == 'linux-x86_64') && backend == 'cpu') &&
                        stageName == 'test'
                ) {
                    mavenArguments.push('-DreuseForks=false')
                }

                if (platformName == 'linux-armhf') {
                    mavenArguments.push('-Djavacpp.platform.compiler=${RPI_HOME}/tools/arm-bcm2708/arm-rpi-4.9.3-linux-gnueabihf/bin/arm-linux-gnueabihf-g++')
                }
            }

            if (backend?.contains('cuda')) {
                if (branchName == 'master' || branchName.contains(releaseBranchPattern)) {
                    mavenArguments.push('-P cuda')
                } else {
                    mavenArguments.push('-P cuda-snapshots')
                }

                if (platformName.contains('linux')) {
                    mavenArguments.push('-DprotocCommand=protoc')
                }

                if (stageName == 'test') {
                    mavenArguments.push('-P nd4j-tests-cuda')
                    mavenArguments.push('-P test-nd4j-cuda-' + cudaVersion)
                    mavenArguments.push('-P tf-gpu')
                    mavenArguments.push('-P nd4j-tf-gpu')
                    mavenArguments.push('-Dorg.bytedeco.javacpp.maxbytes=10G')
                    mavenArguments.push('-Dorg.bytedeco.javacpp.maxphysicalbytes=18G')
                }
            }
        }

        if (modules.any { it =~ /deeplearning4j|nd4j|libnd4j/ } ||
                (platformName == 'linux-x86_64' && (!cpuExtension || backend?.contains('cuda')))
        ) {
            if (stageName == 'test' && !(modules.any { it =~ /^jumpy|^pydatavec|^pydl4j/ })) {
                mavenArguments.push('-P testresources')
            }

//            if (!modules.any { it =~ /^libnd4j|^nd4j/ }) {
            if (!modules.any { it =~ /^nd4j/ }) {
//                mavenArguments.push('-P libnd4j-assembly')
            }
        }

        if (modules.any { it =~ /^datavec/ }) {
            if (!modules.any { it =~ /^nd4j/ }) {
                mavenArguments.push("-Djavacpp.platform=${platformName}")
                mavenArguments.push('-P libnd4j-assembly')
            }
        }

        mavenArguments
    }

    private String getMavenProjects(String stageName) {
        List projects = []
        String mvnArguments
        List supportedModules = [
                'libnd4j', 'nd4j', 'datavec', 'deeplearning4j', 'arbiter',
                'nd4s',
                'gym-java-client', 'rl4j', 'scalnet', 'jumpy', 'pydatavec',
                'pydl4j', 'docs'
        ]
        List mavenExcludesForNd4jNative = [
                (platformName.contains('ios')) ?
                        '!nd4j/nd4j-backends/nd4j-backend-impls/nd4j-native-platform' : '',
//                '!nd4j/nd4j-backends/nd4j-backend-impls/nd4j-cuda',
//                '!nd4j/nd4j-backends/nd4j-backend-impls/nd4j-cuda-platform',
                '!jumpy',
                '!pydatavec',
                '!pydl4j'
        ]
        List mavenExcludesForNd4jCuda = [
//                '!nd4j/nd4j-backends/nd4j-backend-impls/nd4j-native',
//                '!nd4j/nd4j-backends/nd4j-backend-impls/nd4j-native-platform',
                '!jumpy',
                '!pydatavec',
                '!pydl4j'
        ]
        List mavenExcludesForDeeplearning4jNative = [
//                '!deeplearning4j/deeplearning4j-cuda'
        ]

        if (modulesToBuild.any { it =~ /^deeplearning4j/ }) {
            if (streamName == 'linux-x86_64-cpu' && libnd4jBuildMode == 'release') {
                projects.addAll(mavenExcludesForDeeplearning4jNative)
            }
        }

        if (modulesToBuild.any { it =~ /^nd4j/ }) {
            if (platformName != 'linux-x86_64' || (platformName == 'linux-x86_64' && cpuExtension)) {
//                if (modulesToBuild.any { it =~ /^libnd4j/ }) {
//                    projects.addAll(['libnd4j'])
//                }
                projects.addAll(['libnd4j'])

                if (backend == 'cpu') {
                    // FIXME: Temporary add nd4j to the list of projects to build to enable testresources profile (add test resources dependency).
                    projects.addAll(['nd4j', 'nd4j/nd4j-backends/nd4j-backend-impls/nd4j-native'])
                }

                if (backend?.contains('cuda')) {
                    // FIXME: Temporary add nd4j to the list of projects to build to enable testresources profile (add test resources dependency).
                    projects.addAll(['nd4j', 'nd4j/nd4j-backends/nd4j-backend-impls/nd4j-cuda'])
                }

                if (stageName == 'test') {
                    projects.addAll(['nd4j/nd4j-backends/nd4j-tests'])
                }

                mvnArguments = getMvnArguments(stageName, projects).findAll().join(' ')

                return '-am -pl \'' + (projects).findAll().join(',') + '\'' + ' ' + mvnArguments
            } else {
                if (backend == 'cpu') {
                    if (!modulesToBuild.any { mavenExcludesForNd4jNative.contains(it) }) {
                        if (libnd4jBuildMode == 'release') {
                            projects.addAll(mavenExcludesForNd4jNative)
                            projects.addAll(mavenExcludesForDeeplearning4jNative)
                        } else {
                            projects.addAll(['libnd4j'])
                        }
                    }
                }

                if (backend?.contains('cuda')) {
                    /* FIXME: Add this filter for now to not break the build when changes related to modules in excludes */
                    if (!modulesToBuild.any { mavenExcludesForNd4jCuda.contains(it) } &&
                            !modulesToBuild.any {
                                mavenExcludesForDeeplearning4jNative.contains(it)
                            }
                    ) {
                        projects.addAll(mavenExcludesForNd4jCuda)
                    }
                }

                mvnArguments = getMvnArguments(stageName, projects).findAll().join(' ')

                return '-pl \'' + (projects).findAll().join(',') + '\'' + ' ' + mvnArguments
            }
        } else if (modulesToBuild.any { it =~ /^libnd4j/ }) {
            if (platformName != 'linux-x86_64' || (platformName == 'linux-x86_64' && cpuExtension)) {
                projects.addAll(['libnd4j'])
            } else {
                if (backend == 'cpu') {
                    if (libnd4jBuildMode == 'release') {
                        projects.addAll(mavenExcludesForNd4jNative)
                        projects.addAll(mavenExcludesForDeeplearning4jNative)
                    } else {
                        projects.addAll(['libnd4j'])
                    }
                }

                if (backend?.contains('cuda')) {
                    projects.addAll(mavenExcludesForNd4jCuda)
                }
            }

            mvnArguments = getMvnArguments(stageName, projects).findAll().join(' ')

            return '-pl \'' + (projects).findAll().join(',') + '\'' + ' ' + mvnArguments
        } else {
            mvnArguments = getMvnArguments(stageName, modulesToBuild + projects).findAll().join(' ')

            return (modulesToBuild.sort() == supportedModules.sort() ? '-amd ' : '') +
                    '-pl \'' + (modulesToBuild + projects).findAll().join(',') + '\'' + ' ' + mvnArguments
        }
    }

    private String getMvnCommand(String stageName) {
        String mavenCommand

        List commonArguments = [
                // FIXME: -e -B -V not picked by Windows from withMaven pipeline step
                // -T 1C set to run maven build in parallel
                // FIXME: Temporary disable maven parallel builds -T 1C
                'mvn -B -V',
                (stageName == 'build') ? '-U clean install' :
                        (stageName == 'test') ? 'test' :
                                (stageName == 'deploy') ? 'deploy' : '',
                getMavenProjects(stageName),
                (stageName != 'test') ? '-Dmaven.test.skip=true' : '--fail-never',
                (releaseApproved) ? "-P staging" : '',
                (releaseApproved && stageName == 'deploy') ?
                        "-DstagingRepositoryId=${script.env.STAGING_REPOSITORY}" : '',
                (releaseApproved && stageName == 'deploy') ? "-DperformRelease" : '',
                "-Dlocal.software.repository=${script.pipelineEnv.mvnProfileActivationName}"
        ]

        if (isUnixNode) {
//            String devtoolsetVersion = backend?.contains('cuda') ? '6' : '7'
            String devtoolsetVersion = '7'

            mavenCommand = ([
                    "if [ -f /etc/redhat-release ]; " +
                            "then source /opt/rh/devtoolset-${devtoolsetVersion}/enable; fi;",
                    (platformName.contains('ppc64le') && (backend?.contains('cuda-10.0') || backend?.contains('cuda-10.1'))) ?
                            'export MAKEJ=2 &&' : '',
                    /* Pipeline withMaven step requires this line if it runs in Docker container */
//                    (!(withMavenDockerFixPlatformsToIgnore.contains(streamName))) ?
//                            'export PATH=$MVN_CMD_DIR:$PATH &&' : '',
//                    /* MAVEN_OPTS provided below, should help to effectively use of docker container resources with Java 8 */
//                    (!(platformName in ['macosx-x86_64', 'ios-x86_64', 'ios-arm64', 'windows-x86_64'])) ?
//                            'export MAVEN_OPTS="-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap ${MAVEN_OPTS}" &&' : ''
            ] + commonArguments + [
                    /* Workaround for MacOS/iOS which doesn't honour withMaven options */
                    (platformName.contains('macosx') || platformName.contains('ios')) ?
                            "-Dmaven.repo.local=${localRepositoryPath}" :
                            (platformName.contains('android')) ?
                                    "-Dmaven.repo.local=${script.env.HOME}/${localRepositoryPath}" : '',
                    '-s ${MAVEN_SETTINGS}'
            ]).findAll().join(' ')
        } else {
            mavenCommand = ([
                    'vcvars64.bat',
                    '&&',
                    'bash -c "',
            ] + commonArguments + [
                    /* Workaround for Windows which doesn't honour withMaven options */
                    "-Dmaven.repo.local=${localRepositoryPath}",
                    '-s ${MAVEN_SETTINGS}'
            ]).findAll().join(' ') + '"'
        }

        return mavenCommand
    }

    private void updateVersion(String updateTarget, String version) {
        if (isUnixNode) {
            script.sh "bash ./change-${updateTarget.toLowerCase()}-versions.sh ${version}"
        } else {
            script.bat "\"C:\\Program Files\\Git\\bin\\bash.exe\" -c " +
                    "\"./change-${updateTarget.toLowerCase()}-versions.sh ${version}\""
        }
    }

    private void setupEnvForRelease() {
        if (releaseApproved) {
            populateGpgKeys()
            updateGitCredentials()
//            updateDependencyVersions(releaseVersion)
//            script.setProjectVersion(releaseVersion, true)
        }
    }

    private void updateDependencyVersions(String version) {
        if (platformName == 'linux-x86_64' && backend?.contains('cuda')) {
            if (script.isUnix()) {
                script.sh """
                    for item in 'libnd4j' 'nd4j' 'deeplearning4j' 'arbiter' 'datavec' 'gym-java-client' 'jumpy' 'rl4j' 'scalnet'; do
                        pushd "\${item}"

                        sed -i "s/<nd4j.version>.*<\\/nd4j.version>/<nd4j.version>${version}<\\/nd4j.version>/" pom.xml
                        sed -i "s/<datavec.version>.*<\\/datavec.version>/<datavec.version>${
                    version
                }<\\/datavec.version>/" pom.xml
                        sed -i "s/<deeplearning4j.version>.*<\\/deeplearning4j.version>/<deeplearning4j.version>${
                    version
                }<\\/deeplearning4j.version>/" pom.xml
                        sed -i "s/<dl4j-test-resources.version>.*<\\/dl4j-test-resources.version>/<dl4j-test-resources.version>${
                    version
                }<\\/dl4j-test-resources.version>/" pom.xml

                        #Spark versions, like <version>xxx_spark_2-SNAPSHOT</version>
                        for f in \$(find . -name 'pom.xml' -not -path '*target*'); do
                            sed -i "s/version>.*_spark_.*</version>${version}_spark_1</g" "\${f}"
                        done

                        popd
                    done
            """.stripIndent()
            } else {
                /* TODO: Add windows support */
                script.error "[ERROR] Windows is not supported yet."
            }
        }
    }

    private void populateGpgKeys() {
        script.withCredentials([
                /*  GPG public key for signing releases */
                script.file(credentialsId: 'gpg-pub-key-jenkins', variable: 'GPG_PUBRING'),
                /*  GPG private key for signing releases */
                script.file(credentialsId: 'gpg-private-key-jenkins', variable: 'GPG_SECRING')
        ]) {
            if (script.isUnix()) {
                script.sh '''
                    export GPG_TTY=$(tty)
                    rm -rf ${HOME}/.gnupg/*.gpg
                    gpg --list-keys
                    # workaround for mac (maybe not required)
                    # if [ $(gpg --list-keys | echo $?) == 0 ]; then
                        cp ${GPG_PUBRING} ${HOME}/.gnupg/
                        cp ${GPG_SECRING} ${HOME}/.gnupg/
                        chmod 700 $HOME/.gnupg
                        chmod 600 $HOME/.gnupg/secring.gpg $HOME/.gnupg/pubring.gpg
                        gpg --list-keys
                    # fi
                '''.stripIndent()
            } else {
                script.bat '''
                    bash -c "rm -rf ${HOME}/.gnupg/*.gpg"
                    bash -c "gpg --list-keys"
                    bash -c "cp ${GPG_PUBRING} ${HOME}/.gnupg/"
                    bash -c "cp ${GPG_SECRING} ${HOME}/.gnupg/"
                    bash -c "chmod 700 $HOME/.gnupg"
                    bash -c "chmod 600 $HOME/.gnupg/secring.gpg $HOME/.gnupg/pubring.gpg"
                    bash -c "gpg --list-keys"
                    bash -c "gpg.exe --list-keys"
                '''.stripIndent()
            }
        }
    }

    private void updateGitCredentials() {
        if (isUnixNode) {
            script.sh """
                git config user.email 'jenkins@skymind.io'
                git config user.name 'Jenkins CI (Skymind)'
            """.stripIndent()
        } else {
            script.bat """
                bash -c 'git config user.email "jenkins@skymind.io"'
                bash -c 'git config user.name "Jenkins"'
            """.stripIndent()
        }
    }

    private void getFancyStageDecorator(String text) {
        int charsNumber = Math.round((78 - text.length()) / 2)

        script.echo("*" * charsNumber + text + "*" * charsNumber)
    }

    private String parseTestResults(testResults) {
        String testResultsDetails = ''

        if (testResults != null) {
            def total = testResults.totalCount
            def failed = testResults.failCount
            def skipped = testResults.skipCount
            def passed = total - failed - skipped

            testResultsDetails += ("Total: " + total)
            testResultsDetails += (", Passed: " + passed)
            testResultsDetails += (", Failed: " + failed)
            testResultsDetails += (", Skipped: " + skipped)
        } else {
            testResultsDetails = 'No test results found'
        }

        return testResultsDetails
    }

    private void releaseDocs() {
        String dl4jDocsDir
        String dl4jVersion = (branchName.contains(releaseBranchPattern)) ? 'release' : 'snapshot'

        script.dir('deeplearning4j-docs') {
            script.echo "Checkout deeplearning4j-docs source code"
            script.git url: "git@github.com:deeplearning4j/deeplearning4j-docs.git",
                    credentialsId: 'github-username-and-ssh-key',
                    branch: 'master'

            dl4jDocsDir = (script.sh(script: 'pwd', returnStdout: true).trim()) ?:
                    script.error('DL4J_DOCS_DIR value is empty!')
        }

        script.dir('docs') {
            script.echo "Generate all docs"
            script.sh "bash ./gen_all_docs.sh"

            script.echo "Copy generated docs"
            script.sh """\
                export DL4J_DOCS_DIR=${dl4jDocsDir}
                export DL4J_VERSION=${dl4jVersion}
                bash ./copy-to-dl4j-docs.sh
            """
        }

        script.dir('deeplearning4j-docs') {
            def currentTime = new Date()?.format("yyyyMMdd-HH:mm:ss", TimeZone.getTimeZone('UTC'))
            String commitMessage = "Auto-update docs ${currentTime}"
            String targetBranchName = ((branchName.contains(releaseBranchPattern)) ? 'feature' : 'bugfix') +
                    "/" +
                    "jenkins-docs-update-${currentTime.replaceAll(':','-')}"
            String gitHubUsername = "Skymind CI"
            String gitHubUserEmail = "34909009+skymindops@users.noreply.github.com"

            script.sshagent(['github-username-and-ssh-key']) {
                script.sh """\
                    git config --global user.name "${gitHubUsername}"
                    git config --global user.email "${gitHubUserEmail}"
                    git status
                    git checkout -b "${targetBranchName}"
                    git add -A
                    git commit -am "${commitMessage}"
                    # git push origin "${targetBranchName}"
                """.stripIndent()
            }
        }
    }

    private void fetchTestResources() {
        String testResourcesGitBranch
        String testResourcesGitRepository = 'https://github.com/deeplearning4j/dl4j-test-resources.git'
        String testResourcesBuildCommand

        switch (branchName) {
            case ~/^dev.*/:
                testResourcesGitBranch = branchName
                break
            case 'master':
            default:
                testResourcesGitBranch = 'master'
                break
        }

        script.dir('dl4j-test-resources') {
            script.echo "Checkout dl4j-test-resources source code"

            script.git url: testResourcesGitRepository,
                    credentialsId: 'github-username-and-ssh-key',
                    branch: testResourcesGitBranch

            if (isUnixNode) {
                testResourcesBuildCommand = [
                        "if [ -f /etc/redhat-release ]; " +
                                "then source /opt/rh/devtoolset-7/enable; fi;",
                        'mvn -U -B -e -s ${MAVEN_SETTINGS} clean install -Dresources.jar.compression=true'
                ].findAll().join(' ')
            } else {
                testResourcesBuildCommand = [
                        'vcvars64.bat',
                        '&&',
                        'bash -c',
                        '"mvn -U -B -e -s ${MAVEN_SETTINGS} clean install -Dresources.jar.compression=true"'
                ].findAll().join(' ')
            }

            script.mvn testResourcesBuildCommand
        }
    }
}
