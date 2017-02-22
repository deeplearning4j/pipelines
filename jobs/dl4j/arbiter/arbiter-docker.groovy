stage("${ARBITER_PROJECT}-checkout-sources") {
    functions.get_code("${ARBITER_PROJECT}")
    // functions.get_project_code("${ARBITER_PROJECT}")
}

// stage("${ARBITER_PROJECT}-Codecheck") {
//   functions.sonar("${ARBITER_PROJECT}")
// }

stage("${ARBITER_PROJECT}-build") {
  echo "Releasing ${ARBITER_PROJECT} version ${RELEASE_VERSION}"
  dir("${ARBITER_PROJECT}") {

      functions.checktag("${ARBITER_PROJECT}")

      functions.verset("${RELEASE_VERSION}", true)
      sh "./change-scala-versions.sh ${SCALA_VERSION}"

      echo("${TESTS}")

      configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
        switch(PLATFORM_NAME) {
            case "linux-x86_64":
                if (TESTS.toBoolean()) {
                  docker.image(dockerImage).inside(dockerParams) {
                      sh'''
                      mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dnd4j.version=${ND4J_VERSION} \
                      -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION} -Dmaven.deploy.skip=false -Dlocal.software.repository=${PROFILE_TYPE}
                      '''
                  }
                }
                else {
                  docker.image(dockerImage).inside(dockerParams) {
                      sh'''
                      mvn -B -s ${MAVEN_SETTINGS} clean deploy -DskipTests -Dmaven.test.skip \
                      -Dnd4j.version=${ND4J_VERSION} \
                      -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION} -Dmaven.deploy.skip=false -Dlocal.software.repository=${PROFILE_TYPE}
                      '''
                  }
                }
            break
              case "linux-ppc64le":
                if (TESTS.toBoolean()) {
                  docker.image(dockerImage).inside(dockerParams) {
                      sh'''
                      ls -al /
                      ls -al /home
                      mvn -B -s ${MAVEN_SETTINGS} clean install
                      '''
                  }
                }
                else {
                  docker.image(dockerImage).inside(dockerParams) {
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
  if (SONAR.toBoolean()) {
      functions.sonar("${ARBITER_PROJECT}")
  }
}

// if (SONAR.toBoolean()) {
//   stage("${GYM_JAVA_CLIENT_PROJECT}-Codecheck") {
//     functions.sonar("${GYM_JAVA_CLIENT_PROJECT}")
//   }
// }
echo 'MARK: end of arbiter.groovy'
