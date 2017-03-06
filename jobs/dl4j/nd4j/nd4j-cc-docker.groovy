stage("${PROJECT}-checkout-sources") {
    functions.get_project_code("${PROJECT}")
}

stage("${PROJECT}-build") {
    dir("${LIBPROJECT}/blasbuild") {
        sh("ln -s cuda-${CUDA_VERSION} cuda")
    }

    dir("${PROJECT}") {
        // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
        // Temporary section - please remove once it commited updates to source code
        // configFileProvider(
        //         [configFile(fileId: 'MAVEN_POM_DO-192', variable: 'POM_XML')
        //     ]) {
        //     sh "cp ${POM_XML} pom.xml"
        // }
        // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
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
                          #mvn -B -s ${MAVEN_SETTINGS} clean deploy
                          mvn clean deploy -Dlocal.software.repository=${PROFILE_TYPE}
                          '''
                      }
                    }
                    else {
                      docker.image(dockerImage).inside(dockerParams) {
                          sh'''
                          if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                          #mvn -B -s ${MAVEN_SETTINGS} clean deploy -DskipTests
                          mvn clean deploy -Dlocal.software.repository=${PROFILE_TYPE} -Dmaven.test.skip=true
                          '''
                      }
                    }
                    break

                case "linux-ppc64le":
                    if (TESTS.toBoolean()) {
                      docker.image(dockerImage).inside(dockerParams) {
                          sh'''
                          if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                          #mvn -B -s ${MAVEN_SETTINGS} clean deploy
                          mvn clean install -Dlocal.software.repository=${PROFILE_TYPE}
                          '''
                      }
                    }
                    else {
                      docker.image(dockerImage).inside(dockerParams) {
                          sh'''
                          if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                          #mvn -B -s ${MAVEN_SETTINGS} clean deploy -DskipTests
                          mvn clean install -Dlocal.software.repository=${PROFILE_TYPE} -Dmaven.test.skip=true
                          '''
                      }
                    }
                    break

                case ["android-arm", "android-x86"]:
                    if (TESTS.toBoolean()) {
                      docker.image(dockerImage).inside(dockerParams) {
                          sh'''
                          if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                          mvn clean install -Djavacpp.platform=${PLATFORM_NAME} -DskipTests -pl '!:nd4j-cuda-8.0,!:nd4j-cuda-8.0-platform'
                          '''
                      }
                    }
                    else {
                      docker.image(dockerImage).inside(dockerParams) {
                          sh'''
                          if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                          mvn clean install -Djavacpp.platform=${PLATFORM_NAME} -DskipTests -pl '!:nd4j-cuda-8.0,!:nd4j-cuda-8.0-platform'
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
