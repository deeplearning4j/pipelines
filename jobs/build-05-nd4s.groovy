node('master') {

   stage('Preparation')    {
    checkout([$class: 'GitSCM',
       branches: [[name: '*/intropro']],
       doGenerateSubmoduleConfigurations: false,
       extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: '$ND4S_PROJECT'], [$class: 'CloneOption', honorRefspec: true, noTags: true, reference: '', shallow: true]],
       submoduleCfg: [],
       userRemoteConfigs: [[url: 'git@github.com:$ACCOUNT/$ARBITER_PROJECT.git', credentialsId: 'b0fc06e6-a23c-4886-a0c3-f53800a41663']]])
    }
    echo 'Releasing version $RELEASE_VERSION ($SNAPSHOT_VERSION) to repository $STAGING_REPOSITORY'
    echo 'Check if $RELEASE_VERSION has been released already'
    dir("$ND4S_PROJECT") {
      def exitValue = sh (returnStdout: true, script: """git tag -l \"$ND4S_PROJECT-$RELEASE_VERSION\"""")
       if (exitValue != '') {
          echo 'Error: Version $RELEASE_VERSION has already been released!'
       }

      sh ("sed -i 's/version := \".*\",/version := \"$RELEASE_VERSION\",/' build.sbt")
      sh ("sed -i 's/nd4jVersion := \".*\",/nd4jVersion := \"$RELEASE_VERSION\",/' build.sbt")
      sh ("sbt +publishSigned")
    }

   stage ('Build') {
    dir("$ND4S_PROJECT") {
      //sh "git commit -a -m 'Update to version $RELEASE_VERSION'"
      //sh "git tag -a -m '$ND4S_PROJECT-$RELEASE_VERSION" "$ND4S_PROJECT-$RELEASE_VERSION'"
      sh ("sed -i 's/version := \".*\",/version := \"$SNAPSHOT_VERSION\",/' build.sbt")
      sh ("sed -i 's/nd4jVersion := \".*\",/nd4jVersion := \"$SNAPSHOT_VERSION\",/' build.sbt")
      //sh "git commit -a -m 'Update to version $SNAPSHOT_VERSION'"
      sh "echo 'Successfully performed release of version $RELEASE_VERSION ($SNAPSHOT_VERSION) to repository $STAGING_REPOSITORY'"
      }
   } 
}