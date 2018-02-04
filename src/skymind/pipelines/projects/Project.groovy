package skymind.pipelines.projects

import skymind.pipelines.helper.NotificationHelper

abstract class Project implements Serializable {
    protected script
    protected notifications
    protected platforms
    protected String projectVersion
    protected final String branchName
    protected final String projectName
    protected final List scalaVersions = ['2.10', '2.11']
    /* Default platforms for most of the projects */
    protected static List defaultPlatforms = [
            [backends: [], compillers: [], name: 'linux-x86_64']
    ]

    /**
     * Project class constructor
     *
     * @param pipeline script object
     * @param projectName project name
     * @param jobConfig configuration of job/run environment
     */
    Project(script, String projectName, Map jobConfig) {
        this.script = script
        this.projectName = projectName
        branchName = this.script.env.BRANCH_NAME
        /* Default platforms will be used if developer didn't redefine them in Jenkins file */
        platforms = jobConfig?.getAt('platforms') ?: defaultPlatforms
        /* Get instance of NotificationHelper class for sending notifications about run status */
        notifications = new NotificationHelper(script)
        /* Configure job build parameters */
        setBuildParameters()
    }

    abstract void initPipeline()

    @NonCPS
    protected void setBuildParameters(List buildParameters = []) {
        script.properties([
                script.buildDiscarder(
                        script.logRotator(
                                artifactDaysToKeepStr: '3',
                                artifactNumToKeepStr: '5',
                                daysToKeepStr: '3',
                                numToKeepStr: '10'
                        )
                ),
                [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false]
//                [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false],
//                script.parameters([
                //                        script.choice(
                //                                choices: ['nexus', 'sonatype', 'jfrog', 'bintray'].join('\n'),
                //                                description: 'Maven profile names list',
                //                                name: 'MAVEN_PROFILE_ACTIVATION_NAME'
                //                        )
//                ].plus(buildParameters))
        ].plus(buildParameters))
    }

    protected void pipelineWrapper(Closure pipelineBody) {
        try {
            script.stage('Checkout') {
                script.milestone()
                checkoutScm(projectName)
            }

            pipelineBody()
        }
        catch (error) {
            script.echo "[ERROR] ${error}"
            script.currentBuild.result = script.currentBuild.result ?: 'FAILURE'
        }
        finally {
            script.currentBuild.displayName = "#${this.script.currentBuild.number} " +
                    script.pipelineEnv.buildDisplayName?.findAll()?.join(' | ')

            notifications.sendEmail(script.currentBuild.currentResult)

            script.cleanWs cleanWhenFailure: false, deleteDirs: true
        }
    }

    protected void allocateBuildNode(Closure stagesToRun) {
        for (Map pltm : platforms) {
            Map platform = pltm
            String platformName = platform.name
            script.node(platformName) {
                pipelineWrapper {
                    script.pipelineEnv.buildDisplayName.push(platformName)

                    String createFoldersScript = "mkdir -p " +
                            "${script.pipelineEnv.jenkinsDockerM2Folder}/" +
                            "${script.pipelineEnv.mvnProfileActivationName} " +
                            "${script.pipelineEnv.jenkinsDockerSbtFolder}"

                    script.sh script: createFoldersScript

                    Map dockerConf = script.pipelineEnv.getDockerConfig(platformName)
                    String dockerImageName = dockerConf?.getAt('image') ?:
                            script.error("Docker image name is missing.")
                    String dockerImageParams = dockerConf?.getAt('params') ?:
                            script.error("Docker container parameters are missing.")

                    stagesToRun(dockerImageName, dockerImageParams)
                }
            }
        }
    }

    protected void checkoutScm(String project) {
        if (project in ['libnd4j', 'nd4j']) {
            script.checkout script.scm
            script.stash name: 'sourceCode', useDefaultExcludes: false
        } else {
            script.dir(projectName) {
                script.checkout script.scm
            }
        }
    }

    protected String getMvnCommand(String stageName, List mvnArguments = []) {
        Boolean unixNode = script.isUnix()

        switch (stageName) {
            case 'build':
                if (unixNode) {
                    return [
                            'if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi ;',
                            /* Pipeline withMaven step requires this line if it runs in Docker container */
                            'export PATH=$MVN_CMD_DIR:$PATH &&',
                            'mvn -U',
                            '-P trimSnapshots',
                            'clean',
                            branchName == 'master' ? 'deploy' : 'install',
                            "-Dlocal.software.repository=${script.pipelineEnv.mvnProfileActivationName}",
                            '-Dmaven.repo.local=${HOME}/.m2/' + "${script.pipelineEnv.mvnProfileActivationName}" +
                                    '/repository',
                            '-Dmaven.test.skip=true'
                    ].plus(mvnArguments).findAll().join(' ')
                } else {
                    return [
                            'vcvars64.bat',
                            '&&',
                            'bash -c',
                            '"' + 'export PATH=$PATH:/c/msys64/mingw64/bin &&',
                            'mvn -U -B',
                            "-s %MAVEN_SETTINGS%", // Workaround for windows paths in bash
                            '-P trimSnapshots',
                            'clean',
                            branchName == 'master' ? 'deploy' : 'install',
                            "-Dlocal.software.repository=${script.pipelineEnv.mvnProfileActivationName}",
                            '-Dmaven.repo.local=%TEMP%\\.m2\\' + "${script.pipelineEnv.mvnProfileActivationName}" +
                                    '\\repository',
                            '-Dmaven.test.skip=true'
                    ].plus(mvnArguments).findAll().join(' ') + '"'
                }
                break
            /* TODO: Currently not in use */
            case 'test':
                if (unixNode) {
                    return [
                            'if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi ;',
                            /* Pipeline withMaven step requires this line if it runs in Docker container */
                            'export PATH=$MVN_CMD_DIR:$PATH &&',
                            'mvn -U',
                            '-P trimSnapshots',
                            'test',
                            '-Dmaven.repo.local=${HOME}/.m2/' + "${script.pipelineEnv.mvnProfileActivationName}" +
                                    '/repository',
                            "-Dlocal.software.repository=${script.pipelineEnv.mvnProfileActivationName}",
                    ].plus(mvnArguments).findAll().join(' ')
                } else {
                    return [
                            'vcvars64.bat',
                            '&&',
                            'bash -c',
                            '"' + 'export PATH=$PATH:/c/msys64/mingw64/bin &&',
                            'mvn -U',
                            '-P trimSnapshots',
                            'test',
                            '-Dmaven.repo.local=%TEMP%\\.m2\\' + "${script.pipelineEnv.mvnProfileActivationName}" +
                                    '\\repository',
                            "-Dlocal.software.repository=${script.pipelineEnv.mvnProfileActivationName}",
                            '-Dmaven.test.skip=true'
                    ].plus(mvnArguments).findAll().join(' ') + '"'
                }
                break
            default:
                throw new IllegalArgumentException('Stage is not supported yet')
                break
        }
    }

    protected Map getProjectObjectModel() {
        String pomFileName = 'pom.xml'
        Boolean pomExists = script.fileExists(pomFileName)

        return (pomExists) ? script.readMavenPom() : script.error('pom.xml file not found')
    }
}
