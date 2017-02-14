stage("${GYM_JAVA_CLIENT_PROJECT}-CheckoutSources") {
    functions.get_project_code("${GYM_JAVA_CLIENT_PROJECT}")
}

stage("${GYM_JAVA_CLIENT_PROJECT}-Build-${PLATFORM_NAME}") {
    echo "Releasing ${GYM_JAVA_CLIENT_PROJECT} version ${RELEASE_VERSION}"
    dir("${GYM_JAVA_CLIENT_PROJECT}") {
        functions.checktag("${GYM_JAVA_CLIENT_PROJECT}")
        functions.verset("${RELEASE_VERSION}", true)
        configFileProvider([configFile(fileId: 'MAVEN_SETTINGS_DO-192', variable: 'MAVEN_SETTINGS')]) {
          if (!TESTS) {
            docker.image('ubuntu14cuda80').inside(dockerParams) {
                sh'''
                mvn -B -s ${MAVEN_SETTINGS} clean deploy -DskipTests -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION}
                '''
            }
          }
          else {
            docker.image('ubuntu14cuda80').inside(dockerParams) {
                sh'''
                mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION}
                '''
            }
          }
        }
    }
    if (SONAR) {
        functions.sonar("${GYM_JAVA_CLIENT_PROJECT}")
    }
}

echo 'MARK: end of gym-java-client.groovy'
