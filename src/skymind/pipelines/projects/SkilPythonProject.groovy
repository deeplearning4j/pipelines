package skymind.pipelines.projects

import groovy.transform.InheritConstructors

@InheritConstructors
class SkilPythonProject extends Project {
    public List testResults = []
    private Map checkoutDetails
    private Boolean isMember

    protected List getPlatforms() {
        return [
                [name: 'linux-x86_64', pythonVersion: '2'],
                [name: 'linux-x86_64', pythonVersion: '3']
        ]
    }

    void initPipeline() {
        try {
            if (branchName.contains(releaseBranchPattern)) {
                script.stage("Perform Release") {
                    getReleaseParameters()
                }
            }

            script.parallel getBuildStreams(platforms)
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

    private Map getBuildStreams(List platforms) {
        Map streams = [failFast: false]

        for (Map pltm : platforms) {
            Map platform = pltm
            String platformName = platform.get('name')
            String pythonVersion = platform.get('pythonVersion')
            String streamName = [platformName, 'python', pythonVersion].findAll().join('-')

            /* Create stream body */
            streams["$streamName"] = {
                script.node(streamName) {
                    Boolean isUnixNode = script.isUnix()
                    String separator = isUnixNode ? '/' : '\\'
                    /* Workaround for Windows path length limitation */
                    String wsFolderName = ((isUnixNode) ? 'workspace' : 'ws') +
                            separator +
                            [(isUnixNode) ?
                                     projectName :
                                     (projectName.contains('deeplearning4j') ? 'dl4j' : projectName),
                             script.env.BRANCH_NAME,
                             streamName].join('-').replaceAll('/', '-')

                    /* Redefine default workspace to fix Windows path length limitation */
                    script.ws(wsFolderName) {
                        try {
                            script.stage('Checkout') {
                                script.checkout script.scm
                                checkoutDetails = parseCheckoutDetails()
                                isMember = isMemberOrCollaborator(checkoutDetails.GIT_COMMITER_NAME)
                            }

                            script.stage('Install required dependencies') {
                                script.sh """\
                                    pip install --user Cython --install-option=\"--no-cython-compile\"
                                    pip install --user -e .[tests]
                                """.stripIndent()
                            }

                            script.stage('Test') {
                                runTestLogic()
                            }

                            script.stage('Run integration tests') {
                                runIntegrationTests()
                            }
                        }
                        finally {
                            def tr = script.junit allowEmptyResults: true, testResults: '**/test_reports/*.xml'

                            testResults.add([
                                    platform   : streamName,
                                    testResults: parseTestResults(tr)
                            ])
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
                        }
                    }
                }
            }
        }

        streams
    }

    protected void runTestLogic() {
        script.sh 'python -m pytest --junitxml test_reports/unit_test_results.xml --pep8 -m pep8 tests/mock/'
    }

    protected void runIntegrationTests() {
        script.sh 'python -m pytest --junitxml test_reports/integration_test_results.xml --pep8 -m pep8 tests/integration/'
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
