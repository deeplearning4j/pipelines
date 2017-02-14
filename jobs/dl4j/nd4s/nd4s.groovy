tool name: 'SBT100M4', type: 'org.jvnet.hudson.plugins.SbtPluginBuilder$SbtInstallation'
def sbtHome = tool 'SBT100M4'

env.JAVA_HOME = "${tool 'jdk-8u121'}"
env.PATH = "${env.JAVA_HOME}/bin:${env.PATH}"

stage("${ND4S_PROJECT}-CheckoutSources") {
  functions.get_project_code("${ND4S_PROJECT}")
}

// There is no scala plugin for SonarQube
// stage("${ND4S_PROJECT}-Codecheck") {
//   functions.sonar("${ND4S_PROJECT}")
// }

stage("${ND4S_PROJECT}-Build") {

  echo "Releasing ${ND4S_PROJECT} version ${RELEASE_VERSION}"

  dir("${ND4S_PROJECT}") {
    functions.checktag("${ND4S_PROJECT}")

    sh ("sed -i 's/version := \".*\",/version := \"${RELEASE_VERSION}\",/' build.sbt")
    sh ("sed -i 's/nd4jVersion := \".*\",/nd4jVersion := \"${ND4J_VERSION}\",/' build.sbt")
    configFileProvider(
            [configFile(fileId: "${SBTCREDID}", variable: 'SBT_CREDENTIALS')
            ]) {
      sh ("test -d ${HOME}/.ivy2 || mkdir ${HOME}/.ivy2")
      sh ("cp ${SBT_CREDENTIALS}  ${HOME}/.ivy2/.credentials")
    }
    sh ("'${sbtHome}/bin/sbt' +publish")
    sh ("rm -f ${HOME}/.ivy2/.credentials")
    // sh "'${sbtHome}/bin/sbt' +publishSigned"
    // sh "'${sbtHome}/bin/sbt' test -Dsbt.log.noformat=true"
  }
}

// Messages for debugging
echo 'MARK: end of nd4s.groovy'
