if (CBUILD.toBoolean()) {
    functions.get_project_code("${LIBPROJECT}")

    // Workaround to fetch the latest docker image
    for (imageName in dockerImages.values()) {
        docker.image(imageName).pull()
    }

    // Workaround to store values from map in parallel step
    String dockerImageName = ''

    parallel(
            "Stream 0 ${LIBPROJECT}-CPU-${PLATFORM_NAME}": {
                dir("stream0") {
                    sh("cp -a ${WORKSPACE}/${LIBPROJECT} ./")

                    dir("${LIBPROJECT}") {
                        dockerImageName = dockerImages.ubuntu16cuda80

                        docker.image(dockerImageName).inside(dockerParams) {
                            stage("${LIBPROJECT}-CPU-${PLATFORM_NAME}-test") {
                                sh '''\
                                    if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                                    cd ./tests_cpu && cmake -G "Unix Makefiles" && make -j4 && \
                                    ./layers_tests/runtests --gtest_output="xml:cpu_test_results.xml" || true
                                '''.stripIndent()

                                stash includes: '**/cpu_test_results.xml', name: 'cpu-test-results'
                            }

                            stage("${LIBPROJECT}-CPU-${PLATFORM_NAME}-build") {
                                sh './buildnativeoperations.sh -c cpu'

                                stash includes: 'blasbuild/cpu/blas/', name: 'cpu-blasbuild'
                                stash includes: 'blas/', name: 'cpu-blas'
                            }
                        }
                    }
                }
            },
            "Stream 1 ${LIBPROJECT}-CUDA-8.0-${PLATFORM_NAME}": {
                dir("stream1") {
                    sh("cp -a ${WORKSPACE}/${LIBPROJECT} ./")

                    dir("${LIBPROJECT}") {
                        dockerImageName = dockerImages.ubuntu16cuda80

                        docker.image(dockerImageName).inside(dockerParams) {
                            stage("${LIBPROJECT}-CUDA-8.0-${PLATFORM_NAME}") {
                                sh './buildnativeoperations.sh -c cuda -v 8.0 ${BUILD_CUDA_PARAMS}'

                                stash includes: 'blasbuild/cuda-8.0/blas/', name: 'cuda80-blasbuild'
                                stash includes: 'blas/', name: 'cuda80-blas'
                            }
                        }
                    }
                }
            },
            "Stream 2 ${LIBPROJECT}-CUDA-9.0-${PLATFORM_NAME}": {
                dir("stream2") {
                    sh("cp -a ${WORKSPACE}/${LIBPROJECT} ./")

                    dir("${LIBPROJECT}") {
                        dockerImageName = dockerImages.ubuntu16cuda90

                        docker.image(dockerImageName).inside(dockerParams) {
                            stage("${LIBPROJECT}-CUDA-9.0-${PLATFORM_NAME}") {
                                sh './buildnativeoperations.sh -c cuda -v 9.0 ${BUILD_CUDA_PARAMS}'

                                stash includes: 'blasbuild/cuda-9.0/blas/', name: 'cuda90-blasbuild'
                                stash includes: 'blas/', name: 'cuda90-blas'
                            }
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
            dockerImageName = dockerImages.ubuntu16cuda80

            docker.image(dockerImageName).inside(dockerParams) {
                functions.upload_libnd4j_snapshot_version_to_snapshot_repository(VERSION, PLATFORM_NAME, PROFILE_TYPE)
            }
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
    echo "\033[42m MARK: end of libnd4j-linux-ppc64le.groovy \033[0m"
}
