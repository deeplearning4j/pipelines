sh("env | grep LIBBND4J_SNAPSHOT | wc -l > ${WORKSPACE}/resultEnvFile")

def varResultEnvFile = readFile("${WORKSPACE}/resultEnvFile").toInteger()
if (varResultEnvFile == 0) {
    env.LIBBND4J_SNAPSHOT = "${RELEASE_VERSION}"
}

dir("${LIBPROJECT}") {
    sh("find . -type f -name '*.so' | wc -l > ${WORKSPACE}/resultCountFile")
}
def varResultCountFile = readFile("${WORKSPACE}/resultCountFile").toInteger()
echo varResultCountFile.toString()

if (varResultCountFile == 0) {
    functions.get_project_code("${LIBPROJECT}")

    stage("${PROJECT}-resolve-dependencies") {

        dir("${LIBPROJECT}") {
            configFileProvider(
                    [configFile(fileId: 'MAVEN_SETTINGS_DO-192', variable: 'MAVEN_SETTINGS')
                    ]) {
                sh("mvn -version")
                sh("tar -xvf `find ${WORKSPACE} -name ${LIBPROJECT}-${RELEASE_VERSION}.tar`")
                dir("blasbuild") {
                    sh("ln -s cuda-${CUDA_VERSION} cuda")
                }
            }
        }
    }
}
/*

stage("${PROJECT}-checkout-sources") {
    functions.get_project_code("${PROJECT}")
}

stage("${PROJECT}-build") {
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
                [configFile(fileId: 'MAVEN_SETTINGS_DO-192', variable: 'MAVEN_SETTINGS')
                ]) {
                      if (TESTS) {
                        docker.image(dockerImage).inside(dockerParams) {
                            sh'''
                            if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                            mvn -B -s ${MAVEN_SETTINGS} clean deploy
                            '''
                        }
                      }
                      else {
                        docker.image(dockerImage).inside(dockerParams) {
                            sh'''
                            if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                            mvn -B -s ${MAVEN_SETTINGS} clean deploy -DskipTests
                            '''
                        }
                      }
                   }

        if (SONAR) {
               functions.sonar("${PROJECT}")
        }
    }

*/
/*
    sh "./change-scala-versions.sh 2.11"
    sh "./change-cuda-versions.sh 8.0"

    configFileProvider(
            [configFile(fileId: 'MAVEN_SETTINGS_DO-192', variable: 'MAVEN_SETTINGS')
            ]) {
        sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests  ")
        // sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests  " + "-Denv.LIBND4J_HOME=/var/lib/jenkins/workspace/Pipelines/build_nd4j/libnd4j ")
    }
*//*


}
*/

// Messages for debugging
echo 'MARK: end of nd4j.groovy'
