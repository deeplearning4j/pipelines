package skymind.pipelines.projects

import groovy.transform.InheritConstructors

@InheritConstructors
class SkilServerProject extends Project {
    public List testResults = []
    private Map checkoutDetails
    private Boolean isMember
    private String mavenBaseCommand = [
            'export MAVEN_OPTS="-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap ${MAVEN_OPTS}" &&',
            'mvn'
    ].findAll().join(' ')

    protected List getPlatforms() {
        return [
                [name: 'linux-x86_64-generic']
        ]
    }

    void initPipeline() {
        try {
            String platform = getPlatforms()[0].name

            script.node(platform) {
                try {
                    script.stage('Checkout') {
                        script.checkout script.scm
                        checkoutDetails = parseCheckoutDetails()
                        isMember = isMemberOrCollaborator(checkoutDetails.GIT_COMMITER_NAME)
                    }

                    script.stage('Install test resources') {
                        String installTestResourcesMavenArguments = [
                                mavenBaseCommand,
                                'clean',
                                'install',
                                '-pl skil-test-resources'
                        ].findAll().join(' ')

                        script.mvn installTestResourcesMavenArguments
                    }

                    script.stage('Build client APIs') {
                        script.dir('skil-apis') {
                            String buildClientApiMavenArguments = [
                                    mavenBaseCommand,
                                    'clean',
                                    'install'
                            ].findAll().join(' ')

                            script.mvn buildClientApiMavenArguments
                        }
                    }

                    script.stage('Build ModelServer') {
                        script.dir('modelserver') {
                            String buildModelServerMavenArguments = [
                                    mavenBaseCommand,
                                    'clean',
                                    'install',
                                    '-P native',
                                    '-P tf-cpu',
                                    '-DskipTests'
                            ].findAll().join(' ')

                            script.mvn buildModelServerMavenArguments
                        }
                    }

                    script.stage('Build SKIL') {
                        script.dir('modelserver') {
                            String buildSkilMavenArguments = [
                                    mavenBaseCommand,
                                    'clean',
                                    'install'
                            ].findAll().join(' ')

                            script.mvn buildSkilMavenArguments
                        }
                    }

                    script.stage('Run tests') {
                        script.dir('modelserver') {
                            String runTestsMavenArguments = [
                                    mavenBaseCommand,
                                    '-fae test',
                            ].findAll().join(' ')

                            script.mvn runTestsMavenArguments
                        }
                    }
                }
                finally {
                    testResults.add([
                            platform   : platform,
                            testResults: ''
                    ])

                    script.archiveArtifacts allowEmptyArchive: true, artifacts: '**/hs_err_pid*.log'
                    script.archiveArtifacts artifacts: 'skil-distro-parent/skildistro/target/*-dist.tar.gz'
                    script.archiveArtifacts artifacts: 'skil-distro-parent/skil-distro-rpm/target/rpm/skil-server/RPMS/x86_64/*.rpm'

                    script.cleanWs deleteDirs: true
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
