tool name: 'M339', type: 'maven'
def mvnHome = tool 'M339'

functions = load 'jobs/functions.groovy'

stage('Arbiter Preparation') {
  functions.get_project_code("${ARBITER_PROJECT}")

  echo "Releasing ${ARBITER_PROJECT} version ${RELEASE_VERSION} (${SNAPSHOT_VERSION}) to repository ${STAGING_REPOSITORY}"
  echo "Check if ${RELEASE_VERSION} has been released already"

  dir("${ARBITER_PROJECT}") {
    functions.checktag("${ARBITER_PROJECT}")

    sh ("sed -i 's/<nd4j.version>.*<\\/nd4j.version>/<nd4j.version>$RELEASE_VERSION<\\/nd4j.version>/' pom.xml")
    sh ("sed -i 's/<datavec.version>.*<\\/datavec.version>/<datavec.version>$RELEASE_VERSION<\\/datavec.version>/' pom.xml")
    sh ("sed -i 's/<dl4j.version>.*<\\/dl4j.version>/<dl4j.version>$RELEASE_VERSION<\\/dl4j.version>/' pom.xml")
    // sh ("'${mvnHome}/bin/mvn' versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=${RELEASE_VERSION}")
    functions.verset("${RELEASE_VERSION}", true)
    sh "./change-scala-versions.sh 2.10"
    configFileProvider([configFile(fileId: 'MAVEN_SETTINGS_DO-192', variable: 'MAVEN_SETTINGS')]) {
      sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests -Dmaven.test.skip ")}

    sh "./change-scala-versions.sh 2.11"
    configFileProvider([configFile(fileId: 'MAVEN_SETTINGS_DO-192', variable: 'MAVEN_SETTINGS')]) {
      sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests -Dmaven.test.skip ")}



  }
}

// stage('Arbiter Codecheck') {
//   functions.sonar("${ARBITER_PROJECT}")
// }
/*
stage ('Arbiter Build') {
  dir("${ARBITER_PROJECT}") {

    sh "./change-scala-versions.sh 2.10"
    //sh "'${mvnHome}/bin/mvn' clean deploy -Dgpg.executable=gpg2 -DperformRelease -Psonatype-oss-release -Dmaven.test.skip -DskipTests -DstagingRepositoryId=$STAGING_REPOSITORY"

    sh "./change-scala-versions.sh 2.11"
    //sh "'${mvnHome}/bin/mvn' clean deploy -Dgpg.executable=gpg2 -DperformRelease -Psonatype-oss-release -Dmaven.test.skip -DskipTests -DstagingRepositoryId=$STAGING_REPOSITORY"
    //  configFileProvider(
    //   [configFile(fileId: '$MAVENSETS', variable: 'MAVEN_SETTINGS')]) {
    //  sh "'${mvnHome}/bin/mvn' clean deploy -Dgpg.executable=gpg2 -Dgpg.skip -DperformRelease -Psonatype-oss-release -DskipTests -DstagingRepositoryId=$STAGING_REPOSITORY"
    //  }

    sh "./change-scala-versions.sh 2.10"
    // all of git tag or commit actions should be in pipeline.groovy after user "Release" input
    // sh "git commit -a -m 'Update to version $RELEASE_VERSION'"
    // sh "git tag -a -m '$ARBITER_PROJECT-$RELEASE_VERSION" "$ARBITER_PROJECT-$RELEASE_VERSION'"
    sh ("sed -i 's/<nd4j.version>.*<\\/nd4j.version>/<nd4j.version>${SNAPSHOT_VERSION}<\\/nd4j.version>/' pom.xml")
    sh ("sed -i 's/<datavec.version>.*<\\/datavec.version>/<datavec.version>${SNAPSHOT_VERSION}<\\/datavec.version>/' pom.xml")
    sh ("sed -i 's/<dl4j.version>.*<\\/dl4j.version>/<dl4j.version>${SNAPSHOT_VERSION}<\\/dl4j.version>/' pom.xml")
    // sh "${mvnHome}/bin/mvn' versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=${SNAPSHOT_VERSION}"
    functions.verset("${SNAPSHOT_VERSION}", true)
    // sh "git commit -a -m 'Update to version $SNAPSHOT_VERSION'"
    // echo "Successfully performed release of ${ARBITER_PROJECT} version ${RELEASE_VERSION} (${SNAPSHOT_VERSION}) to repository ${STAGING_REPOSITORY}"
  }
}*/
// Messages for debugging
echo 'MARK: end of build-04-arbiter.groovy'
