tool name: 'M339', type: 'maven'
def mvnHome = tool 'M339'

functions = load 'jobs/dl4j/functions.groovy'

stage("${RL4J_PROJECT}-CheckoutSources") {
  functions.get_project_code("${RL4J_PROJECT}")
}

// stage("${RL4J_PROJECT}-Codecheck") {
//   functions.sonar("${RL4J_PROJECT}")
// }

stage("${RL4J_PROJECT}-build") {

  echo "Releasing ${RL4J_PROJECT} version ${RELEASE_VERSION}"

  dir("${RL4J_PROJECT}") {
    functions.checktag("${RL4J_PROJECT}")

    sh ("sed -i 's/<nd4j.version>.*<\\/nd4j.version>/<nd4j.version>${RELEASE_VERSION}<\\/nd4j.version>/' pom.xml")
    sh ("sed -i 's/<datavec.version>.*<\\/datavec.version>/<datavec.version>${RELEASE_VERSION}<\\/datavec.version>/' pom.xml")
    sh ("sed -i 's/<dl4j.version>.*<\\/dl4j.version>/<dl4j.version>${RELEASE_VERSION}<\\/dl4j.version>/' pom.xml")
    // sh ("'${mvnHome}/bin/mvn' versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=${RELEASE_VERSION}")
    functions.verset("${RELEASE_VERSION}", true)
    configFileProvider([configFile(fileId: 'MAVEN_SETTINGS_DO-192', variable: 'MAVEN_SETTINGS')]) {
      sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests ")
    }
  }
}

// Messages for debugging
echo 'MARK: end of rl4j.groovy'
