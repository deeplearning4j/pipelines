timestamps {
 node('power8') {
   tool name: 'M339', type: 'maven'
   def mvnHome
   mvnHome = tool 'M339'
  //  mvnHome = tool 'M3'
  //  sh 'echo $PATH'
   step([$class: 'WsCleanup'])
   stage('Preparation')    {
     checkout([$class: 'GitSCM',
       branches: [[name: '*/intropro']],
      //  branches: [[name: '*/intropro']],
       doGenerateSubmoduleConfigurations: false,
       extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: '$PROJECT'], [$class: 'CloneOption', honorRefspec: true, noTags: true, reference: '', shallow: true]],
       submoduleCfg: [],
       userRemoteConfigs: [[url: 'https://github.com/$ACCOUNT/$PROJECT.git']]])
     sh "cd $PROJECT && git status && git branch && git tag -l $PROJECT-$RELEASE_VERSION && cd .."

     checkout([$class: 'GitSCM',
       branches: [[name: '*/intropro']],
      //  branches: [[name: '*/intropro']],
       doGenerateSubmoduleConfigurations: false,
       extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: '$LIBPROJECT'], [$class: 'CloneOption', honorRefspec: true, noTags: true, reference: '', shallow: true]],
       submoduleCfg: [],
       userRemoteConfigs: [[url: 'https://github.com/$ACCOUNT/$LIBPROJECT.git']]])
   }

   stage('Build') {
    //  echo 'Check if $RELEASE_VERSION has been released already'
    //  dir("$PROJECT") {
    //    def exitValue = sh (returnStdout: true, script: """git tag -l \"$PROJECT-$RELEASE_VERSION\"""")
    //   //  if (exitValue != '') {
    //    if (exitValue != null) {
    //       // error statement stops pipeline if if is true
    //       // echo 'Error: Version $RELEASE_VERSION has already been released!'
    //       error 'Version $RELEASE_VERSION has already been released!'
    //    }
    //  }

     echo 'Build Native Operations'
     dir("$LIBPROJECT") {
       // sh ("git tag -l \"libnd4j-$RELEASE_VERSION\"")
       def check_tag = sh (returnStdout: true, script: """git tag -l \"$LIBPROJECT-$RELEASE_VERSION\"""")
       echo check_tag
         if (check_tag == '') {
        //  if (check_tag == null) {
             echo "Checkpoint #1"
             // input 'Pipeline has paused and needs your input before proceeding'
             sh "export TRICK_NVCC=YES && export LIBND4J_HOME=${WORKSPACE}/$LIBPROJECT && ./buildnativeoperations.sh -c cpu"
             sh "export TRICK_NVCC=YES && export LIBND4J_HOME=${WORKSPACE}/$LIBPROJECT && ./buildnativeoperations.sh -c cuda -v 7.5"
             sh "export TRICK_NVCC=YES && export LIBND4J_HOME=${WORKSPACE}/$LIBPROJECT && ./buildnativeoperations.sh -c cuda -v 8.0"
            //  sh "git tag -a -m "libnd4j-$RELEASE_VERSION""
         }
     }

    //  echo 'Build components with Maven'
    //  dir("$PROJECT") {
    //    echo 'Set Project Version'
    //    sh ("'${mvnHome}/bin/mvn' versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=$RELEASE_VERSION")
    //    echo 'Maven Build, Package and Deploy'
    //    def check_repo = "$STAGING_REPOSITORY"
    //   //  echo check_repo
    //      if (check_repo == '') {
    //        echo 'STAGING_REPOSITORY is not set'
    //        sh "./change-scala-versions.sh 2.10"
    //        sh "./change-cuda-versions.sh 7.5"
    //        configFileProvider(
    //          [configFile(fileId: 'maven-release-bintray-settings-1', variable: 'MAVEN_SETTINGS'),
    //           configFile(fileId: 'maven-release-bintray-settings-security-1', variable: 'MAVEN_SECURITY_SETTINGS')]) {
    //             sh ("'${mvnHome}/bin/mvn' -s $MAVEN_SETTINGS clean deploy \
    //                    -Dsettings.security=$MAVEN_SECURITY_SETTINGS \
    //                    -Dgpg.executable=gpg2 -Dgpg.skip -DperformRelease \
    //                    -DskipTests -Denforcer.skip -DstagingRepositoryId=$STAGING_REPOSITORY")
    //           }
    //      }
    //  }
   }
   step([$class: 'WsCleanup'])
 }
}
