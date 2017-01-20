tool name: 'M339', type: 'maven'
def mvnHome = tool 'M339'
stage('Datavec Preparation') {
  checkout([$class: 'GitSCM',
             branches: [[name: '*/intropro']],
             doGenerateSubmoduleConfigurations: false,
             extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: '$DATAVEC_PROJECT'], [$class: 'CloneOption', honorRefspec: true, noTags: true, reference: '', shallow: true]],
             submoduleCfg: [],
             userRemoteConfigs: [[url: 'https://github.com/$ACCOUNT/$DATAVEC_PROJECT.git']]])

  echo "Releasing version $RELEASE_VERSION ($SNAPSHOT_VERSION) to repository $STAGING_REPOSITORY"
  dir("$DATAVEC_PROJECT") {
    def exitValue = sh (returnStdout: true, script: """git tag -l \"$DATAVEC_PROJECT-$RELEASE_VERSION\"""")
    if (exitValue != null) {
        //  echo "Error: Version $RELEASE_VERSION has already been released!"
        error 'Version $RELEASE_VERSION has already been released!'
      }
    sh ("sed -i 's/<nd4j.version>.*<\\/nd4j.version>/<nd4j.version>$RELEASE_VERSION<\\/nd4j.version>/' pom.xml")
    sh ("'${mvnHome}/bin/mvn' versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=$RELEASE_VERSION")
  }
}

// stage('Datavec Codecheck') {
//   echo 'Check $ACCOUNT/$PROJECT code with SonarQube'
// }

stage ('Datavec Build') {
  dir("$DATAVEC_PROJECT") {
    sh "./change-scala-versions.sh 2.10"
    //sh "'${mvnHome}/bin/mvn' clean deploy -Dgpg.executable=gpg2 -DperformRelease -Psonatype-oss-release -DskipTests -DstagingRepositoryId=$STAGING_REPOSITORY"

    sh "./change-scala-versions.sh 2.11"
    //sh "'${mvnHome}/bin/mvn' clean deploy -Dgpg.executable=gpg2 -DperformRelease -Psonatype-oss-release -DskipTests -DstagingRepositoryId=$STAGING_REPOSITORY"

    sh "./change-scala-versions.sh 2.10"
    // all of git tag or commit actions should be in pipeline.groovy after user "Release" input
    //sh "git commit -a -m 'Update to version $RELEASE_VERSION'"
    //sh "git tag -a -m '$DATAVEC_PROJECT-$RELEASE_VERSION" "$DATAVEC_PROJECT-$RELEASE_VERSION'"

    //  sh "sed -i 's/<nd4j.version>.*<\\/nd4j.version>/<nd4j.version>$SNAPSHOT_VERSION<\\/nd4j.version>/'' pom.xml"
    //  sh "${mvnHome}/bin/mvn' versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=$SNAPSHOT_VERSION"
    //  sh "git commit -a -m 'Update to version $SNAPSHOT_VERSION'"
    //  sh "echo 'Successfully performed release of version $RELEASE_VERSION ($SNAPSHOT_VERSION) to repository $STAGING_REPOSITORY'"
  }
}
// Messages for debugging
echo 'MARK: end of build-02-datavec.groovy'
