package skymind.pipelines.projects

import groovy.transform.InheritConstructors

@InheritConstructors
class SkilPythonProject extends Project {
    protected static releaseBranchPattern = /^master$/

    void initPipeline() {
        pipelineWrapper {
            script.parallel getBuildStreams(platforms)
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
                    script.container('builder') {
                        try {
                            script.stage('Checkout') {
                                runCheckout('skymindio')
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

    private void runTestLogic() {
        script.sh 'python -m pytest --junitxml test_reports/unit_test_results.xml --pep8 -m pep8 tests/mock/'
    }

    private void runIntegrationTests() {
        script.sh 'python -m pytest --junitxml test_reports/integration_test_results.xml --pep8 -m pep8 tests/integration/'
    }
}
