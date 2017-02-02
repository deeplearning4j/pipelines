tool name: 'M339', type: 'maven'
def mvnHome = tool 'M339'

functions = load 'jobs/dl4j/functions.groovy'

stage("${GYM_JAVA_CLIENT_PROJECT}-CheckoutSources") {
  functions.get_project_code("${GYM_JAVA_CLIENT_PROJECT}")
}

// stage("${GYM_JAVA_CLIENT_PROJECT}-Codecheck") {
//   functions.sonar("${GYM_JAVA_CLIENT_PROJECT}")
// }

stage("${GYM_JAVA_CLIENT_PROJECT}-Build") {

  echo "Releasing ${GYM_JAVA_CLIENT_PROJECT} version ${RELEASE_VERSION}"

  dir("${GYM_JAVA_CLIENT_PROJECT}") {
    functions.checktag("${GYM_JAVA_CLIENT_PROJECT}")

    sh ("sed -i 's/<nd4j.version>.*<\\/nd4j.version>/<nd4j.version>${RELEASE_VERSION}<\\/nd4j.version>/' pom.xml")
    sh ("sed -i 's/<datavec.version>.*<\\/datavec.version>/<datavec.version>${RELEASE_VERSION}<\\/datavec.version>/' pom.xml")
    // sh ("'${mvnHome}/bin/mvn' versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=${RELEASE_VERSION}")
    functions.verset("${RELEASE_VERSION}", true)

    configFileProvider([configFile(fileId: 'MAVEN_SETTINGS_DO-192', variable: 'MAVEN_SETTINGS')]) {
      sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean install")
      // sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests  ")
    }
  }
}

// Messages for debugging
echo 'MARK: end of gym-java-client.groovy'
