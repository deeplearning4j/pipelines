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
            docker.image(dockerImage).inside(dockerParams) {
                configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
                    /**
                     * HI MAN - this is HARD CODE for URL
                     */
                    sh("mvn -B dependency:get -DrepoUrl=http://ec2-54-200-65-148.us-west-2.compute.amazonaws.com:8088/nexus/content/repositories/snapshots  \\\n" +
                            " -Dartifact=org.nd4j:${LIBPROJECT}:${LIBBND4J_SNAPSHOT}:tar \\\n" +
                            " -Dtransitive=false \\\n" +
                            " -Ddest=${LIBPROJECT}-${LIBBND4J_SNAPSHOT}.tar")
                    //
                    sh("tar -xvf ${LIBPROJECT}-${LIBBND4J_SNAPSHOT}.tar;")
                    sh("cd blasbuild && ln -s cuda-${CUDA_VERSION} cuda")
                }
            }
        }
    }
}


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

        def listScalaVersion = ["2.10","2.11"]
        def listCudaVersion = ["7.5","8.0"]

        for (int i = 0; i < listScalaVersion.size(); i++) {
            echo "[ INFO ] ++ SET Scala Version to: " + listScalaVersion[i]
            def varScalaVersion = listScalaVersion[i]
            echo "[ INFO ] ++ SET Cuda Version to: " + listCudaVersion[i]
            def varCudaVersion = listCudaVersion[i];

//    sh("./change-scala-versions.sh ${SCALA_VERSION}")
//    sh("./change-cuda-versions.sh ${CUDA_VERSION}")
            sh("./change-scala-versions.sh ${varScalaVersion}")
            sh("./change-cuda-versions.sh ${varCudaVersion}")

            configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
                if (TESTS.toBoolean()) {
                    docker.image(dockerImage).inside(dockerParams) {
                        sh '''
                            if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                            mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dmaven.deploy.skip=flase  \
                            -Dlocal.software.repository=${PROFILE_TYPE}
                            '''
                    }
                } else {
                    docker.image(dockerImage).inside(dockerParams) {
                        sh '''
                            if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                            mvn -B -s ${MAVEN_SETTINGS} clean deploy -DskipTests -Dmaven.deploy.skip=flase \
                            -Dlocal.software.repository=${PROFILE_TYPE}
                            '''
                    }
                }
            }
        }
    }
    if (SONAR.toBoolean()) {
        functions.sonar("${PROJECT}")
    }

}
/*
    sh "./change-scala-versions.sh 2.11"
    sh "./change-cuda-versions.sh 8.0"

    configFileProvider(
            [configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')
            ]) {
        sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests  ")
        // sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean deploy -DskipTests  " + "-Denv.LIBND4J_HOME=/var/lib/jenkins/workspace/Pipelines/build_nd4j/libnd4j ")
    }
*//*


}
*/

// Messages for debugging
echo 'MARK: end of nd4j.groovy'
