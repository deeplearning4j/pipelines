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
            'macosx-x86_64-cuda-8.0',
            'macosx-x86_64-cuda-9.0',
            'macosx-x86_64-cuda-9.2',
            'linux-ppc64le-cuda-8.0',
            'linux-ppc64le-cuda-9.0',
            'linux-ppc64le-cuda-9.2',
            'linux-x86_64-cuda-8.0',
            'linux-x86_64-cuda-9.0',
            'linux-x86_64-cuda-9.2',
            'windows-x86_64-cuda-8.0',
            'windows-x86_64-cuda-9.0',
            'windows-x86_64-cuda-9.2',
    ]

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
        streamName = args.containsKey('streamName') ? args.streamName : ''
    }

    private void runBuildLogic() {
        if (platformName in ['linux-x86_64', 'linux-x86_64-generic']) {
            if (modulesToBuild.any { it =~ /^deeplearning4j|^datavec/ }) {
                updateVersion('scala', scalaVersion)
                updateVersion('spark', sparkVersion)
            } else if (modulesToBuild.any { it =~ /^arbiter|^scalnet|^nd4j/ }) {
                updateVersion('scala', scalaVersion)
            }
        }

        if (cudaVersion) {
            updateVersion('cuda', cudaVersion)
        }

        script.mvn getMvnCommand('build')
    }

    private void runTestLogic() {
        script.mvn getMvnCommand('test')
    }

    private void runDeployLogic() {
        script.mvn getMvnCommand('deploy')
    }

    protected void stagesToRun() {
        script.stage('Checkout') {
            getFancyStageDecorator('Checkout stage')
            script.checkout script.scm
        }

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
                script.stage('Test libnd4j in debug mode') {
                    libnd4jBuildMode = 'debug'
                    getFancyStageDecorator('Test libnd4j in debug mode stage')
                    runTestLogic()
                    libnd4jBuildMode = 'release'
                }

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

//            script.stage('Static code analysis') {
//                runStaticCodeAnalysisLogic()
//            }
//
//            script.stage('Security scan') {
//                runSecurityScanLogic()
//            }
        }
    }

    private List getMvnArguments(String stageName) {
        List mavenArguments = []

        if (modulesToBuild.any { it =~ /^libnd4j/ }) {
            mavenArguments.push("-Dlibnd4j.platform=${platformName}")

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
            }

            if (backend.contains('cuda')) {
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
                if (branchName != 'master') {
                    mavenArguments.push("-Dlibnd4j.compute=37")
                }

//                // FIXME: Workaround to skip tests for libnd4j (because we have no libnd4j tests for CUDA backend)
//                mavenArguments.push('-Dlibnd4j.test.skip=true')

                // FIXME: Workaround to fix dependencies problem if there is nd4j, datavec or deeplearning4j in project reactor, but changes were made only for libnd4j
                mavenArguments.push("-Djavacpp.platform=${platformName}")
            }
        }

        if (modulesToBuild.any { it =~ /^nd4j/ }) {
            mavenArguments.push('-P native-snapshots')
            mavenArguments.push('-P uberjar')
            mavenArguments.push("-Djavacpp.platform=${platformName}")

            if (!modulesToBuild.any { it =~ /^libnd4j/ } &&
                    (platformName != 'linux-x86_64' ||
                            (platformName == 'linux-x86_64' && cpuExtension))
            ) {
                mavenArguments.push('-P libnd4j-assembly')
            }

            if (backend == 'cpu') {
                if (stageName == 'test') {
                    mavenArguments.push('-P test-nd4j-native')
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
                if ((streamName == 'macosx-x86_64-cpu' || streamName == 'linux-x86_64-cpu') &&
                        stageName == 'test') {
                    mavenArguments.push('-DreuseForks=false')
                }
            }

            if (backend.contains('cuda')) {
                if (platformName.contains('linux')) {
                    mavenArguments.push('-DprotocCommand=protoc')
                }

                if (stageName == 'test') {
                    mavenArguments.push('-P test-nd4j-cuda-' + cudaVersion)
                }
            }
        }

        if (modulesToBuild.any { it =~ /^nd4j|^libnd4j/ }) {
            if (backend == 'cpu') {
                if (platformName == 'linux-x86_64' && !cpuExtension) {
                    mavenArguments.push('-P tf-cpu')
                    mavenArguments.push('-P nd4j-tf-cpu')
                }
            }

            if (backend.contains('cuda')) {
                if (platformName == 'linux-x86_64') {
                    mavenArguments.push('-P tf-gpu')
                    mavenArguments.push('-P nd4j-tf-gpu')
                }
            }
        }

        if (modulesToBuild.any { it =~ /^deeplearning4j|^nd4j|^libnd4j/ }) {
            if (stageName == 'test') {
                mavenArguments.push('-P testresources')
            }

            if (!modulesToBuild.any { it =~ /^libnd4j|^nd4j/ }) {
                mavenArguments.push('-P libnd4j-assembly')
            }
        }

        if (modulesToBuild.any { it =~ /^datavec/}) {
            if (!modulesToBuild.any { it =~ /^nd4j/}) {
                mavenArguments.push("-Djavacpp.platform=${platformName}")
                mavenArguments.push('-P libnd4j-assembly')
            }
        }

        mavenArguments
    }

    private String getMvnCommand(String stageName) {
        String mavenCommand

        Closure mavenProjects = {
            List projects = []
            List supportedModules = [
                    'libnd4j', 'nd4j', 'datavec', 'deeplearning4j', 'arbiter',
//                'nd4s',
                    'gym-java-client', 'rl4j', 'scalnet', 'jumpy'
            ]
            List mavenExcludesForNd4jNative = [
                    (platformName.contains('ios')) ?
                            '!nd4j/nd4j-backends/nd4j-backend-impls/nd4j-native-platform' : '',
                    '!nd4j/nd4j-backends/nd4j-backend-impls/nd4j-cuda',
                    '!nd4j/nd4j-backends/nd4j-backend-impls/nd4j-cuda-platform',
                    '!deeplearning4j/deeplearning4j-cuda'
            ]
            List mavenExcludesForNd4jCuda = [
                    '!nd4j/nd4j-backends/nd4j-backend-impls/nd4j-native',
                    '!nd4j/nd4j-backends/nd4j-backend-impls/nd4j-native-platform'
            ]
            List mavenExcludesForDeeplearning4jNative = [
                    '!deeplearning4j/deeplearning4j-cuda'
            ]

            if (modulesToBuild.any { it =~ /^deeplearning4j/}) {
                if (platformName == 'linux-x86_64' && backend == 'cpu' && !cpuExtension && libnd4jBuildMode == 'release') {
                    projects.addAll(mavenExcludesForDeeplearning4jNative)
                }
            }

            if (modulesToBuild.any { it =~ /^nd4j/ }) {
                if (platformName != 'linux-x86_64' || (platformName == 'linux-x86_64' && cpuExtension)) {
                    if (modulesToBuild.any { it =~ /^libnd4j/ }) {
                        projects.addAll(['libnd4j'])
                    }

                    if (backend == 'cpu') {
                        // FIXME: Temporary add nd4j to the list of projects to build to enable testresources profile (add test resources dependency).
                        projects.addAll(['nd4j', 'nd4j/nd4j-backends/nd4j-backend-impls/nd4j-native'])
                    }

                    if (backend.contains('cuda')) {
                        // FIXME: Temporary add nd4j to the list of projects to build to enable testresources profile (add test resources dependency).
                        projects.addAll(['nd4j', 'nd4j/nd4j-backends/nd4j-backend-impls/nd4j-cuda'])
                    }

                    return '-am -pl \'' + (projects).findAll().join(',') + '\''
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

                    if (backend.contains('cuda')) {
                        /* FIXME: Add this filter for now to not break the build when changes related to modules in excludes */
                        if (!modulesToBuild.any { mavenExcludesForNd4jCuda.contains(it) }) {
                            projects.addAll(mavenExcludesForNd4jCuda)
                        }
                    }

                    return '-pl \'' + (projects).findAll().join(',') + '\''
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

                    if (backend.contains('cuda')) {
                        projects.addAll(mavenExcludesForNd4jCuda)
                    }
                }

                return '-pl \'' + (projects).findAll().join(',') + '\''
            } else {
                return (modulesToBuild.sort() == supportedModules.sort() ? '-amd ' : '') +
                        '-pl \'' + (modulesToBuild + projects).findAll().join(',') + '\''
            }
        }

        List commonArguments = [
                // FIXME: -e -B -V not picked by Windows from withMaven pipeline step
                'mvn -e -B -V',
                (stageName == 'build') ? '-U clean install' :
                        (stageName == 'test') ? 'test' :
                                (stageName == 'deploy') ? 'deploy' : '',
                mavenProjects(),
                (stageName != 'test') ? '-Dmaven.test.skip=true' : '',
                (releaseApproved) ? "-P staging" : '',
                (releaseApproved && stageName == 'deploy') ?
                        "-DstagingRepositoryId=${script.env.STAGING_REPOSITORY}" : '',
                (releaseApproved && stageName == 'deploy') ? "-DperformRelease" : '',
                "-Dlocal.software.repository=${script.pipelineEnv.mvnProfileActivationName}"
        ]

        if (isUnixNode) {
            String devtoolsetVersion = backend?.contains('cuda') ? '4' : '7'

            mavenCommand = ([
                    "if [ -f /etc/redhat-release ]; " +
                            "then source /opt/rh/devtoolset-${devtoolsetVersion}/enable; fi;",
                    /* Pipeline withMaven step requires this line if it runs in Docker container */
                    'export PATH=$MVN_CMD_DIR:$PATH &&'
            ] + commonArguments + [
                    /* Workaround for MacOS/iOS which doesn't honour withMaven options */
                    (platformName.contains('macosx') || platformName.contains('ios')) ?
                            "-Dmaven.repo.local=" +
                                    "${script.env.WORKSPACE}/" +
                                    "${script.pipelineEnv.localRepositoryPath}" : ''
            ] + getMvnArguments(stageName)).findAll().join(' ')
        } else {
            mavenCommand = ([
                    'vcvars64.bat',
                    '&&',
                    'bash -c',
                    '"' + 'export PATH=$PATH:/c/msys64/mingw64/bin &&'
            ] + commonArguments + [
                    /* Workaround for Windows which doesn't honour withMaven options */
                    "-Dmaven.repo.local=" +
                            "${script.env.WORKSPACE.replaceAll('\\\\', '/')}/" +
                            "${script.pipelineEnv.localRepositoryPath}",
                    '-s ${MAVEN_SETTINGS}'
            ] + getMvnArguments(stageName)).findAll().join(' ') + '"'
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
//            TODO: add implementation of updateDependencyVersions method
//            updateDependencyVersions(releaseVersion)
            script.setProjectVersion(releaseVersion, true)
        }
    }

    private void populateGpgKeys() {
        script.withCredentials([
                script.file(credentialsId: 'gpg-pub-key-jenkins', variable: 'GPG_PUBRING'),
                script.file(credentialsId: 'gpg-private-key-jenkins', variable: 'GPG_SECRING'),
                script.usernameColonPassword(credentialsId: 'gpg-password-test-1', variable: 'GPG_PASS')
        ]) {
            if (isUnixNode) {
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
        int charsNumber = Math.round((78-text.length())/2)

        script.echo("*" * charsNumber + text + "*" * charsNumber)
    }

    private void parseTestResults(String testStageOutput) {
//        def testStageOutput = script.currentBuild.rawBuild.getAction(AbstractTestResultAction.class)

//        if (testStageOutput != null) {
//            def total = testStageOutput.totalCount
//            def failed = testStageOutput.failCount
//            def skipped = testStageOutput.skipCount
//            def passed = total - failed - skipped
//
//            script.echo "Test Status:\n  Passed: ${passed}, Failed: ${failed} ${testStageOutput.failureDiffString}, Skipped: ${skipped}"
//        }
        for (l in testStageOutput.tokenize('\n')) {
            String line = l

            if (line.contains('Tests run:')) {
                script.echo "Test results: ${line}"
            }
        }
    }
}
