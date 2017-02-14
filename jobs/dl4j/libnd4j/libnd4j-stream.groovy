stage("${LIBPROJECT}-Build-Parallel-withDocker") {
    parallel (
        "Stream 0 ${LIBPROJECT}-BuildCuda-CPU" : {
            dir("stream0") {

                functions.get_project_code("${LIBPROJECT}")

                dir("${LIBPROJECT}") {
                    env.TRICK_NVCC = "YES"
                    env.LIBND4J_HOME = "${PWD}"
                    stage("Building CPU lib ubuntu14") {
                        docker.image('ubuntu14cuda80').inside(dockerParams) {
                            sh '''
                            ./buildnativeoperations.sh -c cpu
                            '''
                            // stash includes: 'blasbuild/cpu/blas/libnd4jcpu.so', name: 'cpu'
                            stash includes: 'blasbuild/cpu/blas/', name: 'cpu-blasbuild'
                            stash includes: 'blas/', name: 'cpu-blas'
                            stash includes: 'include/', name: 'libnd4j-include'
                        }
                    }
                }
            }
        },
        /*
        "Stream 1 ${LIBPROJECT}-BuildCuda-7.5" : {
            dir("stream1") {
                functions.get_project_code("${LIBPROJECT}")
                dir("${LIBPROJECT}") {
                    env.TRICK_NVCC = "YES"
                    env.LIBND4J_HOME = "${PWD}"
                    sh ("for i in `ls -la /tmp/ | grep jenkins | awk  -v env_var=\"${USER}\"  '\$3== env_var {print}' | awk '{print \$9}'`; do rm -rf \${i}; done")
                    stage("Building CUDA 7.5 lib ubuntu14") {
                        docker.image('ubuntu14cuda75').inside(dockerParams) {
                            sh '''
                            ./buildnativeoperations.sh -c cuda -v 7.5
                            '''
                            // stash includes: 'blasbuild/cuda-7.5/blas/libnd4jcuda.so', name: 'cuda75'
                            stash includes: 'blasbuild/cuda-7.5/blas/', name: 'cuda75-blasbuild'
                            stash includes: 'blas/', name: 'cuda75-blas'
                        }
                    }
                }
            }
        },
        */

        "Stream 2 ${LIBPROJECT}-BuildCuda-8.0" : {
            dir("stream2") {
                functions.get_project_code("${LIBPROJECT}")
                dir("${LIBPROJECT}") {
                    env.TRICK_NVCC = "YES"
                    env.LIBND4J_HOME = "${PWD}"
                    sh ("for i in `ls -la /tmp/ | grep jenkins | awk  -v env_var=\"${USER}\"  '\$3== env_var {print}' | awk '{print \$9}'`; do rm -rf \${i}; done")
                    stage("Building CUDA 8.0 lib ubuntu14") {
                        docker.image('ubuntu14cuda75').inside(dockerParams) {
                            sh '''
                            ./buildnativeoperations.sh -c cuda -v 8.0
                            '''
                            // stash includes: 'blasbuild/cuda-7.5/blas/libnd4jcuda.so', name: 'cuda75'
                            stash includes: 'blasbuild/cuda-7.5/blas/', name: 'cuda75-blasbuild'
                            stash includes: 'blas/', name: 'cuda75-blas'
                        }
                    }
                }
            }
        }

    )

    dir("libnd4j") {
        unstash 'cpu-blasbuild'
        unstash 'cpu-blas'
        unstash 'libnd4j-include'
        // unstash 'cuda75-blasbuild'
        // unstash 'cuda75-blas'

    }

    // if(SONAR) {
    //   functions.sonar("${LIBPROJECT}")
    // }
}
