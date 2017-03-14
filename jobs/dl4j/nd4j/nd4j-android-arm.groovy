env.CUDA_VERSION= env.CUDA_VERSION ?: "8.0"

stage("${PROJECT}-ResolveDependencies") {
    docker.image(dockerImage).inside(dockerParams) {
        functions.resolve_dependencies_for_nd4j()
    }
}


stage("${PROJECT}-checkout-sources") {
    functions.get_project_code("${PROJECT}")
}

stage("${PROJECT}-build") {
    dir("${PROJECT}") {
        functions.verset("${VERSION}", true)
//        env.LIBND4J_HOME = "${WORKSPACE}/libnd4j"

        sh("if [ -L ${WORKSPACE}/${LIBPROJECT}/blasbuild/cuda ] ; then rm -f ${WORKSPACE}/${LIBPROJECT}/blasbuild/cuda && ln -s ${WORKSPACE}/${LIBPROJECT}/blasbuild/cuda-${CUDA_VERSION} ${WORKSPACE}/${LIBPROJECT}/blasbuild/cuda ; else  ln -s ${WORKSPACE}/${LIBPROJECT}/blasbuild/cuda-${CUDA_VERSION} ${WORKSPACE}/${LIBPROJECT}/blasbuild/cuda ; fi")
        configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
            docker.image(dockerImage).inside(dockerParams) {
                functions.getGpg()
                sh("gpg --list-keys")
                sh ("if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi")
                sh ('''mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID} -DperformRelease=${GpgVAR} -Dmaven.test.skip=${TESTS} -pl '!:nd4j-cuda-8.0,!:nd4j-cuda-8.0-platform' ''')
            }
        }
    }
}
