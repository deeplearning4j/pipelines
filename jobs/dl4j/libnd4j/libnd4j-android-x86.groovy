if (CBUILD.toBoolean()) {
    functions.get_project_code("${LIBPROJECT}")

    // Workaround to fetch the latest docker image
    docker.image(dockerImage).pull()

    dir("${LIBPROJECT}") {
        docker.image(dockerImage).inside(dockerParams) {
            stage("${LIBPROJECT}-test") {
                String shellCommand = '''\
                    if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                    cd ./tests_cpu && cmake -G "Unix Makefiles" && make -j4 && \
                    ./layers_tests/runtests --gtest_output="xml:cpu_test_results.xml"
                '''.stripIndent()

                int exitCode = sh script: shellCommand, returnStatus: true

                // Check test results
                if (exitCode == 0) {
                    // Archiving test results
                    junit '**/cpu_test_results.xml'
                } else {
                    error "Test stage failed with exit code ${exitCode}."
                }
            }

            stage("${LIBPROJECT}-build") {
                sh '''\
                    if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                    ./buildnativeoperations.sh -platform ${PLATFORM_NAME}
                '''.stripIndent()
            }

            if (PUSH_LIBND4J_LOCALREPO.toBoolean()) {
                functions.upload_libnd4j_snapshot_version_to_snapshot_repository(VERSION, PLATFORM_NAME, PROFILE_TYPE)
            }
        }
    }

    if (SONAR.toBoolean()) {
        functions.sonar("${LIBPROJECT}")
    }
} else {
    echo "Skipping libnd4j build, using snapshot"
}

ansiColor('xterm') {
    echo "\033[42m MARK: end of libnd4j-android-x86.groovy \033[0m"
}
