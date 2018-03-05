package skymind.pipelines.projects

import skymind.pipelines.helper.NotificationHelper

abstract class Project implements Serializable {
    protected script
    protected notifications
    protected platforms
    protected String projectVersion
    protected final String branchName
    protected final String projectName
    /* Default platforms for most of the projects */
    protected static List defaultPlatforms = [
            [backends: [], compillers: [], name: 'linux-x86_64']
    ]
    /* Default job properties */
    protected final List jobSpecificProperties = []
    protected static String gitterEndpointUrl = ''

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
        setBuildParameters(jobSpecificProperties)
        /* Terminate older builds */
        terminateOlderBuilds(this.script.env.JOB_NAME, this.script.env.BUILD_NUMBER.toInteger())
    }

    abstract void initPipeline()

    @NonCPS
    protected void setBuildParameters(List jobSpecificProperties) {
        List commonJobProperties = [
                script.buildDiscarder(
                        script.logRotator(
                                artifactDaysToKeepStr: '3',
                                artifactNumToKeepStr: '5',
                                daysToKeepStr: '3',
                                numToKeepStr: '5'
                        )
                ),
                [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false],
                /* Workaround to disable branch indexing */
                script.pipelineTriggers([])
//                script.parameters([
//                        script.choice(
//                                choices: ['nexus', 'sonatype', 'jfrog', 'bintray'].join('\n'),
//                                description: 'Maven profile names list',
//                                name: 'MAVEN_PROFILE_ACTIVATION_NAME'
//                        )
//                ])
        ]

        if (script.env.JOB_BASE_NAME == 'master') {
            commonJobProperties.push(
                    [
                            $class   : 'HudsonNotificationProperty',
                            endpoints: [
                                    /* Gitter endpoint url for dev_channel room */
                                    [
                                            retries: 5,
                                            urlInfo: [
                                                    urlOrId: 'https://webhooks.gitter.im/e/d74b592f0914132e59ba',
                                                    urlType: 'PUBLIC'
                                            ]
                                    ],
                                    /* If job specific Gitter endpoint url provided that add it to the list */
                                    (gitterEndpointUrl) ?
                                            [
                                                    retries: 5,
                                                    urlInfo: [
                                                            urlOrId: gitterEndpointUrl,
                                                            urlType: 'PUBLIC'
                                                    ]
                                            ] :
                                            null
                            ].findAll()
                    ]
            )
        }

        script.properties(commonJobProperties + jobSpecificProperties)
    }

    protected void pipelineWrapper(Closure pipelineBody) {
        try {
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

            script.cleanWs deleteDirs: true
        }
    }

    protected void allocateBuildNode(Closure stagesToRun) {
        for (Map pltm : platforms) {
            Map platform = pltm
            String platformName = platform.name
            script.node(platformName) {
                pipelineWrapper {
                    script.stage('Checkout') {
                        script.deleteDir()

                        script.dir(projectName) {
                            script.checkout script.scm
                        }
                    }

                    script.stage('Update project version') {
                        script.dir(projectName) {
                            projectVersion = projectObjectModel?.version
                            script.isVersionReleased(projectName, projectVersion)
                            script.setProjectVersion(projectVersion, true)
                        }
                    }

                    script.pipelineEnv.buildDisplayName.push(platformName)

//                    String createFoldersScript = "mkdir -p " +
//                            "${script.pipelineEnv.jenkinsDockerM2Folder}/" +
//                            "${script.pipelineEnv.mvnProfileActivationName} " +
//                            "${script.pipelineEnv.jenkinsDockerSbtFolder}"
//
//                    script.sh script: createFoldersScript

                    Map dockerConf = script.pipelineEnv.getDockerConfig(platformName)
                    String dockerImageName = dockerConf['image'] ?:
                            script.error('Docker image name is missing.')
                    String dockerImageParams = dockerConf?.'params'

                    stagesToRun(dockerImageName, dockerImageParams)
                }
            }
        }
    }

    protected String getMvnCommand(String stageName, List mvnArguments = []) {
        Boolean unixNode = script.isUnix()

        switch (stageName) {
            case 'build':
                if (unixNode) {
                    return [
                            'if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-4/enable ; fi ;',
                            /* Pipeline withMaven step requires this line if it runs in Docker container */
                            'export PATH=$MVN_CMD_DIR:$PATH &&',
                            'mvn -U',
                            'clean',
                            branchName == 'master' ? 'deploy' : 'install',
                            "-Dlocal.software.repository=${script.pipelineEnv.mvnProfileActivationName}",
                            '-Dmaven.test.skip=true'
                    ].plus(mvnArguments).findAll().join(' ')
                } else {
                    return [
                            'vcvars64.bat',
                            '&&',
                            'bash -c',
                            '"' + 'export PATH=$PATH:/c/msys64/mingw64/bin &&',
                            'mvn -U -B',
                            'clean',
                            branchName == 'master' ? 'deploy' : 'install',
                            "-Dlocal.software.repository=${script.pipelineEnv.mvnProfileActivationName}",
                            '-Dmaven.test.skip=true',
                            /* Workaround for Windows which doesn't honour withMaven options */
                            '-s ${MAVEN_SETTINGS}',
                            "-Dmaven.repo.local=" +
                                    "${script.env.WORKSPACE.replaceAll('\\\\', '/')}/" +
                                    "${script.pipelineEnv.localRepositoryPath}"
                    ].plus(mvnArguments).findAll().join(' ') + '"'
                }
                break
        /* TODO: Currently not in use */
            case 'test':
                if (unixNode) {
                    return [
                            'if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-4/enable ; fi ;',
                            /* Pipeline withMaven step requires this line if it runs in Docker container */
                            'export PATH=$MVN_CMD_DIR:$PATH &&',
                            'mvn -U',
                            'test',
                            "-Dlocal.software.repository=${script.pipelineEnv.mvnProfileActivationName}",
                    ].plus(mvnArguments).findAll().join(' ')
                } else {
                    return [
                            'vcvars64.bat',
                            '&&',
                            'bash -c',
                            '"' + 'export PATH=$PATH:/c/msys64/mingw64/bin &&',
                            'mvn -U -B',
                            'test',
                            "-Dlocal.software.repository=${script.pipelineEnv.mvnProfileActivationName}",
                            /* Workaround for Windows which doesn't honour withMaven options */
                            "-Dmaven.repo.local=${script.pipelineEnv.localRepositoryPath}",
                    ].plus(mvnArguments).findAll().join(' ') + '"'
                }
                break
            default:
                throw new IllegalArgumentException('Stage is not supported yet')
                break
        }
    }

    protected getProjectObjectModel() {
        String pomFileName = 'pom.xml'
        Boolean pomExists = script.fileExists(pomFileName)

        return (pomExists) ? script.readMavenPom() : script.error('pom.xml file not found')
    }

    @NonCPS
    protected terminateOlderBuilds(String jobName, int buildsNumber) {
        def currentJob = Jenkins.instance.getItemByFullName(jobName)

        for (def build : currentJob.builds) {
            if (build.isBuilding() && build.number.toInteger() != buildsNumber) {
                build.doStop()
                script.echo "[WARNING] Build number ${build.number} was terminated because of current(latest) run."
            }
        }
    }
}
