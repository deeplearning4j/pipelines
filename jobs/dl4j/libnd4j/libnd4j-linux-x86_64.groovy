stage("${LIBPROJECT}-build") {
    if (CBUILD.toBoolean()) {
        functions.get_project_code("${LIBPROJECT}")
        parallel(
                "Stream 0 ${LIBPROJECT}-BuildCuda-CPU-${PLATFORM_NAME}": {
                    dir("stream0") {
                        sh("cp -a ${WORKSPACE}/${LIBPROJECT} ./")
                        dir("${LIBPROJECT}") {
                            docker.image(dockerImage).inside(dockerParams) {
                                sh '''
                                    if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                                    ./buildnativeoperations.sh -c cpu
                                    '''
                                stash includes: 'blasbuild/cpu/blas/', name: 'cpu-blasbuild'
                                stash includes: 'blas/', name: 'cpu-blas'
                            }
                        }
                    }
                },
                "Stream 1 ${LIBPROJECT}-BuildCuda-8.0-${PLATFORM_NAME}": {
                    dir("stream1") {
                        sh("cp -a ${WORKSPACE}/${LIBPROJECT} ./")
                        dir("${LIBPROJECT}") {
                            docker.image(dockerImage).inside(dockerParams) {
                                sh '''
                                if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                                ./buildnativeoperations.sh -c cuda -v 8.0 ${BUILD_CUDA_PARAMS}
                                '''
                                stash includes: 'blasbuild/cuda-8.0/blas/', name: 'cuda80-blasbuild'
                                stash includes: 'blas/', name: 'cuda80-blas'
                            }
                        }
                    }
                },
                "Stream 2 ${LIBPROJECT}-BuildCuda-7.5-${PLATFORM_NAME}": {
                    dir("stream2") {
                        sh("cp -a ${WORKSPACE}/${LIBPROJECT} ./")
                        dir("${LIBPROJECT}") {
                            // Hardcode here: override dockerImage as it make sense for linux-x86_64 only
                            if (PLATFORM_NAME == "linux-x86_64") {
                                dockerImage = "deeplearning4j-docker-registry.bintray.io/centos6cuda75:latest"
                            }
                            docker.image(dockerImage).inside(dockerParams) {
                                sh '''
                                    if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                                    ./buildnativeoperations.sh -c cuda -v 7.5 ${BUILD_CUDA_PARAMS}
                                    '''
                                stash includes: 'blasbuild/cuda-7.5/blas/', name: 'cuda75-blasbuild'
                                stash includes: 'blas/', name: 'cuda75-blas'
                            }
                        }
                    }
                }
        )
        dir("libnd4j") {
            unstash 'cpu-blasbuild'
            unstash 'cpu-blas'
            unstash 'cuda75-blasbuild'
            unstash 'cuda75-blas'
            unstash 'cuda80-blasbuild'
            unstash 'cuda80-blas'
        }

        if ( PUSH_LIBND4J_LOCALREPO.toBoolean() ) {
            docker.image(dockerImage).inside(dockerParams){
                functions.upload_libnd4j_snapshot_version_to_snapshot_repository(VERSION, PLATFORM_NAME, PROFILE_TYPE)
            }
        }

    } else {
        echo "Skipping libnd4j build, using snapshot"
    }

    if (SONAR.toBoolean()) {
        functions.sonar("${LIBPROJECT}")
    }
}

ansiColor('xterm') {
    echo "\033[42m MARK: end of libnd4j-linux-x86_64.groovy \033[0m"
}
