stage("${RL4J_PROJECT}-checkout-sources") {
    functions.get_project_code("${RL4J_PROJECT}")
}

stage("${RL4J_PROJECT}-build-${PLATFORM_NAME}") {
  echo "Building ${RL4J_PROJECT} version ${RELEASE_VERSION}"
  dir("${RL4J_PROJECT}") {
    functions.checktag("${RL4J_PROJECT}")
    functions.verset("${RELEASE_VERSION}", true)
    configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
      switch(PLATFORM_NAME) {
        case "linux-x86_64":
          if (TESTS) {
            docker.image("${DOCKER_CENTOS6_CUDA80_AMD64}").inside(dockerParams) {
                // sh'''
                // mvn -X -s ${MAVEN_SETTINGS} clean deploy -DskipTests -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION}
                // '''
                sh'''
                mvn -B -s ${MAVEN_SETTINGS} clean deploy
                '''
            }
          }
          else {
            docker.image("${DOCKER_CENTOS6_CUDA80_AMD64}").inside(dockerParams) {
                // sh'''
                // mvn -X -s ${MAVEN_SETTINGS} clean deploy -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION}
                // '''
                sh'''
                mvn -B -s ${MAVEN_SETTINGS} clean deploy -DskipTests
                '''
            }
          }

        break

        case "linux-ppc64le":
          if (TESTS) {
            docker.image("${DOCKER_MAVEN_PPC}").inside(dockerParams_ppc) {
                // sh'''
                // mvn -X -s ${MAVEN_SETTINGS} clean deploy -DskipTests -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION}
                // '''
                sh'''
                mvn -B -s ${MAVEN_SETTINGS} clean deploy
                '''
            }
          }
          else {
            docker.image("${DOCKER_MAVEN_PPC}").inside(dockerParams_ppc) {
                // sh'''
                // mvn -X -s ${MAVEN_SETTINGS} clean deploy -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION}
                // '''
                sh'''
                mvn -B -s ${MAVEN_SETTINGS} clean deploy -DskipTests
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
    functions.sonar("${RL4J_PROJECT}")
  }
}

echo 'MARK: end of rl4j.groovy'
