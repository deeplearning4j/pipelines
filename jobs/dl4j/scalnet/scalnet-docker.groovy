stage("${SCALNET_PROJECT}-CheckoutSources") {
    functions.get_project_code("${SCALNET_PROJECT}")
}

stage("${SCALNET_PROJECT}-Build-${PLATFORM_NAME}") {
    echo "Releasing ${SCALNET_PROJECT} version ${RELEASE_VERSION}"
    dir("${SCALNET_PROJECT}") {
        functions.checktag("${SCALNET_PROJECT}")
        sh  ("sed -i '0,/<artifactId>.*<\\/artifactId>/s//<artifactId>scalnet<\\/artifactId>/' pom.xml")
        functions.verset("${RELEASE_VERSION}", false)
        sh("sed -i '0,/<artifactId>.*<\\/artifactId>/s//<artifactId>scalnet_\${scala.binary.version}<\\/artifactId>/' pom.xml")
        configFileProvider([configFile(fileId: 'MAVEN_SETTINGS_DO-192', variable: 'MAVEN_SETTINGS')]) {
          if (!TESTS) {
            docker.image('ubuntu14cuda80').inside(dockerParams) {
                sh'''
                mvn -B -s ${MAVEN_SETTINGS} clean deploy -DskipTests -Dscalastyle.skip -DscalaVersion=${SCALA_VERSION} -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION}
                '''
            }
          }
          else {
            docker.image('ubuntu14cuda80').inside(dockerParams) {
                sh'''
                mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dscalastyle.skip -DscalaVersion=${SCALA_VERSION} -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION}
                '''
            }
          }

        }
    }
}

echo 'MARK: end of scalnet.groovy'
