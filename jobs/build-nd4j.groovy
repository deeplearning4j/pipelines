timestamps {
  node('master') {
    // node('ec2_x86_64') {
    // install Maven and add it to the path
    // env.PATH = "${tool 'M3'}/bin:${env.PATH}"
    // discard old builds by keeping log of 5 last
    properties([[$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '5']]]);
    // Define MAVEN_HOME
    // tool name: 'M339', type: 'maven'
    def mvnHome
    mvnHome = tool 'M339'
    // Define Job Variables
    def RELEASE_VERSION
    def SNAPSHOT_VERSION
    def STAGING_REPOSITORY
    def PRESETSDIR
    def ACCOUNT
    def PROJECT
    def LIBPROJECT
    def PLATFORM_NAME
    def OpenBLAS_HOME
    def ANDROID_NDK
    // Cleanup workspace
    //step([$class: 'WsCleanup'])
    // Set or choose variables values
    stage('SetVariables') {
        def userInput = input(
                id: 'userInput', message: 'Let\'s promote?', parameters: [
                [$class: 'TextParameterDefinition', defaultValue: '1.0.0', description: 'Release Version', name: 'Release'],
                [$class: 'TextParameterDefinition', defaultValue: '1.0.0-SNAPSHOT', description: 'Snapshot Version', name: 'Snapshot'],
                [$class: 'TextParameterDefinition', defaultValue: '', description: 'Choose Repository', name: 'Repository'],
                [$class: 'TextParameterDefinition', defaultValue: 'presets', description: 'Set directory', name: 'dirSet'],
                [$class: 'TextParameterDefinition', defaultValue: 'deeplearning4j', description: 'Owner of the project at github.com', name: 'accountName'],
                [$class: 'TextParameterDefinition', defaultValue: 'nd4j', description: 'Git Project Name', name: 'ProjectName'],
                [$class: 'TextParameterDefinition', defaultValue: 'libnd4j', description: 'Project Name for libraries', name: 'libProjectName'],
                [$class: 'ChoiceParameterDefinition', choices: "linux-x86_64\nandroid-arm\nandroid-x86\nlinux-x86\nlinux-ppc64le", description: 'OpenBLAS home directory', name: 'Platform'],
                [$class: 'TextParameterDefinition', defaultValue: '$WORKSPACE/javacpp-presets/openblas/cppbuild/$PLATFORM_NAME/', description: 'OpenBLAS home directory"', name: 'BlasHome'],
                [$class: 'TextParameterDefinition', defaultValue: '/opt/android-ndk/', description: 'Android NDK home directory', name: 'NDK']
        ])

        RELEASE_VERSION = userInput['Release']
        SNAPSHOT_VERSION = userInput['Snapshot']
        STAGING_REPOSITORY = userInput['Repository']
        PRESETSDIR = userInput['dirSet']
        ACCOUNT = userInput['accountName']
        PROJECT = userInput['ProjectName']
        LIBPROJECT = userInput['libProjectName']
        PLATFORM_NAME = userInput['Platform']
        OpenBLAS_HOME = userInput['BlasHome']
        ANDROID_NDK = userInput['NDK']

    }
    // Checkout source code from repository
    stage('CheckoutSources') {
        checkout([$class                           : 'GitSCM',
                  branches                         : [[name: '*/intropro']],
                  doGenerateSubmoduleConfigurations: false,
                  extensions                       : [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${PROJECT}"], [$class: 'CloneOption', honorRefspec: true, noTags: false, reference: '', shallow: true]],
                  submoduleCfg                     : [],
                  userRemoteConfigs                : [[url: "https://github.com/${ACCOUNT}/${PROJECT}.git"]]
        ])

        checkout([$class                           : 'GitSCM',
                  branches                         : [[name: '*/intropro']],
                  doGenerateSubmoduleConfigurations: false,
                  extensions                       : [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${LIBPROJECT}"], [$class: 'CloneOption', honorRefspec: true, noTags: false, reference: '', shallow: true]],
                  submoduleCfg                     : [],
                  userRemoteConfigs                : [[url: "https://github.com/${ACCOUNT}/${LIBPROJECT}.git"]]])
        sh "ls -la"
    }
    // Check if current RELEASE_VERSION is exist in repository
   stage('Check Version') {
     dir("$PROJECT") {
            def check_tag = sh(returnStdout: true, script: "git tag -l ${PROJECT}-${RELEASE_VERSION}")
            if (!check_tag) {
                println ("There is no version with provided value: ${PROJECT}-${RELEASE_VERSION}" )
            }
            else {
                println ("Version is exist: " + check_tag)
                error("Fail to proceed with current version!")
            }
     }
   }
    //
   stage('BuildNativeOperations'){
     dir("$LIBPROJECT") {
            // sh ("git tag -l \"libnd4j-$RELEASE_VERSION\"")
            def check_tag = sh (returnStdout: true, script: """git tag -l \"$LIBPROJECT-$RELEASE_VERSION\"""")
            echo check_tag
            if (check_tag == '') {
                //  if (check_tag == null) {
                echo "Checkpoint #1"
                // input 'Pipeline has paused and needs your input before proceeding'
                sh "export TRICK_NVCC=YES && export LIBND4J_HOME=${WORKSPACE}/${LIBPROJECT} && ./buildnativeoperations.sh -c cpu"
                sh "export TRICK_NVCC=YES && export LIBND4J_HOME=${WORKSPACE}/${LIBPROJECT} && ./buildnativeoperations.sh -c cuda -v 7.5"
                sh "export TRICK_NVCC=YES && export LIBND4J_HOME=${WORKSPACE}/${LIBPROJECT} && ./buildnativeoperations.sh -c cuda -v 8.0"
                //  sh "git tag -a -m "libnd4j-$RELEASE_VERSION""
            }
        }
    }

     echo 'Build components with Maven'
     dir("$PROJECT") {
        // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
        // Temporary section - please remove once it commited updates to source code
        // configFileProvider(
        //         [configFile(fileId: 'MAVEN_POM_DO-192', variable: 'POM_XML')
        //     ]) {
        //     sh "cp ${POM_XML} pom.xml"
        // }
        // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
        echo 'Set Project Version'
        sh("'${mvnHome}/bin/mvn' versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=$RELEASE_VERSION")

        sh "./change-scala-versions.sh 2.10"
        sh "./change-cuda-versions.sh 7.5"

        configFileProvider(
                [configFile(fileId: 'MAVEN_SETTINGS_DO-192', variable: 'MAVEN_SETTINGS')
                ]) {
            sh ("'${mvnHome}/bin/mvn' -s $MAVEN_SETTINGS clean deploy -DskipTests ")
        }


    }
  }
}
