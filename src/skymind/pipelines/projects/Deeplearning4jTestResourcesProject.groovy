package skymind.pipelines.projects

import groovy.transform.InheritConstructors

@InheritConstructors
class Deeplearning4jTestResourcesProject extends Project {
    private String mavenBaseCommand = [
            "source /opt/rh/devtoolset-6/enable &&",
            'mvn -U -B -e',
            '-s ${MAVEN_SETTINGS}'
    ].findAll().join(' ')

//    protected static releaseBranchPattern = /^master$/

    void initPipeline() {
        script.node(platforms[0].name) {
            pipelineWrapper {
                script.container('builder') {
                    try {
                        script.dir(projectName) {
                            script.stage('Checkout') {
                                runCheckout()
                            }

                            if (release) {
                                script.stage("Perform Release") {
                                    getReleaseParameters()
                                }

                                script.stage("Prepare for Release") {
                                    setupEnvForRelease()
                                }
                            }

                            script.stage("Build and Deploy") {
                                String mavenCommand = [
                                        mavenBaseCommand,
                                        'clean',
                                        (release) ? 'deploy' : 'package',
                                        '-Dlocal.software.repository=ci-nexus',
                                        '-Dresources.jar.compression=true'
                                ].findAll().join(' ')

                                script.mvn mavenCommand
                            }
                        }
                    }
                    finally {
                        def tr = script.junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'

                        testResults.add([
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
}
