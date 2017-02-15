stage("${GYM_JAVA_CLIENT_PROJECT}-CheckoutSources") {
    functions.get_project_code("${GYM_JAVA_CLIENT_PROJECT}")
}

stage("${GYM_JAVA_CLIENT_PROJECT}-Build-${PLATFORM_NAME}") {
    echo "Building ${GYM_JAVA_CLIENT_PROJECT} version ${RELEASE_VERSION}"
    dir("${GYM_JAVA_CLIENT_PROJECT}") {
        functions.checktag("${GYM_JAVA_CLIENT_PROJECT}")
        functions.verset("${RELEASE_VERSION}", true)
        configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
            switch(PLATFORM_NAME) {
                case "linux-x86_64":
                    if (TESTS) {
                      docker.image("${DOCKER_CENTOS6_CUDA80_AMD64}").inside(dockerParams) {
                          sh'''
                          mvn -B -s ${MAVEN_SETTINGS} clean deploy \
                          -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION}
                          '''
                      }
                    }
                    else {
                      // !!! DO NOT FORGET TO CHANGE BACK TO DOCKER_CENTOS6_CUDA80_AMD64
                      docker.image("${DOCKER_UBUNTU14_CUDA80_AMD64}").inside(dockerParams) {
                          sh'''
                          env
                          ls -al /
                          ls -al /home
                          cat /etc/passwd
                          mvn -B -s ${MAVEN_SETTINGS} clean deploy -DskipTests \
                          -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION}
                          '''
                      }
                    }
                break
                  case "linux-ppc64le":
                    if (TESTS) {
                      docker.image("${DOCKER_MAVEN_PPC}").inside(dockerParams_ppc) {
                          sh'''
                          sudo mvn -B -s ${MAVEN_SETTINGS} clean install
                          '''
                      }
                    }
                    else {
                      docker.image("${DOCKER_MAVEN_PPC}").inside(dockerParams_ppc) {
                          sh'''
                          sudo mvn -B -s ${MAVEN_SETTINGS} clean install -DskipTests
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
        functions.sonar("${GYM_JAVA_CLIENT_PROJECT}")
    }
}

echo 'MARK: end of gym-java-client.groovy'
