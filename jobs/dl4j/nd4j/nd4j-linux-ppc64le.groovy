stage("${PROJECT}-Resolve-Dependencies") {
    // Workaround to fetch the latest docker image
    for (imageName in dockerImages.values()) {
        docker.image(imageName).pull()
    }

    docker.image(dockerImages.ubuntu16cuda80).inside(dockerParams) {
        functions.resolve_dependencies_for_nd4j()
    }
}

stage("${PROJECT}-checkout-sources") {
    functions.get_project_code("${PROJECT}")
}

stage("${PROJECT}-build") {
    dir("${PROJECT}") {
        functions.checktag("${PROJECT}")
        functions.verset("${VERSION}", true)
        /* Set LIBND4J_HOME environment with path to libn4j home folder */
        env.LIBND4J_HOME = ["${WORKSPACE}", "${LIBPROJECT}"].join('/')
        /*
            Mount point of libn4j home folder for Docker container.
            Because by default Jenkins mounts current working folder in Docker container, we need to add custom mount.
         */
        String libnd4jHomeMount = " -v ${LIBND4J_HOME}:${LIBND4J_HOME}:rw,z"

        final nd4jlibs = [
                [cudaVersion: "8.0", scalaVersion: "2.10"],
                [cudaVersion: "8.0", scalaVersion: "2.11"],
                [cudaVersion: "9.0", scalaVersion: "2.10"],
                [cudaVersion: "9.0", scalaVersion: "2.11"]
        ]

        for (lib in nd4jlibs) {
            env.CUDA_VERSION=lib.cudaVersion
            env.SCALA_VERSION=lib.scalaVersion
            // Get Docker image name
            Closure dockerImageName = { cudaVersion ->
                switch (cudaVersion) {
                    case '8.0':
                        return dockerImages.centos6cuda80
                        break
                    case '9.0':
                        return dockerImages.centos6cuda90
                        break
                    default:
                        error('CUDA version is not supported.')
                }
            }
            echo "[ INFO ] ++ Building nd4j with cuda " + CUDA_VERSION + " and scala " + SCALA_VERSION
            sh("if [ -L ${WORKSPACE}/${LIBPROJECT}/blasbuild/cuda ] ; then rm -f ${WORKSPACE}/${LIBPROJECT}/blasbuild/cuda && ln -s ${WORKSPACE}/${LIBPROJECT}/blasbuild/cuda-${CUDA_VERSION} ${WORKSPACE}/${LIBPROJECT}/blasbuild/cuda ; else  ln -s ${WORKSPACE}/${LIBPROJECT}/blasbuild/cuda-${CUDA_VERSION} ${WORKSPACE}/${LIBPROJECT}/blasbuild/cuda ; fi")
            sh(script: "./change-scala-versions.sh ${SCALA_VERSION}")
            sh(script: "./change-cuda-versions.sh ${CUDA_VERSION}")
            configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
                docker.image(dockerImageName("${CUDA_VERSION}")).inside(dockerParams + libnd4jHomeMount) {
                    functions.getGpg()
                    sh '''\
                        export GPG_TTY=$(tty)
                        gpg --list-keys
                        if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                        mvn -U -B -PtrimSnapshots -s ${MAVEN_SETTINGS} clean deploy -Dscala.binary.version=${SCALA_VERSION} -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID} -Dgpg.useagent=false -DperformRelease=${GpgVAR} -Dmaven.test.skip=${SKIP_TEST}
                    '''
                }
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
    echo "\033[42m MARK: end of nd4j-linux-ppc64le.groovy \033[0m"
}
