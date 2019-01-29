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
            'mvn -U'
    ].findAll().join(' ')
    public Boolean release = false

    protected def getBuildMappings() {
        if (release) {
            return [
                    [
                            osType   : 'Linux',
                            platforms: [
                                    [name: 'linux-x86_64-generic', osName: 'centos', osVersion: '7', backend: 'cpu', sparkVersion: '1', scalaVersion: '2.10', hadoopVersion: 'hadoop-2.3', pythonVersion: '2'],
                                    [name: 'linux-x86_64-generic', osName: 'centos', osVersion: '7', backend: 'cuda-9.0', cudnnVersion: '7', sparkVersion: '1', scalaVersion: '2.10', hadoopVersion: 'hadoop-2.3', pythonVersion: '2'],
                                    [name: 'linux-x86_64-generic', osName: 'centos', osVersion: '7', backend: 'cuda-9.2', cudnnVersion: '7', sparkVersion: '1', scalaVersion: '2.10', hadoopVersion: 'hadoop-2.3', pythonVersion: '2'],
                                    [name: 'linux-x86_64-generic', osName: 'centos', osVersion: '7', backend: 'cuda-10.0', cudnnVersion: '7', sparkVersion: '1', scalaVersion: '2.10', hadoopVersion: 'hadoop-2.3', pythonVersion: '2'],
                                    [name: 'linux-x86_64-generic', osName: 'ubuntu', osVersion: '16.04', backend: 'cpu', sparkVersion: '1', scalaVersion: '2.10', hadoopVersion: 'hadoop-2.3', pythonVersion: '2'],
                                    [name: 'linux-x86_64-generic', osName: 'ubuntu', osVersion: '16.04', backend: 'cuda-9.0', cudnnVersion: '7', sparkVersion: '1', scalaVersion: '2.10', hadoopVersion: 'hadoop-2.3', pythonVersion: '2'],
                                    [name: 'linux-x86_64-generic', osName: 'ubuntu', osVersion: '16.04', backend: 'cuda-9.2', cudnnVersion: '7', sparkVersion: '1', scalaVersion: '2.10', hadoopVersion: 'hadoop-2.3', pythonVersion: '2'],
                                    [name: 'linux-x86_64-generic', osName: 'ubuntu', osVersion: '16.04', backend: 'cuda-10.0', cudnnVersion: '7', sparkVersion: '1', scalaVersion: '2.10', hadoopVersion: 'hadoop-2.3', pythonVersion: '2']
                            ]
                    ]
            ]
        } else {
            return [
                    [
                            osType   : 'Linux',
                            platforms: [
                                    [name: 'linux-x86_64-generic', osName: 'centos', osVersion: '7', backend: 'cpu', sparkVersion: '1', scalaVersion: '2.10', hadoopVersion: 'hadoop-2.3', pythonVersion: '2']
                            ]
                    ]
            ]
        }
    }


    void initPipeline() {
        try {
            script.node('linux-x86_64-generic') {
                script.stage('Prepare run') {
                    script.checkout script.scm

                    checkoutDetails = parseCheckoutDetails()
                    isMember = isMemberOrCollaborator(checkoutDetails.GIT_COMMITER_NAME, 'skymindio')

                    script.notifier.sendSlackNotification jobResult: 'STARTED',
                            checkoutDetails: checkoutDetails, isMember: isMember
                }
            }

            for (def m : buildMappings) {
                def mapping = m
                def osType = mapping.osType
                def platforms = mapping.platforms

                script.stage(osType) {
                    script.parallel getBuildStreams(platforms)
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

    private getBuildStreams(platforms) {
        Map streams = [failFast: false]

        for (def p : platforms) {
            def platform = p
            def platformName = platform.get('name')
            def osName = platform.get('osName')
            def osVersion = platform.get('osVersion')
            def backend = platform.get('backend')
            def pythonVersion = platform.containsKey('pythonVersion') ?
                    'python' + '-' + platform.get('pythonVersion') : ''
            def cudaVersion = backend?.contains('cuda') ? backend.split('-')[1] : ''
            def cudnnVersion = platform.get('cudnnVersion')
            def skilDockerBaseImageTag = (cudaVersion && cudnnVersion) ?
                    "nvidia/cuda:${cudaVersion}-cudnn${cudnnVersion}-devel-${osName}${osVersion}" :
                    "${osName}:${osVersion}"
            def skilDockerImageTag = (cudaVersion && cudnnVersion) ?
                    "cuda${cudaVersion}-cudnn${cudnnVersion}-devel-${osName}${osVersion}" :
                    "${osName}${osVersion}"
            def streamName = [
                    osName, osVersion, backend, pythonVersion
            ].findAll().join('-')
            def skilVersion

            /* Create stream body */
            streams["$streamName"] = {
                script.stage(streamName) {
                    script.node(platformName) {
                        script.container('jnlp') {
                            try {
                                script.stage('Checkout') {
                                    script.checkout script.scm

                                    def skilPom = script.readMavenPom(file: 'pom.xml')
                                    skilVersion = skilPom?.version

                                    // Expose gathered values as environment variables
                                    script.env.SKIL_VERSION = skilVersion
                                    script.env.SKIL_DOCKER_IMAGE_REVISION_NUMBER = "0.0.1"
                                }

                                withMavenCustom {
                                    script.stage('Build SKIL and its dependencies') {
                                        script.sh 'bash ./build-skil.sh'
                                    }
                                }

                                if (!release) {
                                    withMavenCustom {
                                        script.stage('Install test resources') {
                                            script.dir('skil-test-resources') {
                                                String installTestResourcesMavenArguments = [
                                                        mavenBaseCommand,
                                                        'clean',
                                                        'install',
                                                        '-DskipTests',
                                                        '-Dmaven.test.skip=true',
                                                        '-Dmaven.javadoc.skip=true'
                                                ].findAll().join(' ')

                                                script.mvn installTestResourcesMavenArguments
                                            }
                                        }

                                        script.stage('Run tests') {
                                            String runTestsMavenArguments = [
                                                    mavenBaseCommand,
                                                    'test',
                                                    '-P ci',
                                                    '-P ci-nexus',
                                            ].findAll().join(' ')

                                            script.mvn runTestsMavenArguments
                                        }
                                    }

                                    script.dir('skil-ui-modules/src/main/typescript/dashboard') {
                                        script.stage('Clear cache and build docker image from scratch') {
                                            script.sh '''\
                                                docker-compose rm -f
                                                docker-compose build
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

                                if (release) {
                                    script.stage('Generate artifacts') {
                                        def pythonPackageBuild = (script.env.PATHON_PACKAGE_BUILD) ?: false

                                        def generateSkilTarballMavenArguments = [
                                                mavenBaseCommand,
                                                'package',
                                                '-DskipTests=true',
                                                '-Dmaven.test.skip=true',
                                                '-Dmaven.javadoc.skip=true',
                                                '-Pbuilddistro',
                                                '-Pmodelserver',
                                                '-Pgenerate-tarball',
                                                (pythonPackageBuild) ? '-Ppython-rpm' : ''
                                        ].findAll().join(' ')

                                        script.mvn generateSkilTarballMavenArguments

                                        if (osName == 'centos') {
                                            def generateSkilRpmMavenArguments = [
                                                    mavenBaseCommand,
                                                    'package',
                                                    '-DskipTests=true',
                                                    '-Dmaven.test.skip=true',
                                                    '-Dmaven.javadoc.skip=true',
                                                    '-Pbuilddistro',
                                                    '-Pmodelserver',
                                                    '-Pgenerate-rpm',
                                                    '-Prpm',
                                                    (pythonPackageBuild) ? '-Ppython-rpm' : ''
                                            ].findAll().join(' ')

                                            script.mvn generateSkilRpmMavenArguments
                                        } else if (osName == 'ubuntu') {
                                            def generateSkilDebMavenArguments = [
                                                    mavenBaseCommand,
                                                    'package',
                                                    '-DskipTests=true',
                                                    '-Dmaven.test.skip=true',
                                                    '-Dmaven.javadoc.skip=true',
                                                    '-Pbuilddistro',
                                                    '-Pmodelserver',
                                                    '-Pgenerate-deb',
                                                    '-Pdeb',
                                                    (pythonPackageBuild) ? '-Ppython-rpm' : ''
                                            ].findAll().join(' ')

                                            script.mvn generateSkilDebMavenArguments
                                        }

                                        // Set required environment variables
                                        script.withEnv([
                                                "OS_NAME=${osName}",
                                                "SKIL_BASE_IMAGE_NAME=${skilDockerBaseImageTag}",
                                                "SKIL_DOCKER_IMAGE_TAG=${skilDockerImageTag}",
                                                "SKIL_DOCKER_IMAGE_REVISION=${script.env.SKIL_DOCKER_IMAGE_REVISION_NUMBER}-${script.env.GIT_COMMIT[0..7]}",
                                                "PYTHON_VERSION=${pythonVersion}"
                                        ]) {
                                            def findArtifactsScriptContent = script.libraryResource 'skymind/pipelines/skil/skil-distro-docker/find_artifacts.sh'
                                            script.writeFile file: 'find_artifacts.sh', text: findArtifactsScriptContent
                                            script.sh "bash ./find_artifacts.sh"

                                            script.dir('docker') {
                                                def scriptFilesList = ['build-skil.sh', 'install-build-packages.sh', 'install-skil-python.sh', 'start-skil.sh']

                                                script.dir("${osName}") {
                                                    script.dir('scripts') {
                                                        for (f in scriptFilesList) {
                                                            def fileName = f
                                                            def fileContent = script.libraryResource 'skymind/pipelines/skil/skil-distro-docker/docker' + '/' + osName + '/scripts/' + fileName

                                                            script.writeFile file: fileName, text: fileContent
                                                        }
                                                    }

                                                    def dockerFileContent = script.libraryResource 'skymind/pipelines/skil/skil-distro-docker/docker' + '/' + osName + '/' + 'Dockerfile'
                                                    script.writeFile file: 'Dockerfile', text: dockerFileContent
                                                }
                                            }

                                            def mainFilesList = ['docker-compose.yml', '.env']

                                            for (f in mainFilesList) {
                                                def fileName = f
                                                def fileContent = script.libraryResource 'skymind/pipelines/skil/skil-distro-docker/' + fileName
                                                script.writeFile file: fileName, text: fileContent
                                            }

                                            script.sh 'docker-compose config'

                                            script.sh """\
                                                docker-compose build skil-build
                                            """.stripIndent()

                                            script.sh 'docker images'
                                            script.sh 'docker inspect skil:${SKIL_VERSION}-${SKIL_DOCKER_IMAGE_TAG}'
                                        }
                                    }
                                }
                            }
                            finally {
                                def tr = script.junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'

                                testResults.add([
                                        testResults: parseTestResults(tr)
                                ])

                                script.archiveArtifacts allowEmptyArchive: true, artifacts: '**/hs_err_pid*.log'
//                                    script.archiveArtifacts artifacts: 'skil-distro-parent/skildistro/target/*-dist.tar.gz'
//                                    script.archiveArtifacts artifacts: 'skil-distro-parent/skil-distro-rpm/target/rpm/skil-server/RPMS/x86_64/*.rpm'

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

        streams
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
