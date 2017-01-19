//node('master') {
   def mvnHome
   stage('Preparation')    {
    checkout([$class: 'GitSCM',
       branches: [[name: '*/intropro']],
       doGenerateSubmoduleConfigurations: false,
       extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: '$DATAVEC_PROJECT'], [$class: 'CloneOption', honorRefspec: true, noTags: true, reference: '', shallow: true]],
       submoduleCfg: [],
       userRemoteConfigs: [[url: 'git@github.com:$ACCOUNT/$DATAVEC_PROJECT.git', credentialsId: 'b0fc06e6-a23c-4886-a0c3-f53800a41663']]])

    echo "Releasing version $RELEASE_VERSION ($SNAPSHOT_VERSION) to repository $STAGING_REPOSITORY"
    dir("$DEEPLEARNING4J_PROJECT") {
      def exitValue = sh (returnStdout: true, script: """git tag -l \"$DATAVEC_PROJECT-$RELEASE_VERSION\"""")
        if (exitValue != '') {
           echo "Error: Version $RELEASE_VERSION has already been released!"
        }
     mvnHome = tool 'M3'
     sh ("sed -i 's/<nd4j.version>.*<\\/nd4j.version>/<nd4j.version>$RELEASE_VERSION<\\/nd4j.version>/' pom.xml")
     sh ("'${mvnHome}/bin/mvn' versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=$RELEASE_VERSION")

    }
  }
   stage ('Build') {
    dir("$ARBITER_PROJECT") {
     sh "./change-scala-versions.sh 2.10"
     sh "'${mvnHome}/bin/mvn' clean deploy -Dgpg.executable=gpg2 -DperformRelease -Psonatype-oss-release -DskipTests -DstagingRepositoryId=$STAGING_REPOSITORY"

     sh "./change-scala-versions.sh 2.11" 
     sh "'${mvnHome}/bin/mvn' clean deploy -Dgpg.executable=gpg2 -DperformRelease -Psonatype-oss-release -DskipTests -DstagingRepositoryId=$STAGING_REPOSITORY"
   

    sh "./change-scala-versions.sh 2.10"
    //sh "git commit -a -m 'Update to version $RELEASE_VERSION'"
    //sh "git tag -a -m '$DATAVEC_PROJECT-$RELEASE_VERSION" "$DATAVEC_PROJECT-$RELEASE_VERSION'"
    
    //  sh "sed -i 's/<nd4j.version>.*<\\/nd4j.version>/<nd4j.version>$SNAPSHOT_VERSION<\\/nd4j.version>/'' pom.xml"
    //  sh "${mvnHome}/bin/mvn' versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=$SNAPSHOT_VERSION"
    //  sh "git commit -a -m 'Update to version $SNAPSHOT_VERSION'"
    //  sh "echo 'Successfully performed release of version $RELEASE_VERSION ($SNAPSHOT_VERSION) to repository $STAGING_REPOSITORY'"
    }
  }
//}
