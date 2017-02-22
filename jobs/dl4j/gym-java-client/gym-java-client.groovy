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

  echo "Building ${GYM_JAVA_CLIENT_PROJECT} version ${VERSION}"

  dir("${GYM_JAVA_CLIENT_PROJECT}") {
    functions.checktag("${GYM_JAVA_CLIENT_PROJECT}")

/*
    sh ("sed -i 's/<nd4j.version>.*<\\/nd4j.version>/<nd4j.version>${VERSION}<\\/nd4j.version>/' pom.xml")
    sh ("sed -i 's/<datavec.version>.*<\\/datavec.version>/<datavec.version>${VERSION}<\\/datavec.version>/' pom.xml")
*/
    // sh ("'${mvnHome}/bin/mvn' versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=${VERSION}")
    functions.verset("${VERSION}", true)

    configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
      sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests  -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION} ")
    }
  }
}

// Messages for debugging
echo 'MARK: end of gym-java-client.groovy'
