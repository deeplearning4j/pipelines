package skymind.pipelines.projects

import groovy.transform.InheritConstructors

@InheritConstructors
class SkilClientsProject extends Project {
    private String mavenBaseCommand = [
            'mvn -U',
            '-s ${MAVEN_SETTINGS}'
    ].findAll().join(' ')

    protected static releaseBranchPattern = /^master$/

    void initPipeline() {
        script.node(platforms[0].name) {
            String wsFolderName = 'workspace' + '/' + [
                    projectName, branchName
            ].join('-').replaceAll('/', '-')

            script.ws(wsFolderName) {
                pipelineWrapper {
                    script.container('builder') {
                        try {
                            script.stage('Checkout') {
                                runCheckout('skymindio')
                            }

                            if (release) {
                                script.stage("Perform Release") {
                                    getReleaseParameters()
                                }

                                script.stage("Prepare for Release") {
                                    setupEnvForRelease()
                                }
                            }

                            script.stage('Build and Test') {
                                script.sh "cp config/Packaging/java/pom.xml java/pom.xml"

                                script.dir('java') {
                                    String buildClientApiMavenArguments = [
                                            mavenBaseCommand,
                                            'clean',
                                            (branchName == 'master') ? 'deploy' : 'install'
                                    ].findAll().join(' ')

                                    script.mvn buildClientApiMavenArguments
                                }
                            }

                            if (release) {
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
    }
}
