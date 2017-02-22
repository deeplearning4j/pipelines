tool name: 'M339', type: 'maven'
def mvnHome = tool 'M339'

functions = load 'jobs/dl4j/functions.groovy'

stage("${PROJECT}-Checkout-Sources") {
    functions.get_project_code("${PROJECT}")
    // functions.get_project_code("${LIBPROJECT}")
}

stage("${PROJECT}-buildMaven") {
dir("$PROJECT") {
    // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
    // Temporary section - please remove once it commited updates to source code
    // configFileProvider(
    //         [configFile(fileId: 'MAVEN_POM_DO-192', variable: 'POM_XML')
    //     ]) {
    //     sh "cp ${POM_XML} pom.xml"
    // }
    // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
    echo 'Set Project Version'
    // sh("'${mvnHome}/bin/mvn' versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=${VERSION}")
    functions.verset("${VERSION}", true)

    sh "./change-scala-versions.sh 2.10"
    sh "./change-cuda-versions.sh 7.5"

    configFileProvider(
            [configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')
            ]) {
        // sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests")
        sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests  " + " -Denv.LIBND4J_HOME=/var/lib/jenkins/workspace/Pipelines/build_nd4j/libnd4j ")

    }


    sh "./change-scala-versions.sh 2.11"
    sh "./change-cuda-versions.sh 8.0"

    configFileProvider(
            [configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')
            ]) {
        // sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests")
        sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests  " + "-Denv.LIBND4J_HOME=/var/lib/jenkins/workspace/Pipelines/build_nd4j/libnd4j ")
    }

}

}

// stage('Libnd4j Codecheck') {
//   functions.sonar("${LIBPROJECT}")
// }

// stage('Nd4j Codecheck') {
//   functions.sonar("${PROJECT}")
// }
/*

stage('Libnd4j build') {

  echo 'Build Native Operations'
  dir("${LIBPROJECT}") {
    functions.checktag("${LIBPROJECT}")

    withEnv(['TRICK_NVCC=YES', "LIBND4J_HOME=${WORKSPACE}/${LIBPROJECT}"]) {
      echo "Building ${LIBPROJECT} version ${VERSION} (${SNAPSHOT_VERSION})"
      // Check TRICK_NVCC and LIBND4J_HOME existence
      sh "env"
      // sh "export TRICK_NVCC=YES && export LIBND4J_HOME=${WORKSPACE}/${LIBPROJECT} && ./buildnativeoperations.sh -c cpu"
      // sh "export TRICK_NVCC=YES && export LIBND4J_HOME=${WORKSPACE}/${LIBPROJECT} && ./buildnativeoperations.sh -c cuda -v 7.5"
      // sh "export TRICK_NVCC=YES && export LIBND4J_HOME=${WORKSPACE}/${LIBPROJECT} && ./buildnativeoperations.sh -c cuda -v 8.0"

      // sh "./buildnativeoperations.sh -c cpu"
      // sh "./buildnativeoperations.sh -c cuda -v 7.5"
      // sh "./buildnativeoperations.sh -c cuda -v 8.0"

      // sh 'git tag -a ${LIBPROJECT}-${VERSION} -m ${LIBPROJECT}-${VERSION}'
    }
  }
}
stage('Nd4j Build') {
  echo "Building ${PROJECT} version ${VERSION} (${SNAPSHOT_VERSION}) to repository ${STAGING_REPOSITORY}"

  echo ("Check if ${VERSION} has been released already")
  dir("${PROJECT}") {
    functions.checktag("${PROJECT}")
  }

  // functions.sonar("${PROJECT}")

  echo 'Build components with Maven'
  dir("${PROJECT}") {

    echo 'Set Project Version'
    // sh ("'${mvnHome}/bin/mvn' versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=${VERSION}")
    functions.verset("${VERSION}", true)

    echo 'Maven Build, Package and Deploy'
    def check_repo = "${STAGING_REPOSITORY}"
    echo check_repo
    if (!check_repo) {
      echo 'STAGING_REPOSITORY is not set'
      // sh "./change-scala-versions.sh 2.10"
      // sh "./change-cuda-versions.sh 7.5"
      sh "./change-scala-versions.sh 2.10"
      sh "./change-cuda-versions.sh 7.5"

      // configFileProvider([configFile(fileId: 'maven-release-bintray-settings-1', variable: 'MAVEN_SETTINGS'),
      //                     configFile(fileId: 'maven-release-bintray-settings-security-1', variable: 'MAVEN_SECURITY_SETTINGS')]) {
      //                       sh ("'${mvnHome}/bin/mvn' -s $MAVEN_SETTINGS clean deploy \
      //                            -Dsettings.security=$MAVEN_SECURITY_SETTINGS \
      //                            -Dgpg.executable=gpg2 -Dgpg.skip -DperformRelease \
      //                            -DskipTests -Denforcer.skip -DstagingRepositoryId=$STAGING_REPOSITORY")
      //                     }

      sh "./change-scala-versions.sh 2.11"
      sh "./change-cuda-versions.sh 8.0"

      // configFileProvider([configFile(fileId: 'maven-release-bintray-settings-1', variable: 'MAVEN_SETTINGS'),
      //                     configFile(fileId: 'maven-release-bintray-settings-security-1', variable: 'MAVEN_SECURITY_SETTINGS')]) {
      //                       sh ("'${mvnHome}/bin/mvn' -s $MAVEN_SETTINGS clean deploy \
      //                            -Dsettings.security=$MAVEN_SECURITY_SETTINGS \
      //                            -Dgpg.executable=gpg2 -Dgpg.skip -DperformRelease \
      //                            -DskipTests -Denforcer.skip -DstagingRepositoryId=$STAGING_REPOSITORY")
      //                     }
    }

    sh "./change-scala-versions.sh 2.10"
    sh "./change-cuda-versions.sh 8.0"
    // sh ("'${mvnHome}/bin/mvn' versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=${VERSION}")
    functions.verset("${SNAPSHOT_VERSION}", true)

  }
}
// Messages for debugging
echo 'MARK: end of nd4j.groovy'
*/
