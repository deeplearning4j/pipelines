tool name: 'M339', type: 'maven'
def mvnHome = tool 'M339'

functions = load 'jobs/functions.groovy'

stage('Gym-Java-Client Preparation') {

  functions.get_project_code("${GYM_JAVA_CLIENT_PROJECT}")

  echo "Releasing ${GYM_JAVA_CLIENT_PROJECT} version ${RELEASE_VERSION} (${SNAPSHOT_VERSION}) to repository ${STAGING_REPOSITORY}"
  echo "Check if ${RELEASE_VERSION} has been released already"

  dir("${GYM_JAVA_CLIENT_PROJECT}") {
    functions.checktag("${GYM_JAVA_CLIENT_PROJECT}")

  sh ("sed -i 's/<nd4j.version>.*<\\/nd4j.version>/<nd4j.version>${RELEASE_VERSION}<\\/nd4j.version>/' pom.xml")
  sh ("sed -i 's/<datavec.version>.*<\\/datavec.version>/<datavec.version>${RELEASE_VERSION}<\\/datavec.version>/' pom.xml")
  // sh ("'${mvnHome}/bin/mvn' versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=${RELEASE_VERSION}")
  functions.verset("${RELEASE_VERSION}", true)
  }
}

stage('Gym-Java-Client Codecheck') {
  functions.sonar("${GYM_JAVA_CLIENT_PROJECT}")
}

stage ('Gym-Java-Client Build') {
  dir("${GYM_JAVA_CLIENT_PROJECT}") {

    // sh "'${mvnHome}/bin/mvn' clean deploy -Dgpg.executable=gpg2 -DperformRelease -Psonatype-oss-release -DskipTests -DstagingRepositoryId=$STAGING_REPOSITORY"
    // sh "git commit -a -m 'Update to version $RELEASE_VERSION'"
    // sh "git tag -a -m '$GYM_JAVA_CLIENT_PROJECT-$RELEASE_VERSION" "$GYM_JAVA_CLIENT_PROJECT-$RELEASE_VERSION'"
    sh ("sed -i 's/<nd4j.version>.*<\\/nd4j.version>/<nd4j.version>${SNAPSHOT_VERSION}<\\/nd4j.version>/' pom.xml")
    sh ("sed -i 's/<datavec.version>.*<\\/datavec.version>/<datavec.version>${SNAPSHOT_VERSION}<\\/datavec.version>/' pom.xml")
    // sh ("'${mvnHome}/bin/mvn' versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=${SNAPSHOT_VERSION}")
    functions.verset("${SNAPSHOT_VERSION}", true)
    // sh "git commit -a -m 'Update to version $SNAPSHOT_VERSION'"
    // echo "Successfully performed release of ${GYM_JAVA_CLIENT_PROJECT} version ${RELEASE_VERSION} (${SNAPSHOT_VERSION}) to repository ${STAGING_REPOSITORY}"
  }
}
// Messages for debugging
echo 'MARK: end of build-06-gym-java-client.groovy'
