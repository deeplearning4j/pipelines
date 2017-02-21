stage("${ND4S_PROJECT}-checkout-sources") {
    functions.get_code("${ND4S_PROJECT}")
}

stage("${ND4S_PROJECT}-build") {
    echo "Releasing ${ND4S_PROJECT} version ${RELEASE_VERSION}"
    dir("${ND4S_PROJECT}") {
        functions.checktag("${ND4S_PROJECT}")
//        sh ("sed -i 's/version := \".*\",/version := \"${RELEASE_VERSION}\",/' build.sbt")
//        sh ("sed -i 's/nd4jVersion := \".*\",/nd4jVersion := \"${ND4J_VERSION}\",/' build.sbt")
        sh ("test -d ${WORKSPACE}/.ivy2 || mkdir ${WORKSPACE}/.ivy2")
        configFileProvider([configFile(fileId: "sbt-local-nexus-id-1", variable: 'SBT_CREDENTIALS')]) {
              sh ("cp ${SBT_CREDENTIALS}  ${WORKSPACE}/.ivy2/.nexus")}
        configFileProvider([configFile(fileId: "sbt-local-jfrog-id-1", variable: 'SBT_CREDENTIALS')]) {
            sh ("cp ${SBT_CREDENTIALS}  ${WORKSPACE}/.ivy2/.jfrog")}
        configFileProvider([configFile(fileId: "sbt-oss-sonatype-id-1", variable: 'SBT_CREDENTIALS')]) {
            sh ("cp ${SBT_CREDENTIALS}  ${WORKSPACE}/.ivy2/.sonatype")}
        configFileProvider([configFile(fileId: "sbt-oss-bintray-id-1", variable: 'SBT_CREDENTIALS')]) {
            sh ("cp ${SBT_CREDENTIALS}  ${WORKSPACE}/.ivy2/.bintray")}


        switch(PLATFORM_NAME) {
          case "linux-x86_64":
            docker.image(dockerImage).inside(dockerParams) {
              sh'''
              cp -a ${WORKSPACE}/.ivy2 ${HOME}/  
              cp ${HOME}/.ivy2/.${PROFILE_TYPE} ${HOME}/.ivy2/.credentials
              sbt -DrepoType=${PROFILE_TYPE} -DcurrentVersion=${RELEASE_VERSION}  publish
              find ${WORKSPACE}/.ivy2 ${HOME}/.ivy2  -type f -name  ".credentials"  -delete -o -name ".nexus"  -delete -o -name ".jfrog" -delete -o -name ".sonatype" -delete -o -name ".bintray" -delete;
              '''
            }
            break

          case "linux-ppc64le":
            docker.image(dockerImage).inside(dockerParams) {
              sh'''            
              cp -a ${WORKSPACE}/.ivy2 ${HOME}/
              sbt +publish
              rm -f ${HOME}/.ivy2/*.* ${WORKSPACE}/.ivy2/*.*
              find ${WORKSPACE}/.ivy2 ${HOME}/.ivy2  -type f -name  ".credentials" -o -name ".nexus" -o -name ".jfrog" -o -name ".sonatype" -o -name ".bintray" ;
              '''
            }
            break

          default:
            break
        }
    }
}
echo 'MARK: end of nd4s.groovy'
