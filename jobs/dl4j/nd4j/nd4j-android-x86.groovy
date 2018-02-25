stage("${PROJECT}-Resolve-Dependencies") {
    docker.image(dockerImage).inside(dockerParams) {
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

        configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
            docker.image(dockerImage).inside(dockerParams + libnd4jHomeMount) {
                functions.getGpg()

                /* Workaround for protobuf */
                env.PROTOBUF_VERSION = '3.5.1'
                functions.fetchAndBuildProtobuf("${PROTOBUF_VERSION}")

                sh '''
                export GPG_TTY=$(tty)
                gpg --list-keys
                if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                mvn -U -B -PtrimSnapshots -s ${MAVEN_SETTINGS} clean deploy -Dmaven.repo.local=${HOME}/.m2/${PROFILE_TYPE}/repository -Djavacpp.platform=${PLATFORM_NAME} -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID} -Dgpg.useagent=false -DperformRelease=${GpgVAR} -Dmaven.test.skip=${SKIP_TEST} -pl '!nd4j-backends/nd4j-backend-impls/nd4j-cuda,!nd4j-backends/nd4j-backend-impls/nd4j-cuda-platform,!nd4j-backends/nd4j-tests' -DprotocCommand=protobuf-$PROTOBUF_VERSION/src/protoc
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
    echo "\033[42m MARK: end of nd4j-android-x86.groovy \033[0m"
}
