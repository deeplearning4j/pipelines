stage("${ND4S_PROJECT}-CheckoutSources") {
    functions.get_project_code("${ND4S_PROJECT}")
}

stage("${ND4S_PROJECT}-Build-${PLATFORM_NAME}") {
    echo "Releasing ${ND4S_PROJECT} version ${RELEASE_VERSION}"
    dir("${ND4S_PROJECT}") {
        functions.checktag("${ND4S_PROJECT}")
        sh ("sed -i 's/version := \".*\",/version := \"${RELEASE_VERSION}\",/' build.sbt")
        sh ("sed -i 's/nd4jVersion := \".*\",/nd4jVersion := \"${ND4J_VERSION}\",/' build.sbt")
        configFileProvider(
                [configFile(fileId: "${SBTCREDID}", variable: 'SBT_CREDENTIALS')
                ]) {
              sh ("test -d ${WORKSPACE}/.ivy2 || mkdir ${WORKSPACE}/.ivy2")
              sh ("cp ${SBT_CREDENTIALS}  ${WORKSPACE}/.ivy2/.credentials")
        }

        switch(PLATFORM_NAME) {
          case "linux-x86_64":
            docker.image("${DOCKER_CENTOS6_CUDA80_AMD64}").inside(dockerParams) {
              sh'''
              env
              ls -al /
              ls -al /home
              cat /etc/passwd
              cp -a ${WORKSPACE}/.ivy2 ${HOME}/
              sbt +publish
              rm -f ${HOME}/.ivy2/.credentials ${WORKSPACE}/.ivy2/.credentials
              '''
            }
            break

          case "linux-ppc64le":
            docker.image("${DOCKER_MAVEN_PPC}").inside(dockerParams_ppc) {
              sh'''
              cp -a ${WORKSPACE}/.ivy2 ${HOME}/
              sbt +publish
              rm -f ${HOME}/.ivy2/.credentials ${WORKSPACE}/.ivy2/.credentials
              '''
            }
            break

          default:
            break
        }
    }
}
echo 'MARK: end of nd4s.groovy'
