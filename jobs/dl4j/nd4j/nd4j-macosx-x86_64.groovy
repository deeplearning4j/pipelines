stage("${PROJECT}-Resolve-Dependencies") {
    functions.resolve_dependencies_for_nd4j()
}

stage("${PROJECT}-checkout-sources") {
    functions.get_project_code("${PROJECT}")
}

stage("${PROJECT}-build") {
    dir("${PROJECT}") {
        functions.checktag("${PROJECT}")
        functions.verset("${VERSION}", true)

        final nd4jlibs = [[cudaVersion: "7.5", scalaVersion: "2.10"],
                          [cudaVersion: "8.0", scalaVersion: "2.11"]]

        for (lib in nd4jlibs) {
            env.CUDA_VERSION = lib.cudaVersion
            env.SCALA_VERSION = lib.scalaVersion
            echo "[ INFO ] ++ Building nd4j with cuda " + CUDA_VERSION + " and scala " + SCALA_VERSION
            sh("if [ -L ${WORKSPACE}/${LIBPROJECT}/blasbuild/cuda ] ; then rm -f ${WORKSPACE}/${LIBPROJECT}/blasbuild/cuda && ln -s ${WORKSPACE}/${LIBPROJECT}/blasbuild/cuda-${CUDA_VERSION} ${WORKSPACE}/${LIBPROJECT}/blasbuild/cuda ; else  ln -s ${WORKSPACE}/${LIBPROJECT}/blasbuild/cuda-${CUDA_VERSION} ${WORKSPACE}/${LIBPROJECT}/blasbuild/cuda ; fi")
            sh(script: "./change-scala-versions.sh ${SCALA_VERSION}")
            sh(script: "./change-cuda-versions.sh ${CUDA_VERSION}")
            configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
                functions.getGpg()
                sh '''
                                gpg --list-keys
                                if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                                mvn -U -B -s ${MAVEN_SETTINGS} clean deploy -Dmaven.repo.local=/Users/admin/.m2/${PROFILE_TYPE}/repository -Dscala.binary.version=${SCALA_VERSION} -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID} -DperformRelease=${GpgVAR} -Dmaven.test.skip=${SKIP_TEST}
                                '''
            }
        }

        // if (!isSnapshot) {
        if (PARENT_JOB.length() > 0) {
            functions.copy_nd4j_native_to_user_content()
        }
    }
    if (SONAR.toBoolean()) {
        functions.sonar("${PROJECT}")
    }
}

ansiColor('xterm') {
    echo "\033[42m MARK: end of nd4j-macosx-x86_64.groovy \033[0m"
}
