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
        echo 'Set Project Version'
        // sh("'mvn' versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=${VERSION}")
        functions.verset("${VERSION}", true)

        def listScalaVersion = ["2.10", "2.11"]
        def listCudaVersion = ["7.5", "8.0"]

        for (int i = 0; i < listScalaVersion.size(); i++) {
            echo "[ INFO ] ++ SET Scala Version to: " + listScalaVersion[i]
            env.SCALA_VERSION = listScalaVersion[i]
            echo "[ INFO ] ++ SET Cuda Version to: " + listCudaVersion[i]
            env.CUDA_VERSION = listCudaVersion[i];

            bat'''
            bash change-scala-versions.sh ${SCALA_VERSION}
            bash change-cuda-versions.sh ${CUDA_VERSION}
            '''

            configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
                if (TESTS) {

                        bat
                        '''
                            mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dmaven.deploy.skip=flase  \
                            -Dlocal.software.repository=${PROFILE_TYPE}
                        '''
                } else {
                        bat
                        '''
                            mvn -B -s ${MAVEN_SETTINGS} clean deploy -DskipTests -Dmaven.deploy.skip=flase \
                            -Dlocal.software.repository=${PROFILE_TYPE}
                        '''
                }
            }
        }
    }
}

echo 'MARK: end of nd4j.groovy'
