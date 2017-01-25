tool name: 'M339', type: 'maven'
def mvnHome = tool 'M339'

functions = load 'jobs/functions.groovy'

stage('Datavec Preparation') {
  functions.get_project_code("${DATAVEC_PROJECT}")

//  echo "Releasing ${DATAVEC_PROJECT} version ${RELEASE_VERSION} (${SNAPSHOT_VERSION}) to repository ${STAGING_REPOSITORY}"
//  echo "Check if ${RELEASE_VERSION} has been released already"

  dir("${DATAVEC_PROJECT}") {
    functions.checktag("${DATAVEC_PROJECT}")
    functions.verset("${RELEASE_VERSION}", true)
    sh "sed -i 's/<nd4j.version>.*<\\/nd4j.version>/<nd4j.version>$RELEASE_VERSION<\\/nd4j.version>/' pom.xml"

    sh "./change-scala-versions.sh 2.10"
    configFileProvider([configFile(fileId: 'MAVEN_SETTINGS_DO-192', variable: 'MAVEN_SETTINGS')]) {
      sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests ")}

    sh "./change-scala-versions.sh 2.11"
    configFileProvider([configFile(fileId: 'MAVEN_SETTINGS_DO-192', variable: 'MAVEN_SETTINGS')]) {
      sh( "'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests ")}

    sh "./change-scala-versions.sh 2.10"
    configFileProvider([configFile(fileId: 'MAVEN_SETTINGS_DO-192', variable: 'MAVEN_SETTINGS')]) {
      sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests  ")}


    //  sh "${mvnHome}/bin/mvn' versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=$SNAPSHOT_VERSION"

    //  sh "git commit -a -m 'Update to version $SNAPSHOT_VERSION'"
    // echo "Successfully performed release of ${DATAVEC_PROJECT} version ${RELEASE_VERSION} (${SNAPSHOT_VERSION}) to repository ${STAGING_REPOSITORY}"
  }
}
// Messages for debugging
echo 'MARK: end of build-02-datavec.groovy'
