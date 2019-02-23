package skymind.pipelines.projects

import groovy.transform.InheritConstructors


@InheritConstructors
class SkilJavaProject extends Project {
    public List testResults = []
    private Map checkoutDetails
    private Boolean isMember
    private String mavenBaseCommand = [
            'export MAVEN_OPTS="-XX:+UnlockExperimentalVMOptions ' +
                    '-XX:+UseCGroupMemoryLimitForHeap ${MAVEN_OPTS}" &&',
            'mvn -U'
    ].findAll().join(' ')

    protected List getPlatforms() {
        return [
                [name: 'linux-x86_64-skil-java']
        ]
    }

    void initPipeline() {
        String platform = getPlatforms()[0].name

        script.node(platform) {
            String wsFolderName = 'workspace' + '/' + [
                    projectName, branchName
            ].join('-').replaceAll('/', '-')

            script.ws(wsFolderName) {
                try {
                    script.container('jnlp') {
                        try {
                            script.stage('Checkout') {
                                script.checkout script.scm

                                checkoutDetails = parseCheckoutDetails()
                                isMember = isMemberOrCollaborator(checkoutDetails.GIT_COMMITER_NAME, 'skymindio')

                                script.notifier.sendSlackNotification jobResult: 'STARTED',
                                        checkoutDetails: checkoutDetails, isMember: isMember
                            }

                            if (branchName.contains(releaseBranchPattern)) {
                                script.stage("Perform Release") {
                                    getReleaseParameters()
                                }

                                script.stage("Prepare for Release") {
                                    setupEnvForRelease()
                                }
                            }

                            script.stage('Build and Test') {
                                String buildClientApiMavenArguments = [
                                        mavenBaseCommand,
                                        'clean',
                                        (branchName == 'master') ? 'deploy' : 'install'
                                ].findAll().join(' ')

                                script.mvn buildClientApiMavenArguments
                            }

                            if (branchName.contains(releaseBranchPattern)) {
                                script.stage('Release') {

                                }
                            }
                        }
                        finally {
                            def tr = script.junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'

                            testResults.add([
                                    testResults: parseTestResults(tr)
                            ])

                            script.archiveArtifacts allowEmptyArchive: true, artifacts: '**/hs_err_pid*.log'
                        }
                    }
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
                    script.cleanWs deleteDirs: true
                    // FIXME: Workaround to clean workspace
                    script.dir("${script.env.WORKSPACE}@tmp") {
                        script.deleteDir()
                    }
                    script.dir("${script.env.WORKSPACE}@script") {
                        script.deleteDir()
                    }
                    script.dir("${script.env.WORKSPACE}@script@tmp") {
                        script.deleteDir()
                    }

                    script.notifier.sendSlackNotification jobResult: script.currentBuild.result,
                            checkoutDetails: checkoutDetails, isMember: isMember, testResults: testResults
                }
            }
        }
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
}
