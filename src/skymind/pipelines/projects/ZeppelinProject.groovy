package skymind.pipelines.projects

import groovy.transform.InheritConstructors

@InheritConstructors
class ZeppelinProject extends Project {
    private String mavenBaseCommand = [
            'mvn -U -B -e',
            '-s ${MAVEN_SETTINGS}'
    ].findAll().join(' ')

    protected releaseBranchPattern = /^skymind-[\d.]+-skil-[\d.]+?[-]?[\w]+$/

    void initPipeline() {
        script.node(platforms[0].name) {
            pipelineWrapper {
                script.container('jnlp') {
                    try {
                        script.stage('Checkout') {
                            script.checkout script.scm

                            checkoutDetails = parseCheckoutDetails()
                            isMember = isMemberOrCollaborator(checkoutDetails.GIT_COMMITER_NAME, 'skymindio')

                            script.notifier.sendSlackNotification jobResult: 'STARTED',
                                    checkoutDetails: checkoutDetails, isMember: isMember

                            release = branchName ==~ releaseBranchPattern

                            script.dir('skil-server') {
                                script.git branch: 'develop/1.2.1',
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
//                                    'install',
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