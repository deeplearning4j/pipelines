tool name: 'M339', type: 'maven'
def mvnHome = tool 'M339'

env.JAVA_HOME = "${tool 'jdk-8u121'}"
env.PATH = "${env.JAVA_HOME}/bin:${env.PATH}"

stage("${ARBITER_PROJECT}-CheckoutSources") {
    functions.get_project_code("${ARBITER_PROJECT}")
}

// stage("${ARBITER_PROJECT}-Codecheck") {
//   functions.sonar("${ARBITER_PROJECT}")
// }

stage("${ARBITER_PROJECT}-Build") {

  echo "Releasing ${ARBITER_PROJECT} version ${RELEASE_VERSION}"


  dir("${ARBITER_PROJECT}") {
    functions.checktag("${ARBITER_PROJECT}")
/*
    sh ("sed -i 's/<nd4j.version>.*<\\/nd4j.version>/<nd4j.version>${RELEASE_VERSION}<\\/nd4j.version>/' pom.xml")
    sh ("sed -i 's/<datavec.version>.*<\\/datavec.version>/<datavec.version>${RELEASE_VERSION}<\\/datavec.version>/' pom.xml")
    sh ("sed -i 's/<dl4j.version>.*<\\/dl4j.version>/<dl4j.version>${RELEASE_VERSION}<\\/dl4j.version>/' pom.xml")
*/
    // sh ("'${mvnHome}/bin/mvn' versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=${RELEASE_VERSION}")
    functions.verset("${RELEASE_VERSION}", true)
    sh "./change-scala-versions.sh ${SCALA_VERSION}"
    configFileProvider([configFile(fileId: 'MAVEN_SETTINGS_DO-192', variable: 'MAVEN_SETTINGS')]) {
      sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests -Dmaven.test.skip -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION}")
    }

    /*sh "./change-scala-versions.sh 2.11"
    configFileProvider([configFile(fileId: 'MAVEN_SETTINGS_DO-192', variable: 'MAVEN_SETTINGS')]) {
      sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests -Dmaven.test.skip -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION}")
    }*/
  }
}

// Messages for debugging
echo 'MARK: end of arbiter.groovy'


