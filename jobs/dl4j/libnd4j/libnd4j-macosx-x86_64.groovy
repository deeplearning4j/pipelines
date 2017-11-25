if (CBUILD.toBoolean()) {
    functions.get_project_code("${LIBPROJECT}")

    parallel(
            "Stream 0 ${LIBPROJECT}-CPU-${PLATFORM_NAME}": {
                dir("stream0") {
                    sh("cp -a ${WORKSPACE}/${LIBPROJECT} ./")

                    dir("${LIBPROJECT}") {
                        stage("${LIBPROJECT}-CPU-${PLATFORM_NAME}-test") {
                            sh '''\
                                cd ./tests_cpu && cmake -G "Unix Makefiles" && make -j4 && \
                                ./layers_tests/runtests --gtest_output="xml:cpu_test_results.xml" || true
                            '''.stripIndent()

                            stash includes: '**/cpu_test_results.xml', name: 'cpu-test-results'
                        }

                        stage("${LIBPROJECT}-CPU-${PLATFORM_NAME}-build") {
                            // env.TRICK_NVCC = "YES"
                            env.LIBND4J_HOME = sh(returnStdout: true, script: "pwd").trim()

                            sh './buildnativeoperations.sh -c cpu'

                            stash includes: 'blasbuild/cpu/blas/', name: 'cpu-blasbuild'
                            stash includes: 'blas/', name: 'cpu-blas'
                        }
                    }
                }
            },
            "Stream 1 ${LIBPROJECT}-CUDA-8.0-${PLATFORM_NAME}": {
                dir("stream1") {
                    sh("cp -a ${WORKSPACE}/${LIBPROJECT} ./")

                    dir("${LIBPROJECT}") {
                        stage("${LIBPROJECT}-CUDA-8.0-${PLATFORM_NAME}") {
                            // env.TRICK_NVCC = "YES"
                            env.LIBND4J_HOME = sh(returnStdout: true, script: "pwd").trim()

                            sh './buildnativeoperations.sh -c cuda -v 8.0 ${BUILD_CUDA_PARAMS}'

                            stash includes: 'blasbuild/cuda/blas/', name: 'cuda80-blasbuild'
                            stash includes: 'blas/', name: 'cuda80-blas'
                        }
                    }
                }
            },
            "Stream 2 ${LIBPROJECT}-CUDA-9.0-${PLATFORM_NAME}": {
                dir("stream2") {
                    sh("cp -a ${WORKSPACE}/${LIBPROJECT} ./")

                    dir("${LIBPROJECT}") {
                        stage("${LIBPROJECT}-CUDA-9.0-${PLATFORM_NAME}") {
                            // env.TRICK_NVCC = "YES"
                            env.LIBND4J_HOME = sh(returnStdout: true, script: "pwd").trim()

                            sh './buildnativeoperations.sh -c cuda -v 9.0 ${BUILD_CUDA_PARAMS}'

                            stash includes: 'blasbuild/cuda/blas/', name: 'cuda90-blasbuild'
                            stash includes: 'blas/', name: 'cuda90-blas'
                        }
                    }
                }
            },
            failFast: true
    )

    dir("${LIBPROJECT}") {
        unstash 'cpu-blasbuild'
        unstash 'cpu-blas'
        unstash 'cpu-test-results'
        unstash 'cuda80-blasbuild'
        unstash 'cuda80-blas'
        unstash 'cuda90-blasbuild'
        unstash 'cuda90-blas'

        if (PUSH_LIBND4J_LOCALREPO.toBoolean()) {
            functions.upload_libnd4j_snapshot_version_to_snapshot_repository(VERSION, PLATFORM_NAME, PROFILE_TYPE)
        }

        // Archiving test results
        junit '**/cpu_test_results.xml'
    }

    if (SONAR.toBoolean()) {
        functions.sonar("${LIBPROJECT}")
    }
} else {
    echo "Skipping libnd4j build, using snapshot"
}

ansiColor('xterm') {
    echo "\033[42m MARK: end of libnd4j-macosx-x86_64.groovy \033[0m"
}
