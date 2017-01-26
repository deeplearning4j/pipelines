tool name: 'M339', type: 'maven'
def mvnHome = tool 'M339'

functions = load 'jobs/functions.groovy'

stage('Scalnet Preparation') {

  functions.get_project_code("${SCALNET_PROJECT}")

  echo "Releasing ${SCALNET_PROJECT} version ${RELEASE_VERSION} (${SNAPSHOT_VERSION}) to repository ${STAGING_REPOSITORY}"
  echo "Check if ${RELEASE_VERSION} has been released already"

  dir("${SCALNET_PROJECT}") {
    // if (${SNAPSHOT_VERSION} != '*-SNAPSHOT' ) {
    //    // error statement stops pipeline if if is true
    //    error("Error: Version ${SNAPSHOT_VERSION} should finish with -SNAPSHOT")
    // }
    functions.checktag("${SCALNET_PROJECT}")

    sh ("sed -i 's/<nd4j.version>.*<\\/nd4j.version>/<nd4j.version>${RELEASE_VERSION}<\\/nd4j.version>/' pom.xml")
    sh ("sed -i 's/<datavec.version>.*<\\/datavec.version>/<datavec.version>${RELEASE_VERSION}<\\/datavec.version>/' pom.xml")
    sh ("sed -i 's/<dl4j.version>.*<\\/dl4j.version>/<dl4j.version>${RELEASE_VERSION}<\\/dl4j.version>/' pom.xml")
    // # In its normal state, repo should contain a snapshot version stanza
    sh ("sed -i 's/<version>.*-SNAPSHOT<\\/version>/<version>${RELEASE_VERSION}<\\/version>/' pom.xml")
    functions.verset("${RELEASE_VERSION}", false)

    configFileProvider([configFile(fileId: 'MAVEN_SETTINGS_DO-192', variable: 'MAVEN_SETTINGS')]) {
      sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests -Dscalastyle.skip -DscalaVersion=2.10")}

    configFileProvider([configFile(fileId: 'MAVEN_SETTINGS_DO-192', variable: 'MAVEN_SETTINGS')]) {
      sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests -Dscalastyle.skip -DscalaVersion=2.11")}
  }
}

// There is no scala plugin for SonarQube
// stage('Scalnet Codecheck') {
//   functions.sonar("${SCALNET_PROJECT}")
// }

/*stage ('Scalnet Build') {
  dir("${SCALNET_PROJECT}") {

    //  configFileProvider(
    //   [configFile(fileId: '$MAVENSETS', variable: 'MAVEN_SETTINGS')]) {
    //     sh "./change-scala-versions.sh 2.10"
    //     sh "'${mvnHome}/bin/mvn' -DscalaVersion=2.10 clean deploy -Dgpg.executable=gpg2 -DperformRelease -Psonatype-oss-release -DskipTests -DstagingRepositoryId=$STAGING_REPOSITORY -Dscalastyle.skip"
    //     sh "./change-scala-versions.sh 2.11"
    //     sh "'${mvnHome}/bin/mvn' -DscalaVersion=2.11 clean deploy -Dgpg.executable=gpg2 -DperformRelease -Psonatype-oss-release -DskipTests -DstagingRepositoryId=$STAGING_REPOSITORY -Dscalastyle.skip"

    // sh "git commit -a -m 'Update to version $RELEASE_VERSION'"
    // sh "git tag -a -m '$RSCALNET_PROJECT-$RELEASE_VERSION" "$SCALNET_PROJECT-$RELEASE_VERSION'"

    sh ("sed -i 's/<nd4j.version>.*<\\/nd4j.version>/<nd4j.version>${SNAPSHOT_VERSION}<\\/nd4j.version>/' pom.xml")
    sh ("sed -i 's/<datavec.version>.*<\\/datavec.version>/<datavec.version>${SNAPSHOT_VERSION}<\\/datavec.version>/' pom.xml")
    sh ("sed -i 's/<dl4j.version>.*<\\/dl4j.version>/<dl4j.version>${SNAPSHOT_VERSION}<\\/dl4j.version>/' pom.xml")
    // # back to a version stanza
    sh ("sed -i 's/<version>${RELEASE_VERSION}<\\/version>/<version>${SNAPSHOT_VERSION}<\\/version>/' pom.xml")
    // sh "${mvnHome}/bin/mvn' -DscalaVersion=2.10 versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=$SNAPSHOT_VERSION"
    functions.verset("${SNAPSHOT_VERSION}", true)
    // sh "git commit -a -m 'Update to version $SNAPSHOT_VERSION'"
    // echo "Successfully performed release of ${SCALNET_PROJECT} version ${RELEASE_VERSION} (${SNAPSHOT_VERSION}) to repository ${STAGING_REPOSITORY}"
}
}*/
// Messages for debugging
echo 'MARK: end of build-08-scalnet.groovy'
