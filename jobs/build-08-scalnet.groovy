   tool name: 'M339', type: 'maven'
   def mvnHome
   mvnHome = tool 'M339'
   stage('Scalnet Preparation')    {
    checkout([$class: 'GitSCM',
       branches: [[name: '*/intropro']],
       doGenerateSubmoduleConfigurations: false,
       extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: '$SCALNET_PROJECT'], [$class: 'CloneOption', honorRefspec: true, noTags: true, reference: '', shallow: true]],
       submoduleCfg: [],
       userRemoteConfigs: [[url: 'https://github.com/$ACCOUNT/$SCALNET_PROJECT.git']]])
    }
    echo 'Releasing version $RELEASE_VERSION ($SNAPSHOT_VERSION) to repository $STAGING_REPOSITORY'
    echo 'Check if $RELEASE_VERSION has been released already'
    dir("$SCALNET_PROJECT") {
      if ($SNAPSHOT_VERSION != '*-SNAPSHOT' ) {
         // error statement stops pipeline if if is true
         error 'Error: Version $SNAPSHOT_VERSION should finish with -SNAPSHOT'
      }

      def exitValue = sh (returnStdout: true, script: """git tag -l \"$SCALNET_PROJECT-$RELEASE_VERSION\"""")
       if (exitValue != '') {
          echo 'Error: Version $RELEASE_VERSION has already been released!'
       }
      sh ("sed -i 's/<nd4j.version>.*<\\/nd4j.version>/<nd4j.version>$RELEASE_VERSION<\\/nd4j.version>/' pom.xml")
      sh ("sed -i 's/<datavec.version>.*<\\/datavec.version>/<datavec.version>$RELEASE_VERSION<\\/datavec.version>/' pom.xml")
      sh ("sed -i 's/<dl4j.version>.*<\\/dl4j.version>/<dl4j.version>$RELEASE_VERSION<\\/dl4j.version>/' pom.xml")
      sh ("sed -i 's/<version>.*-SNAPSHOT<\\/version>/<version>$RELEASE_VERSION<\\/version>/' pom.xml")
    }
   
   stage ('Scalnet Build') {
    //  configFileProvider(
    //   [configFile(fileId: '$MAVENSETS', variable: 'MAVEN_SETTINGS')]) {
     //sh "'${mvnHome}/bin/mvn' -DscalaVersion=2.10 clean deploy -Dgpg.executable=gpg2 -DperformRelease -Psonatype-oss-release -DskipTests -DstagingRepositoryId=$STAGING_REPOSITORY -Dscalastyle.skip"
     //sh "'${mvnHome}/bin/mvn' -DscalaVersion=2.11 clean deploy -Dgpg.executable=gpg2 -DperformRelease -Psonatype-oss-release -DskipTests -DstagingRepositoryId=$STAGING_REPOSITORY -Dscalastyle.skip"
    //sh "git commit -a -m 'Update to version $RELEASE_VERSION'"
    //sh "git tag -a -m '$RSCALNET_PROJECT-$RELEASE_VERSION" "$SCALNET_PROJECT-$RELEASE_VERSION'"

     sh ("sed -i 's/<nd4j.version>.*<\\/nd4j.version>/<nd4j.version>$SNAPSHOT_VERSION<\\/nd4j.version>/' pom.xml")
     sh ("sed -i 's/<datavec.version>.*<\\/datavec.version>/<datavec.version>$SNAPSHOT_VERSION<\\/datavec.version>/' pom.xml")
     sh ("sed -i 's/<dl4j.version>.*<\\/dl4j.version>/<dl4j.version>$SNAPSHOT_VERSION<\\/dl4j.version>/' pom.xml")
     sh ("sed -i 's/<version>$RELEASE_VERSION<\\/version>/<version>$SNAPSHOT_VERSION<\\/version>/' pom.xml")

     //sh "${mvnHome}/bin/mvn' -DscalaVersion=2.10 versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=$SNAPSHOT_VERSION"
     //sh "git commit -a -m 'Update to version $SNAPSHOT_VERSION'"
     sh "echo 'Successfully performed release of version $RELEASE_VERSION ($SNAPSHOT_VERSION) to repository $STAGING_REPOSITORY'"
    }


