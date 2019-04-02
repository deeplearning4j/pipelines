package skymind.pipelines.projects

import groovy.json.JsonSlurper


abstract class Project implements Serializable {
    protected script
    protected List platforms
    protected final String branchName
    protected final String projectName
    protected final List jobSpecificProperties = []
    protected boolean releaseApproved = false
    protected static releaseBranchPattern = /^release\/.*$/
    protected static String releaseVersion
    protected static String snapshotVersion
    protected Map checkoutDetails
    protected boolean isMember
    protected List testResults = []
    public boolean release = false

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
            if (script.currentBuild.rawBuild.getAction(jenkins.model.InterruptedBuildAction.class) ||
                    error instanceof org.jenkinsci.plugins.workflow.steps.FlowInterruptedException ||
                    error instanceof java.lang.InterruptedException ||
                    (error instanceof hudson.AbortException &&
                            (error?.message?.contains('script returned exit code 143') ||
                                    error?.message?.contains('Queue task was cancelled')))
            ) {
                script.currentBuild.result = 'ABORTED'
            } else {
                script.currentBuild.result = 'FAILURE'
            }

            script.echo "[ERROR] ${error}" +
                    (error.cause ? '\n' + "Cause is ${error.cause}" : '') +
                    (error.stackTrace ? '\n' + 'StackTrace: ' + error.stackTrace.join('\n') : '')
        }
        finally {
            script.notifier.sendSlackNotification jobResult: script.currentBuild.result,
                    checkoutDetails: checkoutDetails, isMember: isMember, testResults: testResults
        }
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
                    submitter: 'sshepel, saudet, agibsonccc, wmeddie, maxpumperla, ShamsUlAzeem',
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
            case ['skil-clients', 'zeppelin', 'dl4j-test-resources']:
                defaultPlatforms = [
                        [name: 'linux-x86_64-generic']
                ]
                break
            case 'skil-java':
                defaultPlatforms = [
                        [name: 'linux-x86_64-skil-java']
                ]
                break
            case ['skil-python', 'strumpf']:
                defaultPlatforms = [
                        [name: 'linux-x86_64', pythonVersion: '2'],
                        [name: 'linux-x86_64', pythonVersion: '3']
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

        // Required for skil-server
        script.env.GIT_COMMIT = gitCommitId

        return [GIT_BRANCH        : branchName,
                GIT_COMMIT        : gitCommitId,
                GIT_COMMITER_NAME : shellCommand("git --no-pager show -s --format='%an' ${gitCommitId}"),
                GIT_COMMITER_EMAIL: shellCommand("git --no-pager show -s --format='%ae' ${gitCommitId}"),
                // Change pretty format from -pretty=%B to --pretty=format:'%s%n%n%b' because of old version of git (1.7.1)
                GIT_COMMIT_MESSAGE: shellCommand("git log -1 --pretty=format:'%s%n%n%b' ${gitCommitId}")]
    }

    protected boolean isMemberOrCollaborator(String committerFullName, String gitHubOrganizationName = 'deeplearning4j') {
        Boolean isMember = false
        List gitHubUsers = []
        String authCredentialsId = 'github-username-and-token'
        String usersSearchUrl = "https://api.github.com/search/users?q=${committerFullName.replaceAll(' ', '+')}+in:fullname&type=Users"
        Boolean doCommitterUsernameCheck = true

        if (doCommitterUsernameCheck) {
            String userDetails = script.httpRequest(url: usersSearchUrl,
                    timeout: 60,
                    authentication: authCredentialsId,
                    quiet: true).content

            // WARNING: fetching first username from the search results may cause wrong recipient notifications on organization side.
            gitHubUsers = new JsonSlurper().parseText(userDetails).items.login
        }

        if (gitHubUsers) {
            isMember = gitHubUsers.find() { committerUsername ->
                String memberQueryUrl = "https://api.github.com/orgs/${gitHubOrganizationName}/members/${committerUsername}"

                int isMemberResponse = script.httpRequest(url: memberQueryUrl,
                        timeout: 60,
                        authentication: authCredentialsId,
                        quiet: true,
                        validResponseCodes: '100:404').status

                return (isMemberResponse == 204)
            }
        }

        return isMember
    }

    protected void withMavenCustom(body) {
        String configFileName = (branchName =~ /^master$|^latest_release$/) ?
                'global_mvn_settings_xml' : 'deeplearning4j-maven-global-settings'

        script.withMaven(
                /* Maven installation declared in the Jenkins "Global Tool Configuration" */
                /* -XX:+TieredCompilation -XX:TieredStopAtLevel=1 options should make JVM start a bit faster */
                mavenOpts: "-Djava.awt.headless=true -XX:+TieredCompilation -XX:TieredStopAtLevel=1",
                globalMavenSettingsConfig: configFileName,
                options: [
                        script.artifactsPublisher(disabled: true),
                        script.junitPublisher(disabled: true),
                        script.findbugsPublisher(disabled: true),
                        script.openTasksPublisher(disabled: true),
                        script.dependenciesFingerprintPublisher(disabled: true),
                        script.concordionPublisher(disabled: true),
                        script.invokerPublisher(disabled: true),
                        script.jgivenPublisher(disabled: true)
                ]
        ) {
            body()
        }
    }

    protected void getFancyStageDecorator(String text) {
        int charsNumber = Math.round((78 - text.length()) / 2)

        script.echo("*" * charsNumber + text + "*" * charsNumber)
    }

    protected String parseTestResults(testResults) {
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

    protected void runCheckout(String organization = 'deeplearning4j') {
        script.checkout script.scm

        checkoutDetails = parseCheckoutDetails()
        isMember = isMemberOrCollaborator(checkoutDetails.GIT_COMMITER_NAME, organization)

        script.notifier.sendSlackNotification jobResult: 'STARTED',
                checkoutDetails: checkoutDetails, isMember: isMember

        release = branchName ==~ releaseBranchPattern
    }
}
