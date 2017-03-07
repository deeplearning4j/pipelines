stage("${GYM_JAVA_CLIENT_PROJECT}-checkout-sources") {
    functions.get_project_code("${GYM_JAVA_CLIENT_PROJECT}")
}

stage("${GYM_JAVA_CLIENT_PROJECT}-build") {
    echo "Building ${GYM_JAVA_CLIENT_PROJECT} version ${VERSION}"
    dir("${GYM_JAVA_CLIENT_PROJECT}") {
        functions.checktag("${GYM_JAVA_CLIENT_PROJECT}")
        functions.verset("${VERSION}", true)
        configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
            switch(PLATFORM_NAME) {
                case "linux-x86_64":
                    if (TESTS.toBoolean()) {
                      docker.image(dockerImage).inside(dockerParams) {
                          functions.getGpg()
                          sh'''
                          mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID} -DperformRelease=${GpgVAR}
                          '''
                      }
                    }
                    else {
                      docker.image(dockerImage).inside(dockerParams) {
                          functions.getGpg()
                          sh'''
                          mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID} -DperformRelease=${GpgVAR} -Dmaven.test.skip=true 
                          '''
                      }
                    }
                break
                  case "linux-ppc64le":
                      if (TESTS.toBoolean()) {
                        docker.image(dockerImage).inside(dockerParams) {
                            functions.getGpg()
                            sh'''
                            mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID} -DperformRelease=${GpgVAR} 
                            '''
                        }
                      }
                      else {
                        docker.image(dockerImage).inside(dockerParams) {
                            functions.getGpg()
                            sh'''
                            mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID} -DperformRelease=${GpgVAR} -Dmaven.test.skip=true 
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
        functions.sonar("${GYM_JAVA_CLIENT_PROJECT}")
    }
}

echo 'MARK: end of gym-java-client.groovy'
