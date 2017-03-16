stage("${LIBPROJECT}-build") {
    if (CBUILD.toBoolean()) {
        functions.get_project_code("${LIBPROJECT}")
        parallel(
                "Stream 0 ${LIBPROJECT}-Build-CPU-${PLATFORM_NAME}": {
                    dir("stream0") {
                        sh("cp -a ${WORKSPACE}/${LIBPROJECT} ./")
                        dir("${LIBPROJECT}") {
                            // env.TRICK_NVCC = "YES"
                            env.LIBND4J_HOME = "${PWD}"

                            sh '''
                              if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                              ./buildnativeoperations.sh
                              '''
                            stash includes: 'blasbuild/cpu/blas/', name: 'osx-cpu-blasbuild'
                            stash includes: 'blas/', name: 'osx-cpu-blas'
                        }
                    }
                },
                "Stream 1 ${LIBPROJECT}-Build-Cuda-${PLATFORM_NAME}": {
                    dir("stream2") {
                        sh("cp -a ${WORKSPACE}/${LIBPROJECT} .\\")
                        dir("${LIBPROJECT}") {
                            // env.TRICK_NVCC = "YES"
                            env.LIBND4J_HOME = "${PWD}"

                            sh '''
                              ./buildnativeoperations.sh -c cuda -сс macosx
                              '''
                            stash includes: 'blasbuild/cuda/blas/', name: 'osx-cuda-blasbuild'
                            stash includes: 'blas/', name: 'osx-cuda-blas'

                        }
                    }
                }
        )

        dir("libnd4j") {
            unstash 'osx-cpu-blasbuild'
            unstash 'osx-cpu-blas'
            unstash 'osx-cuda-blasbuild'
            unstash 'osx-cuda-blas'
        }
    }
}

echo 'MARK: end of libnd4j.groovy'