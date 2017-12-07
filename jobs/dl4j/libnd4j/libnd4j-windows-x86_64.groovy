if (CBUILD.toBoolean()) {
    functions.get_project_code("${LIBPROJECT}")

    stash name: 'source'

    parallel(
            "Stream 0 ${LIBPROJECT}-CPU-${PLATFORM_NAME}": {
                dir("stream1") {
                    unstash 'source'

                    dir("${LIBPROJECT}") {
                        stage("${LIBPROJECT}-CPU-${PLATFORM_NAME}-test") {
                            dir('tests_cpu') {
                                String batCommand = [
                                        'cmake -G "Unix Makefiles"',
                                        'make -j4',
                                        'layers_tests\\runtests ' +
                                                '--gtest_output="xml:cpu_test_results.xml" ' +
                                                '--gtest_catch_exceptions=1'
                                ].join(' && ')

                                int exitCode = bat script: batCommand, returnStatus: true

                                // Check test results
                                if (exitCode == 0) {
                                    // Archiving test results
                                    junit '**/cpu_test_results.xml'
                                    stash includes: '**/cpu_test_results.xml', name: 'cpu-test-results'
                                } else {
                                    error "Test stage failed with exit code ${exitCode}."
                                }
                            }
                        }

                        stage("${LIBPROJECT}-CPU-${PLATFORM_NAME}-build") {
                            bat 'bash buildnativeoperations.sh -c cpu'

                            stash includes: 'blasbuild/cpu/blas/', name: 'cpu-blasbuild'
                            stash includes: 'blas/', name: 'cpu-blas'
                            stash includes: 'include/', name: 'libnd4j-include'
                        }
                    }
                }
            },
            "Stream 1 ${LIBPROJECT}-CUDA-8.0-${PLATFORM_NAME}": {
                dir("stream1") {
                    unstash 'source'

                    dir("${LIBPROJECT}") {
                        stage("${LIBPROJECT}-CUDA-8.0-${PLATFORM_NAME}") {
                            String batCommand = [
                                    'vcvars64.bat',
                                    'bash buildnativeoperations.sh -c cuda -v 8.0 %BUILD_CUDA_PARAMS%',
                                    'rmdir /s /q blasbuild\\cuda',
                                    'mklink /J blasbuild\\cuda blasbuild\\cuda-8.0'
                            ].join(' && ')

                            bat batCommand

                            stash includes: 'blasbuild/', name: 'cuda80-blasbuild'
                        }
                    }
                }
            },
            "Stream 2 ${LIBPROJECT}-CUDA-9.0-${PLATFORM_NAME}": {
                dir("stream2") {
                    unstash 'source'

                    dir("${LIBPROJECT}") {
                        stage("${LIBPROJECT}-CUDA-9.0-${PLATFORM_NAME}") {
                            String batCommand = [
                                    'vcvars64.bat',
                                    'bash buildnativeoperations.sh -c cuda -v 9.0 %BUILD_CUDA_PARAMS%',
                                    'rmdir /s /q blasbuild\\cuda',
                                    'mklink /J blasbuild\\cuda blasbuild\\cuda-9.0'
                            ].join(' && ')

                            bat batCommand

                            stash includes: 'blasbuild/', name: 'cuda90-blasbuild'
                        }
                    }
                }
            },
            failFast: false
    )

    dir("${LIBPROJECT}") {
        unstash 'cpu-test-results'
        unstash 'cpu-blasbuild'
        unstash 'cpu-blas'
        unstash 'libnd4j-include'
        unstash 'cuda80-blasbuild'
        unstash 'cuda90-blasbuild'

        if (PUSH_LIBND4J_LOCALREPO.toBoolean()) {
            functions.upload_libnd4j_snapshot_version_to_snapshot_repository(VERSION, PLATFORM_NAME, PROFILE_TYPE)
        }
    }

    if (SONAR.toBoolean()) {
        functions.sonar("${LIBPROJECT}")
    }
} else {
    echo "Skipping libnd4j build, using snapshot"
}

ansiColor('xterm') {
    echo "\033[42m MARK: end of libnd4j-windows-x86_64.groovy \033[0m"
}
