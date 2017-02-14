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
      configFileProvider([configFile(fileId: "${SETTINGS_XML}", variable: 'MAVEN_SETTINGS')]) {
        if (!TESTS) {
          docker.image("${DOCKER_IMAGE}").inside("${DOCKER_PARAMETERS}") {
            if("${PLATFORM_NAME}" == 'linux-ppc64le') {
              sh'''
              sudo mvn -B -s ${MAVEN_SETTINGS} clean deploy -DskipTests -Dmaven.test.skip -Dnd4j.version=${ND4J_VERSION} \
              -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION}
              '''
            }
            else {
              sh'''
              mvn -B -s ${MAVEN_SETTINGS} clean deploy -DskipTests -Dmaven.test.skip -Dnd4j.version=${ND4J_VERSION} \
              -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION}
              '''
            }
          }
        }
        else {
          docker.image("${DOCKER_IMAGE}").inside("${DOCKER_PARAMETERS}") {
            if("${PLATFORM_NAME}" == 'linux-ppc64le') {
              sh'''
              sudo mvn -B -s ${MAVEN_SETTINGS} clean deploy -DskipTests -Dmaven.test.skip -Dnd4j.version=${ND4J_VERSION} \
              -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION}
              '''
            }
            else {
              sh'''
              mvn -B -s ${MAVEN_SETTINGS} clean deploy -DskipTests -Dmaven.test.skip -Dnd4j.version=${ND4J_VERSION} \
              -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION}
              '''
            }
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
