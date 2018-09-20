package skymind.pipelines.projects

import groovy.json.JsonSlurper


abstract class Project implements Serializable {
    protected script
    protected platforms
    protected final String branchName
    protected final String projectName
    /* Default job properties */
    protected final List jobSpecificProperties = []
    protected static String gitterEndpointUrl = ''
    protected boolean releaseApproved = false
    protected static String releaseBranchPattern = 'release'
    protected static String releaseVersion
    protected static String snapshotVersion

    /**
     * Project class constructor
     *
     * @param pipeline script object
     * @param projectName project name
     * @param jobConfig configuration of job/run environment
     */
    Project(Object script, String projectName, Map jobConfig) {
        this.script = script
        this.projectName = projectName
        branchName = this.script.env.BRANCH_NAME
        /* Default platforms will be used if developer didn't redefine them in Jenkins file */
        platforms = jobConfig?.getAt('platforms') ?: getDefaultPlatforms(this.projectName)
        /* Configure job build parameters */
        setBuildParameters(jobSpecificProperties)
        /* Terminate older builds */
        terminateOlderBuilds(this.script.env.JOB_NAME, this.script.env.BUILD_NUMBER.toInteger())
    }

    abstract void initPipeline()

    @NonCPS
    protected void setBuildParameters(List jobSpecificProperties) {
        List commonJobProperties = []

        if (script.env.JOB_BASE_NAME == 'master') {
            commonJobProperties.addAll([
                    script.pipelineTriggers([script.cron('@midnight')])
            ])
        }

        script.properties(commonJobProperties + jobSpecificProperties)
    }

    @NonCPS
    protected terminateOlderBuilds(String jobName, int buildsNumber) {
        def currentJob = Jenkins.instance.getItemByFullName(jobName)

        for (def build : currentJob.builds) {
            if (build.isBuilding() && build.number.toInteger() != buildsNumber) {
                build.doStop()
                script.echo "[WARNING] Build number ${build.number} was " +
                        "terminated because of current(latest) run."
            }
        }
    }

    protected void pipelineWrapper(Closure pipelineBody) {
        try {
            pipelineBody()
        }
        catch (error) {
            script.echo "[ERROR] ${error}"
            script.currentBuild.result = script.currentBuild.result ?: 'FAILURE'
        }
        finally {
            script.notifier.sendEmailNotification(script.currentBuild.result)
        }
    }

    protected void allocateBuildNode(Closure stagesToRun) {
        for (Map pltm : platforms) {
            Map platform = pltm
            String platformName = platform.name

            script.node(platformName) {
                pipelineWrapper {
                    try {
                        script.stage('Checkout') {
                            script.deleteDir()

                            script.dir(projectName) {
                                script.checkout script.scm
                            }
                        }

                        stagesToRun()
                    }
                    finally {
                        script.cleanWs deleteDirs: true
                    }
                }
            }
        }
    }

    protected String getMvnCommand(String stageName, List mvnArguments = []) {
        Boolean unixNode = script.isUnix()

        switch (stageName) {
            case 'build':
                if (unixNode) {
                    return [
                            'if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-4/enable ; fi ;',
                            /* Pipeline withMaven step requires this line if it runs in Docker container */
                            'export PATH=$MVN_CMD_DIR:$PATH &&',
                            'mvn -U -B',
                            'clean',
                            'install',
                            "-Dlocal.software.repository=${script.pipelineEnv.mvnProfileActivationName}",
                            '-Dmaven.test.skip=true',
                            (releaseApproved) ? "-P staging" : '',
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
                            (releaseApproved) ? "-P staging" : ''
                    ].plus(mvnArguments).findAll().join(' ') + '"'
                }
                break
            case 'test':
                if (unixNode) {
                    return [
                            'if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-4/enable ; fi ;',
                            /* Pipeline withMaven step requires this line if it runs in Docker container */
                            'export PATH=$MVN_CMD_DIR:$PATH &&',
                            'mvn -B',
                            'test',
                            "-Dlocal.software.repository=${script.pipelineEnv.mvnProfileActivationName}",
                            (releaseApproved) ? "-P staging" : ''
                    ].plus(mvnArguments).findAll().join(' ')
                } else {
                    return [
                            'vcvars64.bat',
                            '&&',
                            'bash -c',
                            '"' + 'export PATH=$PATH:/c/msys64/mingw64/bin &&',
                            'mvn -B',
                            'test',
                            "-Dlocal.software.repository=${script.pipelineEnv.mvnProfileActivationName}",
                            /* Workaround for Windows which doesn't honour withMaven options */
                            "-Dmaven.repo.local=${script.pipelineEnv.localRepositoryPath}",
                            (releaseApproved) ? "-P staging" : ''
                    ].plus(mvnArguments).findAll().join(' ') + '"'
                }
                break
            case 'deploy':
                if (unixNode) {
                    return [
                            "if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-4/enable; fi;",
                            /* Pipeline withMaven step requires this line if it runs in Docker container */
                            'export PATH=$MVN_CMD_DIR:$PATH &&',
                            'mvn -B',
                            'deploy',
                            "-Dlocal.software.repository=${script.pipelineEnv.mvnProfileActivationName}",
                            (releaseApproved) ? "-DstagingRepositoryId=${script.env.STAGING_REPOSITORY}" : '',
                            (releaseApproved) ? "-DperformRelease" : '',
                            (releaseApproved) ? "-P staging" : '',
                            '-Dmaven.test.skip=true'
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
                    ].plus(mvnArguments).findAll().join(' ') + '"'
                }
                break
            case 'build-test-resources':
                if (unixNode) {
                    return [
                            "if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-4/enable; fi;",
                            /* Pipeline withMaven step requires this line if it runs in Docker container */
                            'export PATH=$MVN_CMD_DIR:$PATH &&',
                            'mvn -U -B',
                            'clean',
                            (branchName == 'master') ? 'deploy' : 'install',
                            "-Dlocal.software.repository=ci-nexus",
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
                            (branchName == 'master') ? 'deploy' : 'install',
                            "-Dlocal.software.repository=ci-nexus",
                            /* Workaround for Windows which doesn't honour withMaven options */
                            '-s ${MAVEN_SETTINGS}',
                            "-Dmaven.repo.local=" +
                                    "${script.env.WORKSPACE.replaceAll('\\\\', '/')}/" +
                                    "${script.pipelineEnv.localRepositoryPath}",
                            (releaseApproved) ? "-P staging" : ''
                    ].plus(mvnArguments).findAll().join(' ') + '"'
                }
                break
            default:
                throw new IllegalArgumentException('Stage is not supported yet')
                break
        }
    }

    protected void runBuild() {
        script.mvn getMvnCommand('build')
    }

    protected void runTests() {
        script.mvn getMvnCommand('test')
    }

    protected void getReleaseParameters() {
        def userInput

        script.timeout(time: 1, unit: 'HOURS') {
            userInput = script.input message: 'Perform release?',
                    parameters: [
                            script.string(defaultValue: '', description: 'Release version', name: 'releaseVersion'),
                            script.string(defaultValue: '', description: 'Snapshot version', name: 'snapshotVersion'),
                            script.string(defaultValue: '', description: 'Staging repository ID', name: 'stagingRepository'),
                    ],
                    submitter: 'sshepel, saudet, agibsonccc',
                    submitterParameter: 'approvedBy'
        }

        if (!userInput) {
            script.error "[ERROR] Missing user-provided values."
        }

        if (checkStagingRepoFormat(userInput.stagingRepository)) {
            releaseApproved = (userInput.approvedBy) ? true : script.error("[ERROR] Can't get approver ID.")
            releaseVersion = userInput.releaseVersion
            snapshotVersion = userInput.snapshotVersion
            script.env.STAGING_REPOSITORY = userInput.stagingRepository
        } else {
            script.error "[ERROR] Provided staging repository ID ${script.env.STAGING_REPOSITORY} is not valid."
        }
    }

    protected void setupEnvForRelease() {
        if (releaseApproved) {
            populateGpgKeys()
            updateGitCredentials()
            updateVersions(releaseVersion)
            script.setProjectVersion(releaseVersion, true)
        }
    }

    protected void runDeploy() {
        script.mvn getMvnCommand('deploy')
    }

    protected void runBuildTestResources(String platform = 'linux-x86_64') {
        String dl4jTestResourcesGitFolderName = 'dl4j-test-resources'
        String dl4jTestResourcesGitUrl = 'https://github.com/deeplearning4j/dl4j-test-resources.git'

        script.checkout([
                $class                           : 'GitSCM',
                branches                         : [[name: '*/master']],
                doGenerateSubmoduleConfigurations: false,
                extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                     relativeTargetDir: "$dl4jTestResourcesGitFolderName"],
                                                    [$class      : 'CloneOption',
                                                     honorRefspec: true,
                                                     noTags      : true,
                                                     reference   : '',
                                                     shallow     : true]],
                submoduleCfg                     : [],
                userRemoteConfigs                : [[url: "$dl4jTestResourcesGitUrl"]]
        ])

        script.dir(dl4jTestResourcesGitFolderName) {
            String mvnCommand = getMvnCommand('build-test-resources', [
                    (platform.contains('macosx') || platform.contains('ios')) ?
                            "-Dmaven.repo.local=${script.env.WORKSPACE}/${script.pipelineEnv.localRepositoryPath}" :
                            ''
            ])

            script.mvn "$mvnCommand"

            script.deleteDir()
        }
    }

    protected getProjectObjectModel() {
        String pomFileName = 'pom.xml'
        Boolean pomExists = script.fileExists(pomFileName)

        return (pomExists) ? script.readMavenPom() : script.error('pom.xml file not found')
    }

    protected boolean checkStagingRepoFormat(String stagingRepoId) {
        stagingRepoId =~ /\w+-\d+/
    }

    protected void updateVersions(String version) {}

    protected void populateGpgKeys() {
        script.withCredentials([
                script.file(credentialsId: 'gpg-pub-key-jenkins', variable: 'GPG_PUBRING'),
                script.file(credentialsId: 'gpg-private-key-jenkins', variable: 'GPG_SECRING'),
                script.usernameColonPassword(credentialsId: 'gpg-password-test-1', variable: 'GPG_PASS')
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

    protected void updateGitCredentials() {
        if (script.isUnix()) {
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

    @NonCPS
    protected List getDefaultPlatforms(String projectName) {
        List defaultPlatforms
        switch (projectName) {
            case ['libnd4j', 'nd4j']:
                defaultPlatforms = [
                        [name: 'android-arm', scalaVersion: '2.10', backend: 'cpu'],
                        [name: 'android-arm64', scalaVersion: '2.11', backend: 'cpu'],
                        [name: 'android-x86', scalaVersion: '2.10', backend: 'cpu'],
                        [name: 'android-x86_64', scalaVersion: '2.11', backend: 'cpu'],

                        [name: 'ios-arm64', scalaVersion: '2.10', backend: 'cpu'],
                        [name: 'ios-x86_64', scalaVersion: '2.11', backend: 'cpu'],

                        [name: 'linux-ppc64le', scalaVersion: '2.11', backend: 'cpu'],
                        [name: 'linux-ppc64le', scalaVersion: '2.10', backend: 'cuda-8.0'],
                        [name: 'linux-ppc64le', scalaVersion: '2.11', backend: 'cuda-9.0'],
                        [name: 'linux-ppc64le', scalaVersion: '2.11', backend: 'cuda-9.1'],

                        [name: 'linux-x86_64', scalaVersion: '2.10', backend: 'cpu'],
                        [name: 'linux-x86_64', scalaVersion: '2.11', backend: 'cpu', cpuExtension: 'avx2'],
                        [name: 'linux-x86_64', scalaVersion: '2.11', backend: 'cpu', cpuExtension: 'avx512'],
                        [name: 'linux-x86_64', scalaVersion: '2.10', backend: 'cuda-8.0'],
                        [name: 'linux-x86_64', scalaVersion: '2.11', backend: 'cuda-9.0'],
                        [name: 'linux-x86_64', scalaVersion: '2.11', backend: 'cuda-9.1'],

                        [name: 'macosx-x86_64', scalaVersion: '2.10', backend: 'cpu'],
                        [name: 'macosx-x86_64', scalaVersion: '2.11', backend: 'cpu', cpuExtension: 'avx2'],
                        /*
                             FIXME: avx512 required Xcode 9.2 to be installed on Mac slave,
                             at the same time for CUDA - Xcode 8 required,
                             which means that we can't enable avx512 builds at the moment
                          */
//                        [name: 'macosx-x86_64', scalaVersion: '2.11', backend: 'cpu', cpuExtension: 'avx512'],
                        [name: 'macosx-x86_64', scalaVersion: '2.10', backend: 'cuda-8.0'],
                        [name: 'macosx-x86_64', scalaVersion: '2.11', backend: 'cuda-9.0'],
                        [name: 'macosx-x86_64', scalaVersion: '2.11', backend: 'cuda-9.1'],

                        [name: 'windows-x86_64', scalaVersion: '2.10', backend: 'cpu'],
                        [name: 'windows-x86_64', scalaVersion: '2.11', backend: 'cpu', cpuExtension: 'avx2'],
                        /* FIXME: avx512 */
//                        [name: 'windows-x86_64', scalaVersion: '2.11', backend: 'cpu', cpuExtension: 'avx512'],
                        [name: 'windows-x86_64', scalaVersion: '2.10', backend: 'cuda-8.0'],
                        [name: 'windows-x86_64', scalaVersion: '2.11', backend: 'cuda-9.0'],
                        [name: 'windows-x86_64', scalaVersion: '2.11', backend: 'cuda-9.1']
                ]
                break
            case 'deeplearning4j':
                defaultPlatforms = [
                        [name: 'linux-x86_64']
                ]
                break
            default:
                defaultPlatforms = [
                        [name: 'linux-x86_64-generic']
                ]
                break
        }

        defaultPlatforms
    }

    protected Map parseCheckoutDetails() {
        Closure shellCommand = { String command ->
            return script.sh(script: command, returnStdout: true).trim()
        }

        String gitCommitId = shellCommand('git log -1 --pretty=%H')

        return [GIT_BRANCH: script.env.BRANCH_NAME,
                GIT_COMMIT: gitCommitId,
                GIT_COMMITER_NAME: shellCommand("git --no-pager show -s --format='%an' ${gitCommitId}"),
                GIT_COMMITER_EMAIL: shellCommand("git --no-pager show -s --format='%ae' ${gitCommitId}"),
                GIT_COMMIT_MESSAGE: shellCommand("git log -1 --pretty=%B ${gitCommitId}")]
    }

    protected Boolean isMemberOrCollaborator(String committerFullName) {
        String authCredentialsId = 'skymindops-username-and-token'
        String usersSearchUrl = "https://api.github.com/search/users?q=${committerFullName.replaceAll(' ', '+')}+in:fullname&type=Users"

        String userDetails = script.httpRequest(url: usersSearchUrl,
                timeout: 60,
                authentication: authCredentialsId,
                quiet: true).content

        // WARNING: fetching first username from the search results may cause wrong recipient notifications on organization side.
        String committerUsername = new JsonSlurper().parseText(userDetails).items[0].login

        String memberQueryUrl = "https://api.github.com/orgs/deeplearning4j/members/${committerUsername}"

        int isMemberResponse = script.httpRequest(url: memberQueryUrl,
                timeout: 60,
                authentication: authCredentialsId,
                quiet: true,
                validResponseCodes: '100:404').status

        return (isMemberResponse == 204)

//    Not working with provided credentials
//    String collaboratorsQueryUrl = "https://api.github.com/orgs/deeplearning4j/outside_collaborators"
//    def isCollaboratorResponse = httpRequest url: collaboratorsQueryUrl,
//            timeout: 120,
//            authentication: authCredentialsId,
//            quiet: true

//    return (isMemberResponse.status == '204' || username in isCollaboratorResponse.content.login)
    }
}
