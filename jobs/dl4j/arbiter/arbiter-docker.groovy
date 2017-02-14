stage("${ARBITER_PROJECT}-CheckoutSources") {
    functions.get_project_code("${ARBITER_PROJECT}")
}

// stage("${ARBITER_PROJECT}-Codecheck") {
//   functions.sonar("${ARBITER_PROJECT}")
// }

stage("${ARBITER_PROJECT}-Build-${PLATFORM_NAME}") {
  echo "Releasing ${ARBITER_PROJECT} version ${RELEASE_VERSION}"
  dir("${ARBITER_PROJECT}") {
      functions.checktag("${ARBITER_PROJECT}")
      functions.verset("${RELEASE_VERSION}", true)
      sh "./change-scala-versions.sh ${SCALA_VERSION}"
      configFileProvider([configFile(fileId: 'MAVEN_SETTINGS_DO-192', variable: 'MAVEN_SETTINGS')]) {
        if (!TESTS) {
          docker.image('ubuntu14cuda80').inside(dockerParams) {
              sh'''
              mvn -B -s ${MAVEN_SETTINGS} clean deploy -DskipTests -Dmaven.test.skip -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION}
              '''
          }
        }
        else {
          docker.image('ubuntu14cuda80').inside(dockerParams) {
              sh'''
              mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION}
              '''
          }
        }
      }
    }
  if (SONAR) {
      functions.sonar("${ARBITER_PROJECT}")
  }
}

// if (SONAR) {
//   stage("${GYM_JAVA_CLIENT_PROJECT}-Codecheck") {
//     functions.sonar("${GYM_JAVA_CLIENT_PROJECT}")
//   }
// }
echo 'MARK: end of arbiter.groovy'
