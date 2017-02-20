stage("${SCALNET_PROJECT}-checkout-sources") {
    functions.get_project_code("${SCALNET_PROJECT}")
}

stage("${SCALNET_PROJECT}-build") {
  echo "Releasing ${SCALNET_PROJECT} version ${RELEASE_VERSION}"
  dir("${SCALNET_PROJECT}") {
    functions.checktag("${SCALNET_PROJECT}")
    sh  ("sed -i '0,/<artifactId>.*<\\/artifactId>/s//<artifactId>scalnet<\\/artifactId>/' pom.xml")
    functions.verset("${RELEASE_VERSION}", false)
    sh("sed -i '0,/<artifactId>.*<\\/artifactId>/s//<artifactId>scalnet_\${scala.binary.version}<\\/artifactId>/' pom.xml")
    configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
      switch(PLATFORM_NAME) {
        case "linux-x86_64":
          if (TESTS) {
            docker.image(dockerImage).inside(dockerParams) {
                sh'''
                mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dscalastyle.skip -DscalaVersion=${SCALA_VERSION} -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION} -Dmaven.deploy.skip=false -Dlocal.software.repository=${PROFILE_TYPE}
                '''
            }
          }
          else {
            docker.image(dockerImage).inside(dockerParams) {
                sh'''
                mvn -B -s ${MAVEN_SETTINGS} clean deploy -DskipTests -Dscalastyle.skip -DscalaVersion=${SCALA_VERSION} -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION} -Dmaven.deploy.skip=false -Dlocal.software.repository=${PROFILE_TYPE}
                '''
            }
          }

        break

        case "linux-ppc64le":
          if (TESTS) {
            docker.image(dockerImage).inside(dockerParams) {
                sh'''
                mvn -B -s ${MAVEN_SETTINGS} clean deploy
                '''
            }
          }
          else {
            docker.image(dockerImage).inside(dockerParams) {
                sh'''
                mvn -B -s ${MAVEN_SETTINGS} clean deploy -DskipTests
                '''
            }
          }

        break
        default:

        break
      }
    }
  }
}

echo 'MARK: end of scalnet.groovy'
