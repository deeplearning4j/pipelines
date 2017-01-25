tool name: 'M339', type: 'maven'
def mvnHome = tool 'M339'

functions = load 'jobs/functions.groovy'

stage('Deeplearning4j Preparation') {
  functions.get_project_code("${DEEPLEARNING4J_PROJECT}")

  echo "Releasing ${DEEPLEARNING4J_PROJECT} version ${RELEASE_VERSION} (${SNAPSHOT_VERSION}) to repository ${STAGING_REPOSITORY}"
  echo "Check if ${RELEASE_VERSION} has been released already"

  dir("${DEEPLEARNING4J_PROJECT}") {
    functions.checktag("${DEEPLEARNING4J_PROJECT}")

    sh ("sed -i 's/<nd4j.version>.*<\\/nd4j.version>/<nd4j.version>$RELEASE_VERSION<\\/nd4j.version>/' pom.xml")
    sh ("sed -i 's/<datavec.version>.*<\\/datavec.version>/<datavec.version>$RELEASE_VERSION<\\/datavec.version>/' pom.xml")
    // sh ("'${mvnHome}/bin/mvn' versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=${RELEASE_VERSION}")
    functions.verset("${RELEASE_VERSION}", true)


    sh "./change-scala-versions.sh 2.10"
    sh "./change-cuda-versions.sh 7.5"
    configFileProvider([configFile(fileId: 'MAVEN_SETTINGS_DO-192', variable: 'MAVEN_SETTINGS')]) {
      sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests  ")}

    sh "./change-scala-versions.sh 2.11"
    sh "./change-cuda-versions.sh 8.0"
    //sh "'${mvnHome}/bin/mvn' clean deploy -Dgpg.executable=gpg2 -DperformRelease -Psonatype-oss-release -DskipTests -DstagingRepositoryId=$STAGING_REPOSITORY"
    //  configFileProvider(
    //   [configFile(fileId: '$MAVENSETS', variable: 'MAVEN_SETTINGS')]) {
    //  sh "'${mvnHome}/bin/mvn' clean deploy -Dgpg.executable=gpg2 -Dgpg.skip -DperformRelease -Psonatype-oss-release -DskipTests -DstagingRepositoryId=$STAGING_REPOSITORY"
    //  }
    configFileProvider([configFile(fileId: 'MAVEN_SETTINGS_DO-192', variable: 'MAVEN_SETTINGS')]) {
      sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests  ")}

    sh "./change-scala-versions.sh 2.10"
    sh "./change-cuda-versions.sh 8.0"
    // all of git tag or commit actions should be in pipeline.groovy after user "Release" input
    //sh "git commit -a -m 'Update to version $RELEASE_VERSION'"
    //sh "git tag -a -m '$DEEPLEARNING4J_PROJECT-$RELEASE_VERSION" "$DEEPLEARNING4J_PROJECT-$RELEASE_VERSION'"
    functions.verset("${RELEASE_VERSION}", true)
    configFileProvider([configFile(fileId: 'MAVEN_SETTINGS_DO-192', variable: 'MAVEN_SETTINGS')]) {
      sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests  ")}

    //sh "git commit -a -m 'Update to version $SNAPSHOT_VERSION'"
    // echo "Successfully performed release of ${DEEPLEARNING4J_PROJECT} version ${RELEASE_VERSION} (${SNAPSHOT_VERSION}) to repository ${STAGING_REPOSITORY}"
  }
}
// Messages for debugging
echo 'MARK: end of build-03-deeplearning4j.groovy'
