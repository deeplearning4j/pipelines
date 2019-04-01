package skymind.pipelines.projects

import groovy.transform.InheritConstructors


@InheritConstructors
class SkilJavaProject extends Project {
    private String mavenBaseCommand = [
            'mvn -U',
            '-s ${MAVEN_SETTINGS}'
    ].findAll().join(' ')

    void initPipeline() {
        String platform = getPlatforms()[0].name

        script.node(platform) {
            try {
//                script.container('skil') {
//                    script.sh 'ls -la /etc/skil/license.txt'
//                    script.withCredentials([script.file(credentialsId: 'skil-unlim-test-license', variable: 'SKIL_LICENSE_PATH')]) {
//                        script.sh "cp \${SKIL_LICENSE_PATH} /etc/skil/license.txt"
//                    }
//                }

                script.container('builder') {
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

                        script.stage('Check SKIL') {
                            // FIXME: Wokraround for SKIL container readiness check
                            checkIfSkilIsRunning()
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

    private void checkIfSkilIsRunning() {
        def started = false
        def retries = 10

        for (int i = 0; i < retries; i++) {
            if (!started) {
                def checkResult = script.sh(
                        script: 'curl -sS http://localhost:9008/status | grep -o -i \'STARTED\' | wc -l',
                        returnStdout: true
                ).trim()

                if (checkResult == '3') {
                    started = true
                    script.echo 'SKIL has been started!'
                } else if (i == 9) {
                    script.error "SKIL has not been started!"
                } else {
                    sleep(60000)
                }
            } else {
                break
            }
        }
    }
}
