stage("${PROJECT}-CheckoutSources") {
    functions.get_project_code("${PROJECT}")
}

stage("${PROJECT}-Build-${PLATFORM_NAME}") {
    dir("${LIBPROJECT}/blasbuild") {
        sh("ln -s cuda-${CUDA_VERSION} cuda")
    }

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
        // sh("'mvn' versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=${RELEASE_VERSION}")
        functions.verset("${RELEASE_VERSION}", true)

        sh "./change-scala-versions.sh ${SCALA_VERSION}"
        sh "./change-cuda-versions.sh ${CUDA_VERSION}"

        configFileProvider(
                [configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')
                ]) {
                      if (!TESTS) {
                        docker.image('ubuntu14cuda80').inside(dockerParams) {
                            sh'''
                            mvn -B -s ${MAVEN_SETTINGS} clean deploy -DskipTests
                            '''
                        }
                      }
                      else {
                        docker.image('ubuntu14cuda80').inside(dockerParams) {
                            sh'''
                            mvn -B -s ${MAVEN_SETTINGS} clean deploy
                            '''
                        }
                      }
                   }
    }

    if (SONAR) {
           functions.sonar("${PROJECT}")
    }

/*
    sh "./change-scala-versions.sh 2.11"
    sh "./change-cuda-versions.sh 8.0"

    configFileProvider(
            [configFile(fileId: "${SETTINGS_XML}", variable: 'MAVEN_SETTINGS')
            ]) {
        sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests  ")
        // sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests  " + "-Denv.LIBND4J_HOME=/var/lib/jenkins/workspace/Pipelines/build_nd4j/libnd4j ")
    }
*/

}

// Messages for debugging
echo 'MARK: end of nd4j.groovy'
