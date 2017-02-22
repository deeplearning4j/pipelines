stage("${RL4J_PROJECT}-checkout-sources") {
    functions.get_code("${RL4J_PROJECT}")
}

stage("${RL4J_PROJECT}-build") {
  echo "Building ${RL4J_PROJECT} version ${RELEASE_VERSION}"
  dir("${RL4J_PROJECT}") {
    functions.checktag("${RL4J_PROJECT}")
    functions.verset("${RELEASE_VERSION}", true)
    configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
      switch(PLATFORM_NAME) {
        case "linux-x86_64":
          if (TESTS.toBoolean()) {
            docker.image(dockerImage).inside(dockerParams) {
                // sh'''
                // mvn -X -s ${MAVEN_SETTINGS} clean deploy -DskipTests -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION}
                // '''
                sh'''
                mvn -B -s ${MAVEN_SETTINGS} clean deploy   -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION} -Dmaven.deploy.skip=false -Dlocal.software.repository=${PROFILE_TYPE}
                '''
            }
          }
          else {
            docker.image(dockerImage).inside(dockerParams) {
                // sh'''
                // mvn -X -s ${MAVEN_SETTINGS} clean deploy -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION}
                // '''
                sh'''
                mvn -B -s ${MAVEN_SETTINGS} clean deploy -DskipTests -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION} -Dmaven.deploy.skip=false -Dlocal.software.repository=${PROFILE_TYPE}
                '''
            }
          }

        break

        case "linux-ppc64le":
          if (TESTS.toBoolean()) {
            docker.image(dockerImage).inside(dockerParams) {
                // sh'''
                // mvn -X -s ${MAVEN_SETTINGS} clean deploy -DskipTests -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION}
                // '''
                sh'''
                mvn -B -s ${MAVEN_SETTINGS} clean deploy
                '''
            }
          }
          else {
            docker.image(dockerImage).inside(dockerParams) {
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
  if (SONAR.toBoolean()) {
    functions.sonar("${RL4J_PROJECT}")
  }
}

echo 'MARK: end of rl4j.groovy'
