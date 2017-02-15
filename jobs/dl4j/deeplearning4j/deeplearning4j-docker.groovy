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

configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
  switch(PLATFORM_NAME) {
      case "linux-x86_64":
          if (!TESTS) {
            docker.image("${DOCKER_CENTOS6_CUDA80_AMD64}").inside(dockerParams) {
              stage("${DEEPLEARNING4J_PROJECT} Build test resources"){
                  sh'''
                  cd dl4j-test-resources
                  mvn clean install
                  '''
              }
              stage("${DEEPLEARNING4J_PROJECT}-Build-${PLATFORM_NAME}") {
                echo "Building ${DEEPLEARNING4J_PROJECT} version ${RELEASE_VERSION}"
                dir("${DEEPLEARNING4J_PROJECT}") {

                  functions.verset("${RELEASE_VERSION}", true)

                  sh'''
                  ./change-scala-versions.sh ${SCALA_VERSION}
                  ./change-cuda-versions.sh ${CUDA_VERSION}
                  mvn -B -s ${MAVEN_SETTINGS} clean deploy -DskipTests -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION}
                  '''
                }
              }
            }
          }
          else {
            docker.image("${DOCKER_CENTOS6_CUDA80_AMD64}").inside(dockerParams) {
              stage("${DEEPLEARNING4J_PROJECT} Build test resources"){
                  sh'''
                  cd dl4j-test-resources
                  mvn -q clean install
                  '''
              }
              stage("${DEEPLEARNING4J_PROJECT}-Build-${PLATFORM_NAME}") {
                echo "Building ${DEEPLEARNING4J_PROJECT} version ${RELEASE_VERSION}"
                dir("${DEEPLEARNING4J_PROJECT}") {

                  functions.verset("${RELEASE_VERSION}", true)

                  sh'''
                  ./change-scala-versions.sh ${SCALA_VERSION}
                  ./change-cuda-versions.sh ${CUDA_VERSION}
                  mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION}
                  '''
                }
              }
            }
          }
      break
        case "linux-ppc64le":
          if (!TESTS) {
            docker.image("${DOCKER_MAVEN_PPC}").inside(dockerParams_ppc) {
              stage("${DEEPLEARNING4J_PROJECT} Build test resources"){
                  sh'''
                  cd dl4j-test-resources
                  sudo mvn clean install
                  '''
              }
              stage("${DEEPLEARNING4J_PROJECT}-Build-${PLATFORM_NAME}") {
                echo "Building ${DEEPLEARNING4J_PROJECT} version ${RELEASE_VERSION}"
                dir("${DEEPLEARNING4J_PROJECT}") {

                  functions.verset("${RELEASE_VERSION}", true)

                  sh'''
                  ./change-scala-versions.sh ${SCALA_VERSION}
                  ./change-cuda-versions.sh ${CUDA_VERSION}
                  sudo mvn -B -s ${MAVEN_SETTINGS} clean deploy -DskipTests -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION}
                  '''
                }
              }
            }
          }
          else {
            docker.image("${DOCKER_MAVEN_PPC}").inside(dockerParams_ppc) {
              stage("${DEEPLEARNING4J_PROJECT} Build test resources"){
                  sh'''
                  cd dl4j-test-resources
                  sudo mvn -q clean install
                  '''
              }
              stage("${DEEPLEARNING4J_PROJECT}-Build-${PLATFORM_NAME}") {
                echo "Building ${DEEPLEARNING4J_PROJECT} version ${RELEASE_VERSION}"
                dir("${DEEPLEARNING4J_PROJECT}") {

                  functions.verset("${RELEASE_VERSION}", true)

                  sh'''
                  ./change-scala-versions.sh ${SCALA_VERSION}
                  ./change-cuda-versions.sh ${CUDA_VERSION}
                  sudo mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION}
                  '''
                }
              }
            }
          }
      break

      default:
      break

  }

  if (SONAR) {
      functions.sonar("${DEEPLEARNING4J_PROJECT}")
  }

echo 'MARK: end of deeplearning4j.groovy'
