package skymind.pipelines.projects

import groovy.json.JsonSlurper
import skymind.pipelines.modules.Module

/**
 * Step 1: Get changed modules, by searching pom.xml file in the path of changed file.
 * Step 2: Prepare build environment (pick right Jenkins slave to build on).
 * Step 3: Generate reactor with the help of profiles.
 * Step 4: Pick projects to build and excludes platform dependent modules with the help of -pl maven option.
 */
class Deeplearning4jProject implements Serializable {
    private script
    private final String projectName
    private final String branchName
    private final Map jobConfig
    private Boolean releaseApproved = false
    private static String releaseBranchPattern = 'release'
    private static String releaseVersion
    private static String snapshotVersion
    private List testResults = []

    /**
     * Project class constructor
     *
     * @param script
     * @param jobConfig configuration of job/run environment
     */
    Deeplearning4jProject(Object script, Map jobConfig) {
        this.script = script
        this.jobConfig = jobConfig ?: [:]
        branchName = this.script.env.BRANCH_NAME
        projectName = (this.script.env.JOB_NAME - this.script.env.JOB_BASE_NAME).
                tokenize('/').
                last().
                trim()
        /* Configure job parameters */
        setBuildParameters()
        /* Terminate older builds */
        terminateOlderBuilds(this.script.env.JOB_NAME, this.script.env.BUILD_NUMBER.toInteger())
    }

    protected Map parseCheckoutDetails() {
        Closure shellCommand = { String command ->
            return script.sh(script: command, returnStdout: true).trim()
        }

        String gitCommitId = shellCommand('git log -1 --pretty=%H')

        return [GIT_BRANCH        : branchName,
                GIT_COMMIT        : gitCommitId,
                GIT_COMMITER_NAME : shellCommand("git --no-pager show -s --format='%an' ${gitCommitId}"),
                GIT_COMMITER_EMAIL: shellCommand("git --no-pager show -s --format='%ae' ${gitCommitId}"),
                // Change pretty format from -pretty=%B to --pretty=format:'%s%n%n%b' because of old version of git (1.7.1)
                GIT_COMMIT_MESSAGE: shellCommand("git log -1 --pretty=format:'%s%n%n%b' ${gitCommitId}")]
    }

    protected Boolean isMemberOrCollaborator(String committerFullName, String gitHubOrganizationName = 'deeplearning4j') {
        Boolean isMember = false
        List gitHubUsers = []
        String authCredentialsId = 'github-username-and-token'
        String usersSearchUrl = "https://api.github.com/search/users?q=${committerFullName.replaceAll(' ', '+')}+in:fullname&type=Users"
        Boolean doCommitterUsernameCheck = true

        if (doCommitterUsernameCheck) {
            String userDetails = script.httpRequest(url: usersSearchUrl,
                    timeout: 60,
                    authentication: authCredentialsId,
                    quiet: true).content

            // WARNING: fetching first username from the search results may cause wrong recipient notifications on organization side.
            gitHubUsers = new JsonSlurper().parseText(userDetails).items.login
        }

        if (gitHubUsers) {
            isMember = gitHubUsers.find() { committerUsername ->
                String memberQueryUrl = "https://api.github.com/orgs/${gitHubOrganizationName}/members/${committerUsername}"

                int isMemberResponse = script.httpRequest(url: memberQueryUrl,
                        timeout: 60,
                        authentication: authCredentialsId,
                        quiet: true,
                        validResponseCodes: '100:404').status

                return (isMemberResponse == 204)
            }
        }

        return isMember
    }

    protected void initPipeline() {
        List modulesToBuild
        Map checkoutDetails
        Boolean isMember

        try {
            script.stage('Prepare Run') {

                script.node('linux-x86_64-generic') {
                    script.container('jnlp') {
                        script.checkout script.scm

                        checkoutDetails = parseCheckoutDetails()

                        isMember = isMemberOrCollaborator(checkoutDetails.GIT_COMMITER_NAME)

                        script.notifier.sendSlackNotification jobResult: 'STARTED',
                                checkoutDetails: checkoutDetails, isMember: isMember

                        modulesToBuild = getModulesToBuild(changes)

                        script.echo "[INFO] Changed modules: ${modulesToBuild}"
                    }
                }
            }

            if (branchName.contains(releaseBranchPattern)) {
                script.stage("Perform Release") {
                    getReleaseParameters()
                }
            }

            List mappings = filterModulesToBuild(modulesToBuild)

            for (map in mappings) {
                Map mapping = map
                List modules = mapping.modules ?: script.error('Missing modules!')
                List platforms = mapping.platforms ?: script.error('Missing platforms!')

                script.parallel getBuildStreams(modules, platforms)
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

//            script.notifier.sendEmailNotification(script.currentBuild.result)
        }
    }

    private List getChanges() {
        List changedFiles = []

        for (chSet in script.currentBuild.changeSets) {
            def changeSet = chSet

            for (fl in changeSet.items) {
                def files = fl

                for (affectedFile in files.affectedFiles) {
                    changedFiles.push(affectedFile.path)
                }
            }
        }

        changedFiles
    }

//    private List getChanges() {
//        Boolean unixNode = script.isUnix()
//        String shell = unixNode ? 'sh' : 'bat'
//
//        String shellCommand = 'git --no-pager diff --name-only HEAD $(git merge-base HEAD origin/master)'
//
//        if (branchName.contains('PR')) {
//            return script."$shell"(script: "${shellCommand}", returnStdout: true).trim().tokenize('\n')
//        } else {
//            return []
//        }
//    }

    private List getModulesToBuild(List changedFiles) {
        List supportedModules = [
                'libnd4j', 'nd4j', 'datavec', 'deeplearning4j', 'arbiter',
                'nd4s',
                'gym-java-client', 'rl4j', 'scalnet', 'jumpy', 'pydatavec',
                'pydl4j', 'docs'
        ]
        List changesRelatedToModules = []
        List changesNotRelatedToModules = []

        for (fl in changedFiles) {
            String file = fl
            String moduleName = getModuleName(file)
            String baseModuleName = moduleName.tokenize('/')[0]

            if (baseModuleName in supportedModules) {
                changesRelatedToModules.push(moduleName)
            } else {
                changesNotRelatedToModules.push(moduleName)
            }

        }

        (changesNotRelatedToModules || !changesRelatedToModules) ?
                supportedModules :
                changesRelatedToModules.unique()
    }


    private String getModuleName(String filePath) {
        String moduleName
        String scriptName = 'get_module_name.sh'
        String getModuleNameScript = script.libraryResource 'skymind/pipelines/scripts/' +
                scriptName

        script.writeFile file: scriptName, text: getModuleNameScript

        if (script.isUnix()) {
            moduleName = script.sh(script: "bash ${scriptName} ${filePath}", returnStdout: true).
                    trim()
        } else {
            moduleName = script.bat(
                    script: "\"C:\\Program Files\\Git\\bin\\bash.exe\" -c " +
                            "'${scriptName} ${filePath}'",
                    returnStdout: true
            ).trim()
        }

        moduleName
    }

    private List filterModulesToBuild(List modulesToBuild) {
        Map mappings = [
                multi    : [modules: [], platforms: getPlatforms('libnd4j')],
                gpu      : [modules: [], platforms: getPlatforms('deeplearning4j')],
                pymodules: [modules: [], platforms: getPlatforms('jumpy')],
                docs:      [modules: [], platforms: getPlatforms('docs')],
                generic  : [modules: [], platforms: getPlatforms()]
        ]

        for (mod in modulesToBuild) {
            String module = mod

            if (module =~ /^libnd4j|^nd4j/) {
                mappings.multi.modules.push(module)
            } else if (module =~ /^deeplearning4j|^datavec/) {
                mappings.gpu.modules.push(module)
            } else if (module =~ /^pydatavec|^jumpy|^pydl4j/) {
                mappings.pymodules.modules.push(module)
            } else if (module =~ /^docs/) {
                mappings.docs.modules.push(module)
            } else {
                mappings.generic.modules.push(module)
            }
        }

        /* Strip mappings with empty modules list */
        Map result = mappings.collectEntries { key, value ->
            value.modules ?
                    [(key): [modules: value.modules.unique(), platforms: value.platforms]] :
                    [:]
        }

        if (result.containsKey('gpu') && result.containsKey('generic')) {
            result.gpu.modules += result.generic.modules
            result.remove('generic')
        }

        if (result.containsKey('multi') && result.containsKey('gpu')) {
            result.multi.modules += result.gpu.modules
            result.remove('gpu')
        }

        result.collect { key, value ->
            value
        }
    }

    private Map getBuildStreams(List modulesToBuild, List platforms) {
        Map streams = [failFast: false]
        List buildInContainer = [
                'android-arm-cpu',
                'android-arm64-cpu',
                'android-x86-cpu',
                'android-x86_64-cpu',
                'linux-armhf-cpu',
                'linux-x86_64-centos6-cpu',
                'linux-x86_64-centos6-cpu-avx2',
                'linux-x86_64-centos6-cpu-avx512',
                'linux-x86_64-cpu',
                'linux-x86_64-cpu-avx2',
                'linux-x86_64-cpu-avx512',
                'linux-x86_64-python-2',
                'linux-x86_64-python-3',
                'linux-x86_64-generic'
        ]

        for (Map pltm : platforms) {
            Map platform = pltm
            String platformName = platform.get('name')
            String backend = platform.get('backend')
            String cpuExtension = platform.get('cpuExtension')
            String scalaVersion = platform.get('scalaVersion')
            String sparkVersion = platform.get('sparkVersion')
            String os = platform.get('os')
            String pythonVersion = platform.containsKey('pythonVersion') ?
                    'python' + '-' + platform.get('pythonVersion') : ''
            String streamName = [
                    platformName, os, backend, cpuExtension, pythonVersion
            ].findAll().join('-')


            /* Create stream body */
            streams["$streamName"] = {
                script.node(streamName) {
                    Boolean isUnixNode = script.isUnix()
                    String separator = isUnixNode ? '/' : '\\'
                    /* Workaround for Windows path length limitation */
                    String wsFolderName = ((isUnixNode) ? 'workspace' : 'ws') +
                            separator +
                            [(isUnixNode) ?
                                     projectName :
                                     (projectName.contains('deeplearning4j') ? 'dl4j' : projectName),
                             branchName,
                             streamName].join('-').replaceAll('/', '-')
                    /* Get logic for the run, depending on changes */
                    Module module = new Module([
                            backend             : backend,
                            branchName          : branchName,
                            cpuExtension        : cpuExtension,
                            isUnixNode          : isUnixNode,
                            modulesToBuild      : modulesToBuild,
                            platformName        : platformName,
                            releaseApproved     : releaseApproved,
                            releaseBranchPattern: releaseBranchPattern,
                            releaseVersion      : releaseVersion,
                            scalaVersion        : scalaVersion,
                            sparkVersion        : sparkVersion,
                            streamName          : streamName
                    ], script)

                    if (buildInContainer.contains(streamName)) {
                        /* Redefine default workspace to fix Windows path length limitation */
                        script.ws(wsFolderName) {
                            try {
                                script.stage('Checkout') {
                                    script.checkout script.scm
                                }

                                script.container('builder') {
                                    module.stagesToRun()
                                }
                            }
                            finally {
                                testResults.add(module.testResults)
                                script.archiveArtifacts allowEmptyArchive: true, artifacts: '**/hs_err_pid*.log'
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
                    } else {
                        /* Redefine default workspace to fix Windows path length limitation */
                        script.ws(wsFolderName) {
                            try {
                                if (platformName.contains('ppc64') ||
                                        platformName.contains('linux') &&
                                        backend?.contains('cuda')
                                ) {
//                                TODO: Check local repository mapping for builds in docker
                                    /* Get docker container configuration */
                                    Map dockerConf = script.pipelineEnv.getDockerConfig(streamName)

                                    String dockerImageName = dockerConf['image'] ?:
                                            script.error('Docker image name is missing.')
                                    String dockerImageParams = dockerConf?.params

                                    script.docker.image(dockerImageName).inside(dockerImageParams) {
                                        script.stage('Checkout') {
                                            script.checkout script.scm
                                        }

                                        module.stagesToRun()
                                    }
                                } else {
                                    script.stage('Checkout') {
                                        script.checkout script.scm
                                    }

                                    module.stagesToRun()
                                }
                            }
                            finally {
                                testResults.add(module.testResults)
                                script.archiveArtifacts allowEmptyArchive: true, artifacts: '**/hs_err_pid*.log'
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

    protected List getPlatforms(String module = '') {
        List platforms

        // Set linux platform for fast tests pipeline
//        if (!(branchName =~ 'master|release|PR-\\d+|deeplearning4j-\\d+.\\d+.\\d+.*')) {
//            platforms = [
//                    [name: 'linux-x86_64', sparkVersion: '1', scalaVersion: '2.10', backend: 'cpu']
//            ]
//        } else {
        switch (module) {
            case ['libnd4j', 'nd4j']:
                platforms = [
                        [name: 'android-arm', scalaVersion: '2.10', backend: 'cpu'],
                        [name: 'android-arm64', scalaVersion: '2.11', backend: 'cpu'],
                        [name: 'android-x86', scalaVersion: '2.10', backend: 'cpu'],
                        [name: 'android-x86_64', scalaVersion: '2.11', backend: 'cpu'],

                        [name: 'ios-arm64', scalaVersion: '2.11', backend: 'cpu'],
                        [name: 'ios-x86_64', scalaVersion: '2.11', backend: 'cpu'],

                        [name: 'linux-ppc64le', scalaVersion: '2.10', backend: 'cpu'],
                        [name: 'linux-ppc64le', scalaVersion: '2.11', backend: 'cuda-9.2'],
                        [name: 'linux-ppc64le', scalaVersion: '2.11', backend: 'cuda-10.0'],
                        [name: 'linux-ppc64le', scalaVersion: '2.11', backend: 'cuda-10.1'],

                        (branchName == 'master' || branchName.contains(releaseBranchPattern)) ?
                                [name: 'linux-x86_64', os: 'centos6', sparkVersion: '1', scalaVersion: '2.10', backend: 'cpu'] :
                                [name: 'linux-x86_64', sparkVersion: '1', scalaVersion: '2.10', backend: 'cpu'],

                        (branchName == 'master' || branchName.contains(releaseBranchPattern)) ?
                                [name: 'linux-x86_64', os: 'centos6', sparkVersion: '1', scalaVersion: '2.11', backend: 'cpu', cpuExtension: 'avx2'] :
                                [name: 'linux-x86_64', sparkVersion: '1', scalaVersion: '2.11', backend: 'cpu', cpuExtension: 'avx2'],

                        (branchName == 'master' || branchName.contains(releaseBranchPattern)) ?
                                [name: 'linux-x86_64', os: 'centos6', sparkVersion: '2', scalaVersion: '2.11', backend: 'cpu', cpuExtension: 'avx512'] :
                                [name: 'linux-x86_64', sparkVersion: '2', scalaVersion: '2.11', backend: 'cpu', cpuExtension: 'avx512'],

                        [name: 'linux-x86_64', sparkVersion: '2', scalaVersion: '2.11', backend: 'cuda-9.2'],
                        [name: 'linux-x86_64', sparkVersion: '2', scalaVersion: '2.11', backend: 'cuda-10.0'],
                        [name: 'linux-x86_64', sparkVersion: '2', scalaVersion: '2.11', backend: 'cuda-10.1'],

                        [name: 'macosx-x86_64', scalaVersion: '2.10', backend: 'cpu'],
                        [name: 'macosx-x86_64', scalaVersion: '2.11', backend: 'cpu', cpuExtension: 'avx2'],
                        /*
                         FIXME: avx512 required Xcode 9.2 to be installed on Mac slave,
                         at the same time for CUDA - Xcode 8 required,
                         which means that we can't enable avx512 builds at the moment
                        */
//                        [name: 'macosx-x86_64', scalaVersion: '2.11', backend: 'cpu', cpuExtension: 'avx512'],
                        // FIXME: CUDA for macOS has been disabled, because of https://github.com/deeplearning4j/deeplearning4j/issues/7186 issue
//                        [name: 'macosx-x86_64', scalaVersion: '2.11', backend: 'cuda-9.0'],
//                        [name: 'macosx-x86_64', scalaVersion: '2.11', backend: 'cuda-9.2'],
//                        [name: 'macosx-x86_64', scalaVersion: '2.11', backend: 'cuda-10.0'],

                        [name: 'windows-x86_64', scalaVersion: '2.10', backend: 'cpu'],
                        [name: 'windows-x86_64', scalaVersion: '2.11', backend: 'cpu', cpuExtension: 'avx2'],
                        /* FIXME: avx512 */
//                        [name: 'windows-x86_64', scalaVersion: '2.11', backend: 'cpu', cpuExtension: 'avx512'],
                        [name: 'windows-x86_64', scalaVersion: '2.11', backend: 'cuda-9.2'],
                        [name: 'windows-x86_64', scalaVersion: '2.11', backend: 'cuda-10.0'],
                        [name: 'windows-x86_64', scalaVersion: '2.11', backend: 'cuda-10.1'],
                        [name: 'linux-armhf', scalaVersion: '2.10', backend: 'cpu']
                ]
                break
            case 'deeplearning4j':
                platforms = [
                        [name: 'linux-x86_64', sparkVersion: '2', scalaVersion: '2.11', backend: 'cuda-9.2'],
                        [name: 'linux-x86_64', sparkVersion: '2', scalaVersion: '2.11', backend: 'cuda-10.0'],
                        [name: 'linux-x86_64', sparkVersion: '2', scalaVersion: '2.11', backend: 'cuda-10.1']
                ]
                break
            case ['pydatavec', 'jumpy', 'pydl4j']:
                platforms = [
                        [name: 'linux-x86_64', pythonVersion: '2'],
                        [name: 'linux-x86_64', pythonVersion: '3']
                ]
                break
            case ['docs']:
                platforms = [
                        [name: 'linux-x86_64-generic', sparkVersion: '1', scalaVersion: '2.10']
                ]
                break
            default:
                platforms = [
                        [name: 'linux-x86_64-generic', sparkVersion: '1', scalaVersion: '2.10'],
                        [name: 'linux-x86_64-generic', sparkVersion: '1', scalaVersion: '2.11'],
                        [name: 'linux-x86_64-generic', sparkVersion: '2', scalaVersion: '2.11']
                ]
                break
        }
//        }

        platforms
    }

    @NonCPS
    protected void setBuildParameters() {
        List commonJobProperties = []

        if (script.env.JOB_BASE_NAME == 'master') {
            commonJobProperties.addAll([
                    script.pipelineTriggers([script.cron('@midnight')])
            ])
        }

        script.properties(commonJobProperties)
    }

    @NonCPS
    protected terminateOlderBuilds(String jobName, int buildsNumber) {
        def currentJob = Jenkins.instance.getItemByFullName(jobName)

        for (def build : currentJob.builds) {
            if (build.isBuilding() && build.number.toInteger() != buildsNumber) {
                build.doStop()
                script.echo "[WARNING] Build number ${build.number} was " +
                        "terminated because of current(latest) run."
            }
        }
    }

    protected void getReleaseParameters() {
        def userInput

        script.timeout(time: 1, unit: 'HOURS') {
            userInput = script.input message: 'Perform release?',
                    parameters: [
                            script.string(defaultValue: '', description: 'Release version', name: 'releaseVersion'),
                            script.string(defaultValue: '', description: 'Snapshot version', name: 'snapshotVersion'),
                            script.string(defaultValue: '', description: 'Staging repository ID', name: 'stagingRepository'),
                    ],
                    submitter: 'sshepel, saudet, agibsonccc',
                    submitterParameter: 'approvedBy'
        }

        if (!userInput) {
            script.error "[ERROR] Missing user-provided values."
        }

        if (checkStagingRepoFormat(userInput.stagingRepository)) {
            releaseApproved = (userInput.approvedBy) ? true : script.error("[ERROR] Can't get approver ID.")
            releaseVersion = userInput.releaseVersion
            snapshotVersion = userInput.snapshotVersion
            script.env.STAGING_REPOSITORY = userInput.stagingRepository
        } else {
            script.error "[ERROR] Provided staging repository ID ${script.env.STAGING_REPOSITORY} is not valid."
        }
    }

    protected boolean checkStagingRepoFormat(String stagingRepoId) {
        stagingRepoId =~ /\w+-\d+/
    }

//    private String getModuleName(String changedFilePath, String previousPath = '') {
//        List filePath = changedFilePath.tokenize('/')
//
//        if (filePath.size() > 2) {
//            filePath.pop()
//
//            if (previousPath && filePath) {
//                filePath?.pop()
//            }
//
//            filePath.push('pom.xml')
//        }
//        else if (filePath.size() == 2) {
//            filePath.pop()
//        }
//
//        String pomPath = filePath.join('/')
//
//        Boolean isPomExists = script.fileExists pomPath
//
//        if (!isPomExists && pomPath != previousPath) {
//            getModuleName(pomPath, changedFilePath)
//        } else {
//            println "Resutl " + pomPath
//        }
//    }
//
//    @NonCPS
//    private List getChanges() {
//        Boolean unixNode = script.isUnix()
//        String shell = unixNode ? 'sh' : 'bat'
//
//        script."$shell"(script: "git diff-tree --no-commit-id --name-only -r HEAD", returnStdout: true).trim().tokenize('\n')
//    }
}