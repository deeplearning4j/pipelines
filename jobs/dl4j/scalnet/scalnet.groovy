tool name: 'M339', type: 'maven'
def mvnHome = tool 'M339'

env.JAVA_HOME = "${tool 'jdk-8u121'}"
env.PATH = "${env.JAVA_HOME}/bin:${env.PATH}"

stage("${SCALNET_PROJECT}-CheckoutSources") {
    functions.get_project_code("${SCALNET_PROJECT}")
}

// There is no scala plugin for SonarQube
// stage("${SCALNET_PROJECT}-Codecheck") {
//   functions.sonar("${SCALNET_PROJECT}")
// }

stage("${SCALNET_PROJECT}-Build") {

  echo "Releasing ${SCALNET_PROJECT} version ${RELEASE_VERSION}"

  dir("${SCALNET_PROJECT}") {

    functions.checktag("${SCALNET_PROJECT}")

/*
    sh ("sed -i 's/<nd4j.version>.*<\\/nd4j.version>/<nd4j.version>${RELEASE_VERSION}<\\/nd4j.version>/' pom.xml")
    sh ("sed -i 's/<datavec.version>.*<\\/datavec.version>/<datavec.version>${RELEASE_VERSION}<\\/datavec.version>/' pom.xml")
    sh ("sed -i 's/<dl4j.version>.*<\\/dl4j.version>/<dl4j.version>${RELEASE_VERSION}<\\/dl4j.version>/' pom.xml")
    // # In its normal state, repo should contain a snapshot version stanza
    sh ("sed -i 's/<version>.*-SNAPSHOT<\\/version>/<version>${RELEASE_VERSION}<\\/version>/' pom.xml")
*/

    sh  ("sed -i '0,/<artifactId>.*<\\/artifactId>/s//<artifactId>scalnet<\\/artifactId>/' pom.xml")
    functions.verset("${RELEASE_VERSION}", false)
    sh("sed -i '0,/<artifactId>.*<\\/artifactId>/s//<artifactId>scalnet_\${scala.binary.version}<\\/artifactId>/' pom.xml")

    configFileProvider([configFile(fileId: 'MAVEN_SETTINGS_DO-192', variable: 'MAVEN_SETTINGS')]) {
      sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests -Dscalastyle.skip -DscalaVersion=${SCALA_VERSION} -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION}")
    }

/*    sh  ("sed -i '0,/<artifactId>.*<\\/artifactId>/s//<artifactId>scalnet<\\/artifactId>/' pom.xml")
    functions.verset("${RELEASE_VERSION}", false)
    sh("sed -i '0,/<artifactId>.*<\\/artifactId>/s//<artifactId>scalnet_\${scala.binary.version}<\\/artifactId>/' pom.xml")

    configFileProvider([configFile(fileId: 'MAVEN_SETTINGS_DO-192', variable: 'MAVEN_SETTINGS')]) {
      sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests -Dscalastyle.skip -DscalaVersion=2.11 -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION}")
    }*/
  }
}

// Messages for debugging
echo 'MARK: end of scalnet.groovy'
