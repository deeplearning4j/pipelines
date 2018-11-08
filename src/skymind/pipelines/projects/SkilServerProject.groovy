package skymind.pipelines.projects

import groovy.transform.InheritConstructors

@InheritConstructors
class SkilServerProject extends Project {
    public List testResults = []
    private Map checkoutDetails
    private Boolean isMember
    private String mavenBaseCommand = [
            'export MAVEN_OPTS="-XX:+UnlockExperimentalVMOptions ' +
                    '-XX:+UseCGroupMemoryLimitForHeap ${MAVEN_OPTS}" &&',
            'mvn'
    ].findAll().join(' ')

    protected List getPlatforms() {
        return [
                [name: 'linux-x86_64-generic']
        ]
    }

    void initPipeline() {
        String platform = getPlatforms()[0].name

        script.node(platform) {
            String wsFolderName = 'workspace' + '/' + [
                    projectName, script.env.BRANCH_NAME
            ].join('-').replaceAll('/', '-')

            script.ws(wsFolderName) {
                try {
                    script.container('jnlp') {
                        try {
                            script.stage('Checkout') {
                                script.checkout script.scm

                                checkoutDetails = parseCheckoutDetails()
                                isMember = isMemberOrCollaborator(checkoutDetails.GIT_COMMITER_NAME, 'skymindio')

                                script.notifier.sendSlackNotification jobResult: 'STARTED',
                                        checkoutDetails: checkoutDetails, isMember: isMember
                            }

                            script.stage('Build client APIs') {
                                script.dir('skil-apis') {
                                    String buildClientApiMavenArguments = [
                                            mavenBaseCommand,
                                            'clean',
                                            'install',
                                            '-DskipTests',
                                            '-Dmaven.test.skip=true',
                                            '-Dmaven.javadoc.skip=true'
                                    ].findAll().join(' ')

                                    script.mvn buildClientApiMavenArguments
                                }
                            }

                            script.stage('Build SKIL auth') {
                                String buildSkilAuthMavenArguments = [
                                        mavenBaseCommand,
                                        'clean',
                                        'install',
                                        '-pl :skil-auth',
                                        '--also-make',
                                        '-DskipTests',
                                        '-Dmaven.test.skip=true',
                                        '-Dmaven.javadoc.skip=true'
                                ].findAll().join(' ')

                                script.mvn buildSkilAuthMavenArguments
                            }

                            script.stage('Build ModelServer') {
                                script.dir('modelserver') {
                                    String buildModelServerMavenArguments = [
                                            mavenBaseCommand,
                                            'clean',
                                            'install',
                                            '-P native',
                                            '-P tf-cpu',
                                            '-DskipTests',
                                            '-Dmaven.test.skip=true'
                                    ].findAll().join(' ')

                                    script.mvn buildModelServerMavenArguments
                                }
                            }

                            script.stage('Install test resources') {
                                script.dir('skil-test-resources') {
                                    String installTestResourcesMavenArguments = [
                                            mavenBaseCommand,
                                            'clean',
                                            'install',
                                            '-DskipTests',
                                            '-Dmaven.test.skip=true',
                                            '-Dmaven.javadoc.skip=true',

                                    ].findAll().join(' ')

                                    script.mvn installTestResourcesMavenArguments
                                }
                            }

                            script.stage('Build SKIL') {
                                String buildSkilMavenArguments = [
                                        mavenBaseCommand,
                                        'clean',
                                        'install',
                                        '-DskipTests=true',
                                        '-Dmaven.test.skip=true',
                                        '-Dmaven.javadoc.skip=true'
                                ].findAll().join(' ')

                                script.mvn buildSkilMavenArguments
                            }

                            script.stage('Run tests') {
                                String runTestsMavenArguments = [
                                        mavenBaseCommand,
                                        'test',
                                        '-P ci',
                                        '-P ci-nexus'
                                ].findAll().join(' ')

                                script.mvn runTestsMavenArguments
                            }

                            script.stage('Generate artifacts') {
                                String generateSkilTarballMavenArguments = [
                                        mavenBaseCommand,
                                        'package',
                                        '-Pgenerate-tarball',
                                        '-DskipTests=true',
                                        '-Dmaven.test.skip=true',
                                        '-Dmaven.javadoc.skip=true'
                                ].findAll().join(' ')

                                script.mvn generateSkilTarballMavenArguments

//                                String generateSkilRpmMavenArguments = [
//                                        mavenBaseCommand,
//                                        'package',
//                                        '-Pgenerate-rpm',
//                                        '-DskipTests=true',
//                                        '-Dmaven.test.skip=true',
//                                        '-Dmaven.javadoc.skip=true',
//                                ].findAll().join(' ')
//
//                                script.mvn generateSkilRpmMavenArguments
                            }
                        }
                        finally {
                            def tr = script.junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'

                            testResults.add([
                                    testResults: parseTestResults(tr)
                            ])

                            script.archiveArtifacts allowEmptyArchive: true, artifacts: '**/hs_err_pid*.log'
                            script.archiveArtifacts artifacts: 'skil-distro-parent/skildistro/target/*-dist.tar.gz'
//                            script.archiveArtifacts artifacts: 'skil-distro-parent/skil-distro-rpm/target/rpm/skil-server/RPMS/x86_64/*.rpm'
                        }
                    }

                    script.container('docker') {
                        script.dir('skil-ui-modules/src/main/typescript/dashboard') {
                            script.stage('Clear cache and build docker image from scratch') {
                                script.sh '''\
                                    docker-compose rm -f
                                    docker-compose build
                                    # docker-compose build --no-cache --pull
                                '''.stripIndent()
                            }

                            script.stage('SKIL Dashboard Unit Tests') {
                                script.sh '''\
                                    docker-compose run --rm dev yarn run test-teamcity
                                '''.stripIndent()
                            }

                            script.stage('SKIL Dashboard E2E Tests') {
                                script.sh '''\
                                    docker-compose run --rm dev yarn run e2e-teamcity
                                '''.stripIndent()
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
                    script.cleanWs deleteDirs: true

                    script.notifier.sendSlackNotification jobResult: script.currentBuild.result,
                            checkoutDetails: checkoutDetails, isMember: isMember, testResults: testResults
                }
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