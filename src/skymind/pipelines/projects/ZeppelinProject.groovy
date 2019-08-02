package skymind.pipelines.projects

import groovy.transform.InheritConstructors

@InheritConstructors
class ZeppelinProject extends Project {
    private String mavenBaseCommand = [
            'mvn -U -B -e',
            '-s ${MAVEN_SETTINGS}'
    ].findAll().join(' ')

    protected static releaseBranchPattern = /^skymind-[\d.]+-skil-[\d.]+?[-]?[\w]+$/

    void initPipeline() {
        script.node(platforms[0].name) {
            pipelineWrapper {
                script.container('jnlp') {
                    try {
                        script.stage('Checkout') {
                            runCheckout('skymindio')
                        }

                        script.stage('Fetch skil-server sources') {
                            script.dir('skil-server') {
                                script.git branch: 'develop/1.3.0',
                                        changelog: false,
                                        poll: false,
                                        url: 'https://github.com/SkymindIO/skil-server.git',
                                        credentialsId: 'github-username-and-token'
                            }
                        }

                        script.stage('Build SKIL APIs') {
                            String buildSkilApisMavenArguments = [
                                    mavenBaseCommand,
                                    'clean',
                                    'install',
                                    '-DskipTests=true',
                                    '-Dmaven.test.skip=true',
                                    '-Dmaven.javadoc.skip=true'
                            ].findAll().join(' ')

                            script.dir('skil-server') {
                                script.sh 'bash -c \'./change-scala-versions.sh 2.10\''
                                script.sh 'bash -c \'./change-spark-major-versions.sh spark_1\''
                            }

                            script.dir('skil-server/skil-apis') {
                                script.mvn buildSkilApisMavenArguments
                            }
                        }

                        script.stage('Build/Deploy') {
                            String buildModelServerMavenArguments = [
                                    mavenBaseCommand,
                                    'clean',
                                    (release) ? 'deploy' : 'install',
                                    '-P build-distr',
                                    '-P scala-2.10',
                                    '-P spark-1.6',
                                    '-P hadoop-2.7',
                                    '-P yarn',
                                    '-P pyspark',
                                    '-P sparkr',
                                    '-DskipTests',
                                    '-Dmaven.test.skip=true'
                            ].findAll().join(' ')

                            script.sh 'bash -c \'./dev/change_scala_version.sh 2.10\''

                            script.mvn buildModelServerMavenArguments
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