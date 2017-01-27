tool name: 'SBT100M4', type: 'org.jvnet.hudson.plugins.SbtPluginBuilder$SbtInstallation'
def sbtHome = tool 'SBT100M4'

functions = load 'jobs/functions.groovy'

stage("${ND4S_PROJECT}-Build") {

  functions.get_project_code("${ND4S_PROJECT}")

  echo "Releasing ${ND4S_PROJECT} version ${RELEASE_VERSION}" // (${SNAPSHOT_VERSION}) to repository ${STAGING_REPOSITORY}"
  echo "Check if ${RELEASE_VERSION} has been released already"

  dir("${ND4S_PROJECT}") {
    functions.checktag("${ND4S_PROJECT}")

    sh ("sed -i 's/version := \".*\",/version := \"${RELEASE_VERSION}\",/' build.sbt")
    //sh ("sed -i 's/nd4jVersion := \".*\",/nd4jVersion := \"${RELEASE_VERSION}\",/' build.sbt")
    configFileProvider(
            [configFile(fileId: 'SBT_CREDENTIALS_DO-192', variable: 'SBT_CREDENTIALS')
            ]) {
      sh ("cp ${SBT_CREDENTIALS}  ${HOME}/.ivy2/.credentials")
    }
    sh ("'${sbtHome}/bin/sbt' +publish")
    sh ("rm -f ${HOME}/.ivy2/.credentials")
    //sh ("sbt +publishSigned")
    // sh "'${sbtHome}/bin/sbt' +publishSigned"
    // sh "'${sbtHome}/bin/sbt' test -Dsbt.log.noformat=true"
  }
}

// There is no scala plugin for SonarQube
// stage('Nd4s Codecheck') {
//   functions.sonar("${ND4S_PROJECT}")
// }

/*stage ('Nd4s Build') {
  dir("${ND4S_PROJECT}") {

    //sh "git commit -a -m 'Update to version $RELEASE_VERSION'"
    //sh "git tag -a -m '$ND4S_PROJECT-$RELEASE_VERSION" "$ND4S_PROJECT-$RELEASE_VERSION'"
    sh ("sed -i 's/version := \".*\",/version := \"$SNAPSHOT_VERSION\",/' build.sbt")
    sh ("sed -i 's/nd4jVersion := \".*\",/nd4jVersion := \"$SNAPSHOT_VERSION\",/' build.sbt")
    //sh "git commit -a -m 'Update to version $SNAPSHOT_VERSION'"
    // echo "Successfully performed release of ${ND4S_PROJECT} version ${RELEASE_VERSION} (${SNAPSHOT_VERSION}) to repository ${STAGING_REPOSITORY}"
  }
}*/
// Messages for debugging
echo 'MARK: end of build-05-nd4s.groovy'
