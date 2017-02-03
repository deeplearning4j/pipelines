tool name: 'M339', type: 'maven'
def mvnHome = tool 'M339'

functions = load "${PDIR}/functions.groovy"

stage("${DATAVEC_PROJECT}-CheckoutSources") {
    functions.get_project_code("${DATAVEC_PROJECT}")
}

stage("${DATAVEC_PROJECT}-Build") {

  echo "Releasing ${DATAVEC_PROJECT} version ${RELEASE_VERSION}"

  dir("${DATAVEC_PROJECT}") {
    functions.checktag("${DATAVEC_PROJECT}")
    functions.verset("${RELEASE_VERSION}", true)
    sh "sed -i 's/<nd4j.version>.*<\\/nd4j.version>/<nd4j.version>${RELEASE_VERSION}<\\/nd4j.version>/' pom.xml"

    sh "./change-scala-versions.sh 2.10"
    configFileProvider([configFile(fileId: 'MAVEN_SETTINGS_DO-192', variable: 'MAVEN_SETTINGS')]) {
      sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean install ")
      // sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests ")
    }

    sh "./change-scala-versions.sh 2.11"
    configFileProvider([configFile(fileId: 'MAVEN_SETTINGS_DO-192', variable: 'MAVEN_SETTINGS')]) {
      sh( "'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean instal ")
      // sh( "'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests ")
    }

    sh "./change-scala-versions.sh 2.10"
    configFileProvider([configFile(fileId: 'MAVEN_SETTINGS_DO-192', variable: 'MAVEN_SETTINGS')]) {
      sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean install  ")
      // sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests  ")
    }


    //  sh "${mvnHome}/bin/mvn' versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=$SNAPSHOT_VERSION"

    //  sh "git commit -a -m 'Update to version $SNAPSHOT_VERSION'"
  }
}

// Findbugs needs sources to be compiled. Please build project before executing sonar
stage("${DATAVEC_PROJECT}-Codecheck") {
  functions.sonar("${DATAVEC_PROJECT}")
}

// Messages for debugging
echo 'MARK: end of datavec.groovy'
