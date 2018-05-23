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

    /**
     * Module class constructor
     *
     * @param Map args constructor arguments
     * @param pipeline script object
     */
    Module(Map args, script) {
        this.script = script
        backend = args.containsKey('backend') ?
                args.backend :
                script.error('Missing backend argument!')
        branchName = args.containsKey('branchName') ?
                args.branchName :
                script.error('Missing branchName argument!')
        cpuExtension = args.containsKey('cpuExtension') ? args.cpuExtension : ''
        cudaVersion = backend.contains('cuda') ? backend.tokenize('-')[1] : ''
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
    }

    protected void runBuildLogic() {
        if (modulesToBuild.any { it =~ /^deeplearning4j|^datavec/ }) {
            updateVersion('scala', scalaVersion)
            updateVersion('spark', sparkVersion)
        } else if (modulesToBuild.any { it =~ /^arbiter|^scalnet|^nd4j/ }) {
            updateVersion('scala', scalaVersion)
        }

        if (cudaVersion) {
            updateVersion('cuda', cudaVersion)
        }

        script.mvn getMvnCommand('build')
    }

    protected void runTestLogic() {
        String testCommand = getMvnCommand('test')

        switch (platformName) {
//            case ~/^android.*$/:
//            case ['linux-x86_64', 'linux-x86_64-generic']:
//                testCommand = """\
//                    if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable; fi
//                    ${testCommand}
//                """.stripIndent()
//                break
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
                break
        }

        script.mvn testCommand

//        TODO: Add archive test artifacts for libnd4j
        /* Archiving test results */
//        script.junit testResults: "**/${backend}_test_results.xml", allowEmptyResults: true
    }

    protected void runDeployLogic() {
        script.mvn getMvnCommand('deploy')
    }

    protected void stagesToRun() {
        script.stage('Checkout') {
            script.checkout script.scm
        }

        if (branchName.contains(releaseBranchPattern)) {
            script.stage("Prepare for Release") {
                setupEnvForRelease()
            }
        }

        script.stage('Build') {
            runBuildLogic()
        }

//        if (!branchName.contains(releaseBranchPattern)) {
//            script.stage('Test') {
//                runTestLogic()
//            }
//        }
//        script.stage('Static code analysis') {
//            runStaticCodeAnalysisLogic()
//        }
//
//        script.stage('Security scan') {
//            runSecurityScanLogic()
//        }

        if (branchName == 'master' || branchName.contains(releaseBranchPattern)) {
            script.stage('Deploy') {
                runDeployLogic()
            }
        }
    }

    private List getMavenBuildArguments() {
        List mavenArguments = []

        if (modulesToBuild.any { it =~ /^libnd4j/ }) {
            mavenArguments.push("-Dlibnd4j.platform=${platformName}")

            if (backend == 'cpu') {
                mavenArguments.push("-Dmaven.libnd4j.test.skip=true")

                if (cpuExtension) {
                    mavenArguments.push("-Dlibnd4j.extension=${cpuExtension}")
                }
            }

            if (backend.contains('cuda')) {
                mavenArguments.push("-Dlibnd4j.cuda=${cudaVersion}")

                if (branchName != 'master') {
                    mavenArguments.push("-Dlibnd4j.compute=30")
                }
            }
        }

        if (modulesToBuild.any { it =~ /^nd4j/ }) {
            mavenArguments.push('-P native-snapshots')
            mavenArguments.push('-P uberjar')

            if (!modulesToBuild.any { it =~ /^libnd4j/ }) {
                mavenArguments.push('-P libnd4j-assembly')
            }

            if (backend == 'cpu') {
                mavenArguments.push("-Djavacpp.platform=${platformName}")

                if (cpuExtension) {
                    mavenArguments.push("-Djavacpp.extension=${cpuExtension}")
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
            }

            if (backend.contains('cuda')) {
                if (platformName.contains('linux')) {
                    mavenArguments.push('-DprotocCommand=protoc')
                }
            }
        }

        mavenArguments
    }

    private List getMavenTestArguments() {
        List mavenArguments = []

        if (modulesToBuild.any { it =~ /^libnd4j/ }) {
            mavenArguments.push("-Dlibnd4j.platform=${platformName}")
            mavenArguments.push("-Dclean.skip=true")

            if (backend == 'cpu') {
                if (cpuExtension) {
                    mavenArguments.push("-Dlibnd4j.extension=${cpuExtension}")
                }
            }

            if (backend.contains('cuda')) {
                mavenArguments.push("-Dlibnd4j.cuda=${cudaVersion}")

                if (branchName != 'master') {
                    mavenArguments.push("-Dlibnd4j.compute=30")
                }
            }
        }

        if (modulesToBuild.any { it =~ /^nd4j/ }) {
//            FIXME: temporary remove this profile for libnd4j tests
//            mavenArguments.push('-P testresources')
            mavenArguments.push('-P native-snapshots')
            mavenArguments.push('-P uberjar')

            if (!modulesToBuild.any { it =~ /^libnd4j/ }) {
                mavenArguments.push('-P libnd4j-assembly')
            }

            if (backend == 'cpu') {
                mavenArguments.push("-Djavacpp.platform=${platformName}")

                if (cpuExtension) {
                    mavenArguments.push("-Djavacpp.extension=${cpuExtension}")
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
            }

            if (backend.contains('cuda')) {
                if (platformName.contains('linux')) {
                    mavenArguments.push('-DprotocCommand=protoc')
                }
            }
        }
//            FIXME: temporary remove this profile for libnd4j tests
//        if (modulesToBuild.any { it =~ /^deeplearning4j/ }) {
//            mavenArguments.push('-P testresources')
//        }

        mavenArguments
    }

    private List getMavenDeployArguments() {
        List mavenArguments = []

        if (modulesToBuild.any { it =~ /^libnd4j/ }) {
            mavenArguments.push("-Dlibnd4j.platform=${platformName}")

            if (backend == 'cpu') {
                mavenArguments.push("-Dmaven.libnd4j.test.skip=true")

                if (cpuExtension) {
                    mavenArguments.push("-Dlibnd4j.extension=${cpuExtension}")
                }
            }

            if (backend.contains('cuda')) {
                mavenArguments.push("-Dlibnd4j.cuda=${cudaVersion}")

                if (branchName != 'master') {
                    mavenArguments.push("-Dlibnd4j.compute=30")
                }
            }
        }

        if (modulesToBuild.any { it =~ /^nd4j/ }) {
            mavenArguments.push('-P native-snapshots')
            mavenArguments.push('-P uberjar')

            if (!modulesToBuild.any { it =~ /^libnd4j/ }) {
                mavenArguments.push('-P libnd4j-assembly')
            }

            if (backend == 'cpu') {
                mavenArguments.push("-Djavacpp.platform=${platformName}")

                if (cpuExtension) {
                    mavenArguments.push("-Djavacpp.extension=${cpuExtension}")
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
            }

            if (backend.contains('cuda')) {
                if (platformName.contains('linux')) {
                    mavenArguments.push('-DprotocCommand=protoc')
                }
            }
        }

        mavenArguments
    }

    protected String getMvnCommand(String stageName) {
        List mavenArguments
        List mavenBuildArguments = getMavenBuildArguments()
        List mavenTestArguments = getMavenTestArguments()
        List mavenDeployArguments = getMavenDeployArguments()

        Closure mavenProjects = {
            List projects = []
            List mavenExcludesForNd4jNative = [
                    (platformName.contains('ios')) ?
                            '!nd4j/nd4j-backends/nd4j-backend-impls/nd4j-native-platform' : '',
                    '!nd4j/nd4j-backends/nd4j-backend-impls/nd4j-cuda',
                    '!nd4j/nd4j-backends/nd4j-backend-impls/nd4j-cuda-platform',
                    '!nd4j/nd4j-backends/nd4j-tests'
            ]
            List mavenExcludesForNd4jCuda = [
                    '!nd4j/nd4j-backends/nd4j-backend-impls/nd4j-native',
                    '!nd4j/nd4j-backends/nd4j-backend-impls/nd4j-native-platform',
                    '!nd4j/nd4j-backends/nd4j-tests'
            ]

            if (modulesToBuild.any { it =~ /^nd4j/ }) {
                if (backend == 'cpu') {
                    projects.addAll(mavenExcludesForNd4jNative)
                }

                if (backend.contains('cuda')) {
                    projects.addAll(mavenExcludesForNd4jCuda)
                }
            }

            '-pl \'' + (modulesToBuild + projects).findAll().join(',') + '\''
        }

        List commonArguments = [
                'mvn -B -amd',
                (stageName == 'build') ? '-U' : '',
                (stageName == 'build') ? 'clean install' :
                        (stageName == 'test') ? 'test' :
                                (stageName == 'deploy') ? 'deploy' : '',
                (modulesToBuild.any { it =~ /^libnd4j|^nd4j/ }) ?
                        '-P ci-build-backend-modules' : '',
                (modulesToBuild.any { it =~ /^deeplearning4j|^datavec/ }) ?
                        '-P ci-build-multiplatform-projects' : '',
                (modulesToBuild.any { it =~ /^arbiter|^gym-java-client|^rl4j|^scalnet|^jumpy/ }) ?
                        '-P ci-build-generic-modules' : '',
                mavenProjects(),
//                FIXME: workaround to run tests for cpu backend
//                (stageName != 'test') ? '-Dmaven.test.skip=true' : '',
                '-Dmaven.test.skip=true',
                (releaseApproved) ? "-P staging" : '',
                (releaseApproved && stageName == 'deploy') ?
                        "-DstagingRepositoryId=${script.env.STAGING_REPOSITORY}" : '',
                (releaseApproved && stageName == 'deploy') ? "-DperformRelease" : '',
                "-Dlocal.software.repository=${script.pipelineEnv.mvnProfileActivationName}"
        ]

        if (isUnixNode) {
            String devtoolsetVersion = cpuExtension ? '6' : (stageName == 'test') ? '3' : '4'

            mavenArguments = [
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
            ]
        } else {
            mavenArguments = [
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
            ]
        }

        switch (stageName) {
            case 'build':
                if (isUnixNode) {
                    return mavenArguments.plus(mavenBuildArguments).findAll().join(' ')
                } else {
                    return mavenArguments.plus(mavenBuildArguments).findAll().join(' ') + '"'
                }
                break
            case 'test':
                if (isUnixNode) {
                    return mavenArguments.plus(mavenTestArguments).findAll().join(' ')
                } else {
                    return mavenArguments.plus(mavenTestArguments).findAll().join(' ') + '"'
                }
                break
            case 'deploy':
                if (isUnixNode) {
                    return mavenArguments.plus(mavenDeployArguments).findAll().join(' ')
                } else {
                    return mavenArguments.plus(mavenDeployArguments).findAll().join(' ') + '"'
                }
                break
            default:
                throw new IllegalArgumentException('Stage is not supported yet')
                break
        }
    }

    protected void updateVersion(String updateTarget, String version) {
        if (isUnixNode) {
            script.sh "bash ./change-${updateTarget.toLowerCase()}-versions.sh ${version}"
        } else {
            script.bat "\"C:\\Program Files\\Git\\bin\\bash.exe\" -c " +
                    "\"./change-${updateTarget.toLowerCase()}-versions.sh ${version}\""
        }
    }

    protected void setupEnvForRelease() {
        if (releaseApproved) {
            populateGpgKeys()
            updateGitCredentials()
//            TODO: add implementation of updateDependencyVersions method
//            updateDependencyVersions(releaseVersion)
            script.setProjectVersion(releaseVersion, true)
        }
    }

    protected void populateGpgKeys() {
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

    protected void updateGitCredentials() {
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
}
