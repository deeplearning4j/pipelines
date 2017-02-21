stage("${PROJECT}-checkout-sources") {
    functions.get_code("${PROJECT}")
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
        // sh("'mvn' versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=${RELEASE_VERSION}")
        functions.verset("${RELEASE_VERSION}", true)

        sh "./change-scala-versions.sh ${SCALA_VERSION}"
        sh "./change-cuda-versions.sh ${CUDA_VERSION}"

        configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
            switch(PLATFORM_NAME) {
                case "linux-x86_64":
                    if (TESTS) {
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
           functions.sonar("${PROJECT}")
    }
}

// Messages for debugging
echo 'MARK: end of nd4j.groovy'
