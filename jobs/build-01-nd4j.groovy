tool name: 'M339', type: 'maven'
def mvnHome = tool 'M339'
stage('Nd4j Preparation') {
  checkout([$class: 'GitSCM',
             branches: [[name: '*/intropro']],
             doGenerateSubmoduleConfigurations: false,
            //  extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: '${PROJECT}'], [$class: 'CloneOption', honorRefspec: true, noTags: true, reference: '', shallow: true]],
             extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: '${PROJECT}'], [$class: 'CloneOption', honorRefspec: true, noTags: false, reference: '', shallow: true]],
             submoduleCfg: [],
             userRemoteConfigs: [[url: 'git@github.com:${ACCOUNT}/${PROJECT}.git', credentialsId: '${GITCREDID}']]])
            //  userRemoteConfigs: [[url: 'git@github.com:${ACCOUNT}/${PROJECT}.git', credentialsId: 'github-private-deeplearning4j-id-1']]])

  checkout([$class: 'GitSCM',
             branches: [[name: '*/intropro']],
             doGenerateSubmoduleConfigurations: false,
            //  extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: '${LIBPROJECT}'], [$class: 'CloneOption', honorRefspec: true, noTags: true, reference: '', shallow: true]],
             extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: '${LIBPROJECT}'], [$class: 'CloneOption', honorRefspec: true, noTags: false, reference: '', shallow: true]],
             submoduleCfg: [],
             userRemoteConfigs: [[url: 'git@github.com:${ACCOUNT}/${LIBPROJECT}.git', credentialsId: '${GITCREDID}']]])
            //  userRemoteConfigs: [[url: 'git@github.com:${ACCOUNT}/${LIBPROJECT}.git', credentialsId: 'github-private-deeplearning4j-id-1']]])
}

// stage('Nd4j Codecheck') {
//   echo 'Check $ACCOUNT/$PROJECT code with SonarQube'
// }
// stage('Libnd4j Codecheck') {
//   echo 'Check $ACCOUNT/$PROJECT code with SonarQube'
// }

stage('Nd4j Build') {
  echo "Releasing ${PROJECT} version ${RELEASE_VERSION} (${SNAPSHOT_VERSION}) to repository ${STAGING_REPOSITORY}"
  echo ("Check if ${RELEASE_VERSION} has been released already")

  dir("${PROJECT}") {
    def check_tag = sh(returnStdout: true, script: "git tag -l ${PROJECT}-${RELEASE_VERSION}")
    if (!check_tag) {
        println ("There is no tag with provided value: ${PROJECT}-${RELEASE_VERSION}" )
    }
    else {
        println ("Version exists: " + check_tag)
        error("Failed to proceed with current version: " + check_tag)
    }

    // def exitValue = sh (returnStdout: true, script: """git tag -l \"$PROJECT-$RELEASE_VERSION\"""")
    // echo ${exitValue}
    // if (exitValue != null) {
    //  //  echo 'Error: Version $RELEASE_VERSION has already been released!'
    //   // error 'Version $RELEASE_VERSION has already been released!'
    //   error 'Version ${exitValue} has already been released!'
    // }
  }

  echo 'Build Native Operations'
  dir("${LIBPROJECT}") {
    def check_tag = sh(returnStdout: true, script: "git tag -l ${LIBPROJECT}-${RELEASE_VERSION}")
    if (!check_tag) {
        println ("There is no tag with provided value: ${LIBPROJECT}-${RELEASE_VERSION}" )
    }
    else {
        println ("Version exists: " + check_tag)
        error("Failed to proceed with current version: " + check_tag)
    }

        echo "Building ${LIBPROJECT} version ${RELEASE_VERSION} (${SNAPSHOT_VERSION})"
        //  sh "export TRICK_NVCC=YES && export LIBND4J_HOME=${WORKSPACE}/$LIBPROJECT && ./buildnativeoperations.sh -c cpu"
        //  sh "export TRICK_NVCC=YES && export LIBND4J_HOME=${WORKSPACE}/$LIBPROJECT && ./buildnativeoperations.sh -c cuda -v 7.5"
        //  sh "export TRICK_NVCC=YES && export LIBND4J_HOME=${WORKSPACE}/$LIBPROJECT && ./buildnativeoperations.sh -c cuda -v 8.0"
        // all of git tag or commit actions should be in pipeline.groovy after user "Release" input
        //  sh "git tag -a -m "libnd4j-$RELEASE_VERSION""
  }
}

  echo 'Build components with Maven'
  dir("${PROJECT}") {
    echo 'Set Project Version'
    //  sh ("'${mvnHome}/bin/mvn' versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=$RELEASE_VERSION")
    echo 'Maven Build, Package and Deploy'
    def check_repo = "${STAGING_REPOSITORY}"
    echo check_repo
      if (!check_repo) {
        echo 'STAGING_REPOSITORY is not set'
        sh "./change-scala-versions.sh 2.10"
        sh "./change-cuda-versions.sh 7.5"

        // configFileProvider([configFile(fileId: 'maven-release-bintray-settings-1', variable: 'MAVEN_SETTINGS'),
        //                     configFile(fileId: 'maven-release-bintray-settings-security-1', variable: 'MAVEN_SECURITY_SETTINGS')]) {
        //                       sh ("'${mvnHome}/bin/mvn' -s $MAVEN_SETTINGS clean deploy \
        //                            -Dsettings.security=$MAVEN_SECURITY_SETTINGS \
        //                            -Dgpg.executable=gpg2 -Dgpg.skip -DperformRelease \
        //                            -DskipTests -Denforcer.skip -DstagingRepositoryId=$STAGING_REPOSITORY")
        //                     }
      }
  }

// Messages for debugging
echo 'MARK: end of build-01-nd4j.groovy'
