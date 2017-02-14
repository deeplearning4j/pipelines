stage("${RL4J_PROJECT}-CheckoutSources") {
    functions.get_project_code("${RL4J_PROJECT}")
}

stage("${RL4J_PROJECT}-Build-withDocker") {
  echo "Releasing ${RL4J_PROJECT} version ${RELEASE_VERSION}"
  dir("${RL4J_PROJECT}") {
    functions.checktag("${RL4J_PROJECT}")
    functions.verset("${RELEASE_VERSION}", true)
    configFileProvider([configFile(fileId: 'MAVEN_SETTINGS_DO-192', variable: 'MAVEN_SETTINGS')]) {
      if (!TESTS) {
        docker.image('ubuntu14cuda80').inside(dockerParams) {
            // sh'''
            // mvn -X -s ${MAVEN_SETTINGS} clean deploy -DskipTests -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION}
            // '''
            sh'''
            mvn -B -s ${MAVEN_SETTINGS} clean deploy -DskipTests
            '''
        }
      }
      else {
        docker.image('ubuntu14cuda80').inside(dockerParams) {
            // sh'''
            // mvn -X -s ${MAVEN_SETTINGS} clean deploy -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION}
            // '''
            sh'''
            mvn -B -s ${MAVEN_SETTINGS} clean deploy
            '''
        }
      }
    }
  }
  if (SONAR) {
    functions.sonar("${RL4J_PROJECT}")
  }
}

echo 'MARK: end of rl4j.groovy'
