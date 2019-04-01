package skymind.pipelines.projects

import groovy.transform.InheritConstructors


@InheritConstructors
class SkilJavaProject extends Project {
    private String mavenBaseCommand = [
            'mvn -U',
            '-s ${MAVEN_SETTINGS}'
    ].findAll().join(' ')

    void initPipeline() {
        script.node(platforms[0].name) {
            pipelineWrapper {
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
        }
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
