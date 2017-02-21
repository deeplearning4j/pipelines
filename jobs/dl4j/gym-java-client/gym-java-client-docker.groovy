stage("${GYM_JAVA_CLIENT_PROJECT}-checkout-sources") {
    functions.get_project_code("${GYM_JAVA_CLIENT_PROJECT}")
}

stage("${GYM_JAVA_CLIENT_PROJECT}-build") {
    echo "Building ${GYM_JAVA_CLIENT_PROJECT} version ${RELEASE_VERSION}"
    dir("${GYM_JAVA_CLIENT_PROJECT}") {
        functions.checktag("${GYM_JAVA_CLIENT_PROJECT}")
        functions.verset("${RELEASE_VERSION}", true)
        configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
            switch(PLATFORM_NAME) {
                case "linux-x86_64":
                    if (TESTS) {
                      docker.image(dockerImage).inside(dockerParams) {
                          sh'''
                          mvn -B -s ${MAVEN_SETTINGS} clean deploy \
                          -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION} \
                          -Dmaven.deploy.skip=false -Dlocal.software.repository=${PROFILE_TYPE}
                          '''
                      }
                    }
                    else {
                      docker.image(dockerImage).inside(dockerParams) {
                          sh'''
                          mvn -B -s ${MAVEN_SETTINGS} clean deploy -DskipTests \
                          -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION} \
                          -Dmaven.deploy.skip=false -Dlocal.software.repository=${PROFILE_TYPE}
                          '''
                      }
                    }
                break
                  case "linux-ppc64le":
                    if (TESTS) {
                      docker.image(dockerImage).inside(dockerParams) {
                          sh'''
                          mvn -B -s ${MAVEN_SETTINGS} clean install
                          '''
                      }
                    }
                    else {
                      docker.image(dockerImage).inside(dockerParams) {
                          sh'''
                          mvn -B -s ${MAVEN_SETTINGS} clean install -DskipTests
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
