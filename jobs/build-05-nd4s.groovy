stage('Nd4s Preparation') {
  checkout([$class: 'GitSCM',
             branches: [[name: '*/intropro']],
             doGenerateSubmoduleConfigurations: false,
             extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: '${ND4S_PROJECT}'], [$class: 'CloneOption', honorRefspec: true, noTags: true, reference: '', shallow: true]],
            //  extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: '${ND4S_PROJECT}'], [$class: 'CloneOption', honorRefspec: true, noTags: false, reference: '', shallow: true]],
             submoduleCfg: [],
             userRemoteConfigs: [[url: 'https://github.com/${ACCOUNT}/${ND4S_PROJECT}.git']]])

  echo "Releasing ${ND4S_PROJECT} version ${RELEASE_VERSION} (${SNAPSHOT_VERSION}) to repository ${STAGING_REPOSITORY}"
  echo "Check if ${RELEASE_VERSION} has been released already"

  dir("${ND4S_PROJECT}") {
    def check_tag = sh(returnStdout: true, script: "git tag -l ${ND4S_PROJECT}-${RELEASE_VERSION}")
    if (!check_tag) {
        println ("There is no tag with provided value: ${ND4S_PROJECT}-${RELEASE_VERSION}" )
    }
    else {
        println ("Version exists: " + check_tag)
        error("Fail to proceed with current version: " + check_tag)
    }

    sh ("sed -i 's/version := \".*\",/version := \"$RELEASE_VERSION\",/' build.sbt")
    sh ("sed -i 's/nd4jVersion := \".*\",/nd4jVersion := \"$RELEASE_VERSION\",/' build.sbt")
    //sh ("sbt +publishSigned")
  }
}

// stage('Nd4s Codecheck') {
//   echo 'Check $ACCOUNT/$PROJECT code with SonarQube'
// }

stage ('Nd4s Build') {
  dir("${ND4S_PROJECT}") {
    // all of git tag or commit actions should be in pipeline.groovy after user "Release" input
    //sh "git commit -a -m 'Update to version $RELEASE_VERSION'"
    //sh "git tag -a -m '$ND4S_PROJECT-$RELEASE_VERSION" "$ND4S_PROJECT-$RELEASE_VERSION'"
    sh ("sed -i 's/version := \".*\",/version := \"$SNAPSHOT_VERSION\",/' build.sbt")
    sh ("sed -i 's/nd4jVersion := \".*\",/nd4jVersion := \"$SNAPSHOT_VERSION\",/' build.sbt")
    //sh "git commit -a -m 'Update to version $SNAPSHOT_VERSION'"
    // echo "Successfully performed release of ${ND4S_PROJECT} version ${RELEASE_VERSION} (${SNAPSHOT_VERSION}) to repository ${STAGING_REPOSITORY}"
  }
}
// Messages for debugging
echo 'MARK: end of build-05-nd4s.groovy'
