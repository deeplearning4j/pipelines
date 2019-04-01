package skymind.pipelines.projects

import groovy.transform.InheritConstructors

@InheritConstructors
class Deeplearning4jTestResourcesProject extends Project {
    private String mavenBaseCommand = [
            "source /opt/rh/devtoolset-6/enable &&",
            'mvn -U -B -e',
            '-s ${MAVEN_SETTINGS}'
    ].findAll().join(' ')

    void initPipeline() {
        script.node(platforms[0].name) {
            pipelineWrapper {
                script.container('builder') {
                    try {
                        script.dir(projectName) {
                            if (branchName.contains(releaseBranchPattern)) {
                                script.stage("Perform Release") {
                                    getReleaseParameters()
                                }

                                script.stage("Prepare for Release") {
                                    setupEnvForRelease()
                                }
                            }

                            script.stage('Checkout') {
                                script.checkout script.scm
                            }

                            script.stage("Build/Deploy") {
                                String mavenCommand = [
                                        mavenBaseCommand,
                                        'clean',
                                        (release) ? 'deploy' : 'install',
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
