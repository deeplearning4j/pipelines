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

//        TODO: Add archive test artifacts for libnd4j
        /* Archiving test results */
//        script.junit testResults: "**/${backend}_test_results.xml", allowEmptyResults: true
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

        script.stage('Build') {
            getFancyStageDecorator('Build stage')
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
                getFancyStageDecorator('Deploy stage')
                runDeployLogic()
            }
        }
    }

    private List getMvnArguments(String stageName) {
        List mavenArguments = []

        if (modulesToBuild.any { it =~ /^libnd4j/ }) {
            mavenArguments.push("-Dlibnd4j.platform=${platformName}")

            if (backend == 'cpu') {
                List platformExcludesForTests = [
                        'android-arm',
                        'android-arm64',
                        'android-x86',
                        'android-x86_64',
                        'ios-arm64',
                        'ios-x86_64'
                ]

                // FIXME: Workaround to enable tests only for supported by current infra platforms
                if (!platformExcludesForTests.contains(platformName)) {
                    // FIXME: Skipping tests for release branches to reduce the build time
                    if (!branchName.contains(releaseBranchPattern)) {
                        mavenArguments.push('-Dlibnd4j.test.skip=false')
                        mavenArguments.push('-Dlibnd4j.tests.phase=process-resources')
                    }
                }

                // According to raver119 debug build mode for tests should be enable only for linux-x86_64-cpu
                if (!cpuExtension && platformName == 'linux-x86_64') {
                    mavenArguments.push('-Dlibnd4j.test.is.release.build=false')
                }

                if (stageName in ['test', 'deploy']) {
                    mavenArguments.push('-Dlibnd4j.cpu.compile.skip=true')
                }

                if (cpuExtension) {
                    mavenArguments.push("-Dlibnd4j.extension=${cpuExtension}")
                }
            }

            if (backend.contains('cuda')) {
                mavenArguments.push("-Dlibnd4j.cuda=${cudaVersion}")

                if (platformName != 'linux-x86_64') {
                    mavenArguments.push('-Dlibnd4j.cpu.compile.skip=true')
                }

                if (stageName in ['test', 'deploy']) {
                    mavenArguments.push('-Dlibnd4j.cuda.compile.skip=true')
                }

                // Set CC to 30 to increase build speed for PR and ordinary branches
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

        if (modulesToBuild.any { it =~ /^deeplearning4j|^nd4j/ }) {
            if (stageName == 'test') {
                mavenArguments.push('-P testresources')
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
                    '!nd4j/nd4j-backends/nd4j-backend-impls/nd4j-cuda-platform'
            ]
            List mavenExcludesForNd4jCuda = [
                    '!nd4j/nd4j-backends/nd4j-backend-impls/nd4j-native',
                    '!nd4j/nd4j-backends/nd4j-backend-impls/nd4j-native-platform'
            ]

            if (modulesToBuild.any { it =~ /^nd4j/ }) {
                if (platformName != 'linux-x86_64' || (platformName == 'linux-x86_64' && cpuExtension)) {
                    if (modulesToBuild.any { it =~ /^libnd4j/ }) {
                        projects.addAll(['libnd4j'])
                    }

                    if (backend == 'cpu') {
                        projects.addAll(['nd4j/nd4j-backends/nd4j-backend-impls/nd4j-native'])
                    }

                    if (backend.contains('cuda')) {
                        projects.addAll(['nd4j/nd4j-backends/nd4j-backend-impls/nd4j-cuda'])
                    }

                    return '-am -pl \'' + (projects).findAll().join(',') + '\''
                } else {
                    /* FIXME: Looks like with lates changes above (first if) current if statement will never be triggered. */
                    if (backend == 'cpu') {
                        if (!modulesToBuild.any { mavenExcludesForNd4jNative.contains(it) }) {
                            projects.addAll(mavenExcludesForNd4jNative)
                        }
                    }

                    if (backend.contains('cuda')) {
                        /* FIXME: Add this filter for now to not break the build when changes related to modules in excludes */
                        if (!modulesToBuild.any { mavenExcludesForNd4jCuda.contains(it) }) {
                            projects.addAll(mavenExcludesForNd4jCuda)
                        }
                    }
                    return (modulesToBuild.sort() == supportedModules.sort() ? '-amd ' : '-am ') +
                            '-pl \'' + (modulesToBuild + projects).findAll().join(',') + '\''
                }
            } else if (modulesToBuild.any { it =~ /^libnd4j/ }) {
                if (platformName != 'linux-x86_64' || (platformName == 'linux-x86_64' && backend == 'cpu')) {
                    projects.addAll(['libnd4j'])

                    return '-am -pl \'' + (projects).findAll().join(',') + '\''
                } else {
                    return (modulesToBuild.sort() == supportedModules.sort() ? '-amd ' : '-am ') +
                            '-pl \'' + (modulesToBuild + projects).findAll().join(',') + '\''
                }
            } else {
                return (modulesToBuild.sort() == supportedModules.sort() ? '-amd ' : '-am ') +
                        '-pl \'' + (modulesToBuild + projects).findAll().join(',') + '\''
            }
        }

        List commonArguments = [
                'mvn -B',
                (stageName == 'build') ? '-U' : '',
                (stageName == 'build') ? 'clean install' :
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
            String devtoolsetVersion = cpuExtension ? '6' : '4'

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
}
