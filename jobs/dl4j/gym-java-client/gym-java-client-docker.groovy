stage("${GYM_JAVA_CLIENT_PROJECT}-CheckoutSources") {
    functions.get_project_code("${GYM_JAVA_CLIENT_PROJECT}")
}

stage("${GYM_JAVA_CLIENT_PROJECT}-Build-${PLATFORM_NAME}") {
    echo "Building ${GYM_JAVA_CLIENT_PROJECT} version ${RELEASE_VERSION}"
    dir("${GYM_JAVA_CLIENT_PROJECT}") {
        functions.checktag("${GYM_JAVA_CLIENT_PROJECT}")
        functions.verset("${RELEASE_VERSION}", true)
        configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
          if (!TESTS) {
            docker.image('ubuntu14cuda80').inside(dockerParams) {
                sh'''
                ${MVNCMD} -DskipTests -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION}
                '''
            }
          }
          else {
            docker.image('ubuntu14cuda80').inside(dockerParams) {
                sh'''
                ${MVNCMD} -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION}
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
