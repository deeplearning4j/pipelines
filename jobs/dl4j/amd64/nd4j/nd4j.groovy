tool name: 'M339', type: 'maven'
def mvnHome = tool 'M339'

functions = load "${PDIR}/functions.groovy"

stage("${PROJECT}-CheckoutSources") {
    functions.get_project_code("${PROJECT}")
}

stage("${PROJECT}-Build") {
  dir("${PROJECT}") {
    // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
    // Temporary section - please remove once it commited updates to source code
    // configFileProvider(
    //         [configFile(fileId: 'MAVEN_POM_DO-192', variable: 'POM_XML')
    //     ]) {
    //     sh "cp ${POM_XML} pom.xml"
    // }
    // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
    echo 'Set Project Version'
    // sh("'${mvnHome}/bin/mvn' versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=${RELEASE_VERSION}")
    functions.verset("${RELEASE_VERSION}", true)

    sh "./change-scala-versions.sh 2.10"
    sh "./change-cuda-versions.sh 7.5"

    configFileProvider(
            [configFile(fileId: 'MAVEN_SETTINGS_DO-192', variable: 'MAVEN_SETTINGS')
            ]) {
        sh("'${mvnHome}/bin/mvn' -e -s ${MAVEN_SETTINGS} clean install -DskipTests")
        // sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests  ")
        // sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests  " +  " -Denv.LIBND4J_HOME=/var/lib/jenkins/workspace/Pipelines/build_nd4j/libnd4j ")
    }


    sh "./change-scala-versions.sh 2.11"
    sh "./change-cuda-versions.sh 8.0"

    configFileProvider(
            [configFile(fileId: 'MAVEN_SETTINGS_DO-192', variable: 'MAVEN_SETTINGS')
            ]) {
        sh("'${mvnHome}/bin/mvn' -e -s ${MAVEN_SETTINGS} clean install -DskipTests")
        // sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests  ")
        // sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests  " + "-Denv.LIBND4J_HOME=/var/lib/jenkins/workspace/Pipelines/build_nd4j/libnd4j ")
    }

  }

}

// Findbugs needs sources to be compiled. Please build project before executing sonar
stage("${PROJECT}-Codecheck") {
    functions.sonar("${PROJECT}")
}
// Messages for debugging
echo 'MARK: end of nd4j.groovy'
