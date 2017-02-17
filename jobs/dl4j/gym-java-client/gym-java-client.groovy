tool name: 'M339', type: 'maven'
def mvnHome = tool 'M339'

env.JAVA_HOME = "${tool 'jdk-8u121'}"
env.PATH = "${env.JAVA_HOME}/bin:${env.PATH}"


stage("${GYM_JAVA_CLIENT_PROJECT}-checkout-sources") {
  functions.get_project_code("${GYM_JAVA_CLIENT_PROJECT}")
}

// stage("${GYM_JAVA_CLIENT_PROJECT}-Codecheck") {
//   functions.sonar("${GYM_JAVA_CLIENT_PROJECT}")
// }

stage("${GYM_JAVA_CLIENT_PROJECT}-build") {

  echo "Releasing ${GYM_JAVA_CLIENT_PROJECT} version ${RELEASE_VERSION}"

  dir("${GYM_JAVA_CLIENT_PROJECT}") {
    functions.checktag("${GYM_JAVA_CLIENT_PROJECT}")

/*
    sh ("sed -i 's/<nd4j.version>.*<\\/nd4j.version>/<nd4j.version>${RELEASE_VERSION}<\\/nd4j.version>/' pom.xml")
    sh ("sed -i 's/<datavec.version>.*<\\/datavec.version>/<datavec.version>${RELEASE_VERSION}<\\/datavec.version>/' pom.xml")
*/
    // sh ("'${mvnHome}/bin/mvn' versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=${RELEASE_VERSION}")
    functions.verset("${RELEASE_VERSION}", true)

    configFileProvider([configFile(fileId: 'MAVEN_SETTINGS_DO-192', variable: 'MAVEN_SETTINGS')]) {
      sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests  -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION} ")
    }
  }
}

// Messages for debugging
echo 'MARK: end of gym-java-client.groovy'
