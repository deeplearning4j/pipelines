stage("${LIBPROJECT}-build-parallel-${PLATFORM_NAME}") {
  def dockerimage = dockerimage.toString()
  def dockerpars = dockerParameters.toString()
    parallel (
        "Stream 0 ${LIBPROJECT}-BuildCuda-CPU-${PLATFORM_NAME}" : {
            dir("stream0") {

                functions.get_project_code("${LIBPROJECT}")

                dir("${LIBPROJECT}") {
                    env.TRICK_NVCC = "YES"
                    env.LIBND4J_HOME = "${PWD}"
                    // stage("building CPU lib") {
                        docker.image("${dockerimage}").inside("${dockerpars}") {
                            sh '''
                            if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                            ./buildnativeoperations.sh -c cpu
                            '''
                            // stash includes: 'blasbuild/cpu/blas/libnd4jcpu.so', name: 'cpu'
                            stash includes: 'blasbuild/cpu/blas/', name: 'cpu-blasbuild'
                            stash includes: 'blas/', name: 'cpu-blas'
                            stash includes: 'include/', name: 'libnd4j-include'
                        }
                    // }
                }
            }
        },
        "Stream 1 ${LIBPROJECT}-BuildCuda-7.5-${PLATFORM_NAME}" : {
            dir("stream1") {
                functions.get_project_code("${LIBPROJECT}")
                dir("${LIBPROJECT}") {
                    env.TRICK_NVCC = "YES"
                    env.LIBND4J_HOME = "${PWD}"
                    sh ("for i in `ls -la /tmp/ | grep jenkins | awk  -v env_var=\"${USER}\"  '\$3== env_var {print}' | awk '{print \$9}'`; do rm -rf \${i}; done")
                    // stage("building CUDA 7.5 lib") {
                        docker.image("${dockerimage}").inside("${dockerpars}") {
                            sh '''
                            if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                            ./buildnativeoperations.sh -c cuda -v 7.5
                            '''
                            // stash includes: 'blasbuild/cuda-7.5/blas/libnd4jcuda.so', name: 'cuda75'
                            stash includes: 'blasbuild/cuda-7.5/blas/', name: 'cuda75-blasbuild'
                            stash includes: 'blas/', name: 'cuda75-blas'
                        }
                    // }
                }
            }
        },
        "Stream 2 ${LIBPROJECT}-BuildCuda-8.0-${PLATFORM_NAME}" : {
            dir("stream2") {
                functions.get_project_code("${LIBPROJECT}")
                dir("${LIBPROJECT}") {
                    env.TRICK_NVCC = "YES"
                    env.LIBND4J_HOME = "${PWD}"
                    sh ("for i in `ls -la /tmp/ | grep jenkins | awk  -v env_var=\"${USER}\"  '\$3== env_var {print}' | awk '{print \$9}'`; do rm -rf \${i}; done")
                    // stage("building CUDA 8.0 lib") {
                        docker.image("${dockerimage}").inside("${dockerpars}") {
                            sh '''
                            if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                            ./buildnativeoperations.sh -c cuda -v 8.0
                            '''
                            // stash includes: 'blasbuild/cuda-8.0/blas/libnd4jcuda.so', name: 'cuda80'
                            stash includes: 'blasbuild/cuda-8.0/blas/', name: 'cuda80-blasbuild'
                            stash includes: 'blas/', name: 'cuda80-blas'
                        }
                    // }
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
        unstash 'libnd4j-include'
    }

    // if(SONAR) {
    //   functions.sonar("${LIBPROJECT}")
    // }
}
