env.LIBBND4J_SNAPSHOT = env.LIBBND4J_SNAPSHOT ?: "${VERSION}"
env.CUDA_VERSION = env.CUDA_VERSION ?: "7.5"


stage("${PROJECT}-checkout-sources") {
    functions.get_project_code("${PROJECT}")
}

stage("${PROJECT}-build") {
    dir("${LIBPROJECT}/blasbuild") {
      bat returnStdout: true, script:
      '''
      mklink /J cuda cuda-${CUDA_VERSION}
      '''

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
        // sh("'mvn' versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=${VERSION}")
        functions.verset("${VERSION}", true)

        // def listScalaVersion = ["2.10", "2.11"]
        def listCudaVersion = ["7.5", "8.0"]

        for (int i = 0; i < listScalaVersion.size(); i++) {
            // echo "[ INFO ] ++ SET Scala Version to: " + listScalaVersion[i]
            // env.SCALA_VERSION = listScalaVersion[i]
            echo "[ INFO ] ++ SET Cuda Version to: " + listCudaVersion[i]
            env.CUDA_VERSION = listCudaVersion[i];

            sh("./change-scala-versions.sh ${SCALA_VERSION}")
            sh("./change-cuda-versions.sh ${CUDA_VERSION}")

            configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
                if (TESTS) {
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
    if (SONAR) {
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

echo 'MARK: end of nd4j.groovy'
