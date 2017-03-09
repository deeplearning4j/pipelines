stage("${PROJECT}-checkout-sources") {
    functions.get_project_code("${PROJECT}")
}

stage("${PROJECT}-build") {
    // dir("${LIBPROJECT}/blasbuild") {
    //     sh("ln -s cuda-${CUDA_VERSION} cuda")
    // }

    dir("${PROJECT}") {
        echo 'Set Project Version'
        //// sh("'mvn' versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=${VERSION}")
        // functions.verset("${VERSION}", true)

        // sh "./change-scala-versions.sh ${SCALA_VERSION}"
        // sh "./change-cuda-versions.sh ${CUDA_VERSION}"

        configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
            switch(PLATFORM_NAME) {
                case "linux-x86_64":
                    if (TESTS.toBoolean()) {
                      docker.image(dockerImage).inside(dockerParams) {
                          sh'''
                          if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                          mvn -B -s ${MAVEN_SETTINGS} clean deploy
                          '''
                      }
                    }
                    else {
                      docker.image(dockerImage).inside(dockerParams) {
                          sh'''
                          if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                          mvn -B -s ${MAVEN_SETTINGS} clean deploy -DskipTests
                          '''
                      }
                    }
                    break

                case "linux-ppc64le":
                    if (TESTS.toBoolean()) {
                      docker.image(dockerImage).inside(dockerParams) {
                          sh'''
                          if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                          mvn -B -s ${MAVEN_SETTINGS} clean deploy
                          '''
                      }
                    }
                    else {
                      docker.image(dockerImage).inside(dockerParams) {
                          sh'''
                          if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                          mvn -B -s ${MAVEN_SETTINGS} clean deploy -DskipTests
                          '''
                      }
                    }
                    break

                case ["android-arm", "android-x86"]:
                    if (TESTS.toBoolean()) {
                      docker.image(dockerImage).inside(dockerParams) {
                          sh'''
                          if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                          mvn -B -s ${MAVEN_SETTINGS} clean deploy -Djavacpp.platform=${PLATFORM_NAME} -Dlocal.software.repository=${PROFILE_TYPE} -DperformRelease=${GpgVAR} -DstagingRepositoryId=${STAGE_REPO_ID} -Dmaven.test.skip=false -pl '!:nd4j-cuda-8.0,!:nd4j-cuda-8.0-platform'
                          '''
                      }
                    }
                    else {
                      docker.image(dockerImage).inside(dockerParams) {
                          sh'''
                          if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                          mvn -B -s ${MAVEN_SETTINGS} clean deploy -Djavacpp.platform=${PLATFORM_NAME} -Dlocal.software.repository=${PROFILE_TYPE} -DperformRelease=${GpgVAR} -DstagingRepositoryId=${STAGE_REPO_ID} -Dmaven.test.skip=true -pl '!:nd4j-cuda-8.0,!:nd4j-cuda-8.0-platform'
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
           functions.sonar("${PROJECT}")
    }
}

echo 'MARK: end of nd4j.groovy'
