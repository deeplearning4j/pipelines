stage("${ARBITER_PROJECT}-checkout-sources") {
    functions.get_project_code("${ARBITER_PROJECT}")
}

// stage("${ARBITER_PROJECT}-Codecheck") {
//   functions.sonar("${ARBITER_PROJECT}")
// }

stage("${ARBITER_PROJECT}-build-${PLATFORM_NAME}") {
  echo "Releasing ${ARBITER_PROJECT} version ${RELEASE_VERSION}"
  dir("${ARBITER_PROJECT}") {
      functions.checktag("${ARBITER_PROJECT}")
      functions.verset("${RELEASE_VERSION}", true)
      sh "./change-scala-versions.sh ${SCALA_VERSION}"
      configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
        switch(PLATFORM_NAME) {
            case "linux-x86_64":
                if (TESTS) {
                  docker.image("${DOCKER_CENTOS6_CUDA80_AMD64}").inside(dockerParams) {
                      sh'''
                      mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dnd4j.version=${ND4J_VERSION} \
                      -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION}
                      '''
                  }
                }
                else {
                  docker.image("${DOCKER_CENTOS6_CUDA80_AMD64}").inside(dockerParams) {
                      sh'''
                      mvn -B -s ${MAVEN_SETTINGS} clean deploy -DskipTests -Dmaven.test.skip \
                      -Dnd4j.version=${ND4J_VERSION} \
                      -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION}
                      '''
                  }
                }
            break
              case "linux-ppc64le":
                if (TESTS) {
                  docker.image("${DOCKER_MAVEN_PPC}").inside(dockerParams_ppc) {
                      sh'''
                      ls -al /
                      ls -al /home
                      mvn -B -s ${MAVEN_SETTINGS} clean install
                      '''
                  }
                }
                else {
                  docker.image("${DOCKER_MAVEN_PPC}").inside(dockerParams_ppc) {
                      sh'''
                      ls -al /
                      ls -al /home
                      mvn -B -s ${MAVEN_SETTINGS} clean install -DskipTests -Dmaven.test.skip
                      '''
                  }
                }
            break
            default:
            break
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
