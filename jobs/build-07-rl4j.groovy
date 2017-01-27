tool name: 'M339', type: 'maven'
def mvnHome = tool 'M339'

functions = load 'jobs/functions.groovy'

stage("${RL4J_PROJECT}-build") {

  functions.get_project_code("${RL4J_PROJECT}")

  echo "Releasing ${RL4J_PROJECT} version ${RELEASE_VERSION} (${SNAPSHOT_VERSION}) to repository ${STAGING_REPOSITORY}"
  echo "Check if ${RELEASE_VERSION} has been released already"

  dir("${RL4J_PROJECT}") {
    functions.checktag("${RL4J_PROJECT}")

    sh ("sed -i 's/<nd4j.version>.*<\\/nd4j.version>/<nd4j.version>${RELEASE_VERSION}<\\/nd4j.version>/' pom.xml")
    sh ("sed -i 's/<datavec.version>.*<\\/datavec.version>/<datavec.version>${RELEASE_VERSION}<\\/datavec.version>/' pom.xml")
    sh ("sed -i 's/<dl4j.version>.*<\\/dl4j.version>/<dl4j.version>${RELEASE_VERSION}<\\/dl4j.version>/' pom.xml")
    // sh ("'${mvnHome}/bin/mvn' versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=${RELEASE_VERSION}")
    functions.verset("${RELEASE_VERSION}", true)
    configFileProvider([configFile(fileId: 'MAVEN_SETTINGS_DO-192', variable: 'MAVEN_SETTINGS')]) {
      sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests ")}

  }
}

// stage('Rl4j Codecheck') {
//   functions.sonar("${RL4J_PROJECT}")
// }
/*
stage ('Rl4j Build') {
  dir("${RL4J_PROJECT}") {

    //  configFileProvider(
      // [configFile(fileId: '$MAVENSETS', variable: 'MAVEN_SETTINGS')]) {
    // sh "'${mvnHome}/bin/mvn' clean deploy -Dgpg.executable=gpg2 -DperformRelease -Psonatype-oss-release -DskipTests -DstagingRepositoryId=$STAGING_REPOSITORY"
    // sh "git commit -a -m 'Update to version $RELEASE_VERSION'"
    // sh "git tag -a -m '$RL4J_PROJECT-$RELEASE_VERSION" "$RL4J_PROJECT-$RELEASE_VERSION'"
    sh "sed -i 's/<nd4j.version>.*<\\/nd4j.version>/<nd4j.version>${SNAPSHOT_VERSION}<\\/nd4j.version>/' pom.xml"
    sh "sed -i 's/<datavec.version>.*<\\/datavec.version>/<datavec.version>${SNAPSHOT_VERSION}<\\/datavec.version>/' pom.xml"
    sh "sed -i 's/<dl4j.version>.*<\\/dl4j.version>/<dl4j.version>${SNAPSHOT_VERSION}<\\/dl4j.version>/' pom.xml"

    // sh ("'${mvnHome}/bin/mvn' versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=${SNAPSHOT_VERSION}")
    functions.verset("${SNAPSHOT_VERSION}", true)
    // sh "git commit -a -m 'Update to version $SNAPSHOT_VERSION'"
    // echo "Successfully performed release of ${RL4J_PROJECT} version ${RELEASE_VERSION} (${SNAPSHOT_VERSION}) to repository ${STAGING_REPOSITORY}"
  }
}*/
// Messages for debugging
echo 'MARK: end of build-07-rl4j.groovy'
