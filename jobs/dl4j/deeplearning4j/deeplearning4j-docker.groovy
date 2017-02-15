stage("${DEEPLEARNING4J_PROJECT}-CheckoutSources") {

  functions.get_project_code("${DEEPLEARNING4J_PROJECT}")
  dir("${DEEPLEARNING4J_PROJECT}") {
      functions.checktag("${DEEPLEARNING4J_PROJECT}")
  }

  checkout([$class                           : 'GitSCM',
            branches                         : [[name: '*/master']],
            doGenerateSubmoduleConfigurations: false,
            extensions                       : [[$class: 'RelativeTargetDirectory', relativeTargetDir: "dl4j-test-resources"], [$class: 'CloneOption', honorRefspec: true, noTags: true, reference: '', shallow: true]],
            submoduleCfg                     : [],
            userRemoteConfigs                : [[url: "https://github.com/${ACCOUNT}/dl4j-test-resources.git"]]
  ])
}

stage("Build test resources on ${PLATFORM_NAME}") {
  configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
    switch(PLATFORM_NAME) {
      case "linux-x86_64":
        dir('dl4j-test-resources') {
          docker.image("${DOCKER_CENTOS6_CUDA80_AMD64}").inside(dockerParams) {
            sh'''
            mvn -q clean install
            '''
          }
        }
      break

      case "linux-ppc64le":
        dir('dl4j-test-resources') {
          docker.image("${DOCKER_MAVEN_PPC}").inside(dockerParams_ppc) {
            sh'''
            mvn -q clean install
            '''
          }
        }
      break

      default:
      break
    }
  }
}

stage("${DEEPLEARNING4J_PROJECT}-Build-${PLATFORM_NAME}") {
  configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
    switch(PLATFORM_NAME) {
        case "linux-x86_64":
            if (!TESTS) {
              dir("${DEEPLEARNING4J_PROJECT}") {
                docker.image("${DOCKER_CENTOS6_CUDA80_AMD64}").inside(dockerParams) {
                  echo "Building ${DEEPLEARNING4J_PROJECT} version ${RELEASE_VERSION}"
                  functions.verset("${RELEASE_VERSION}", true)

                  sh'''
                  ./change-scala-versions.sh ${SCALA_VERSION}
                  ./change-cuda-versions.sh ${CUDA_VERSION}
                  mvn -B -s ${MAVEN_SETTINGS} clean deploy -DskipTests -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION}
                  '''
                }
              }
            }
            else {
              dir("${DEEPLEARNING4J_PROJECT}") {
                docker.image("${DOCKER_CENTOS6_CUDA80_AMD64}").inside(dockerParams) {
                  echo "Building ${DEEPLEARNING4J_PROJECT} version ${RELEASE_VERSION}"
                  functions.verset("${RELEASE_VERSION}", true)

                  sh'''
                  ./change-scala-versions.sh ${SCALA_VERSION}
                  ./change-cuda-versions.sh ${CUDA_VERSION}
                  mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION}
                  '''
                }
              }
            }
        break
          case "linux-ppc64le":
            if (!TESTS) {
              dir("${DEEPLEARNING4J_PROJECT}") {
                docker.image("${DOCKER_MAVEN_PPC}").inside(dockerParams_ppc) {
                  echo "Building ${DEEPLEARNING4J_PROJECT} version ${RELEASE_VERSION}"
                  functions.verset("${RELEASE_VERSION}", true)

                  sh'''
                  ./change-scala-versions.sh ${SCALA_VERSION}
                  ./change-cuda-versions.sh ${CUDA_VERSION}
                  mvn -B -s ${MAVEN_SETTINGS} clean install -DskipTests
                  '''
                }
              }
            }
            else {
              dir("${DEEPLEARNING4J_PROJECT}") {
                docker.image("${DOCKER_MAVEN_PPC}").inside(dockerParams_ppc) {
                  echo "Building ${DEEPLEARNING4J_PROJECT} version ${RELEASE_VERSION}"
                  functions.verset("${RELEASE_VERSION}", true)

                  sh'''
                  ./change-scala-versions.sh ${SCALA_VERSION}
                  ./change-cuda-versions.sh ${CUDA_VERSION}
                  sudo mvn -B -s ${MAVEN_SETTINGS} clean install
                  '''
                }
              }
            }
        break

        default:
        break

    }
  }

  if (SONAR) {
      functions.sonar("${DEEPLEARNING4J_PROJECT}")
  }
}
echo 'MARK: end of deeplearning4j.groovy'
