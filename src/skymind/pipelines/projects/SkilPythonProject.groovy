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

                            if (!branchName.contains(releaseBranchPattern)) {
                                script.stage('Test') {
                                    runTestLogic()
                                }
                            }
                        }
                        finally {
                            testResults.add([
                                    platform: streamName,
                                    testResults: ''
                            ])
                            script.cleanWs deleteDirs: true
                        }
                    }
                }
            }
        }

        streams
    }

    protected void runTestLogic() {
        script.sh """\
            pip install --user Cython --install-option=\"--no-cython-compile\"
            pip install --user -e .[tests]
            python -m pytest --pep8 -m pep8 tests/mock/
        """.stripIndent()
    }
}
