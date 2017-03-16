    dir("libnd4j") {
        functions.get_project_code("${LIBPROJECT}")
        parallel (
                "Stream 1 Build CPU" : {
                    dir("stream1") {
                        functions.get_project_code("${LIBPROJECT}")
                        bat 'bash buildnativeoperations.sh'
                        stash includes: 'blasbuild/cpu/blas/', name: 'cpu-blasbuild'
                        stash includes: 'blas/', name: 'cpu-blas'
                        stash includes: 'include/', name: 'libnd4j-include'
                    }
                },
                "Stream 2 Build CUDA 7.5" : {
                    dir("stream2") {
                        functions.get_project_code("${LIBPROJECT}")
                        bat '''
                    vcvars64.bat && bash buildnativeoperations.sh -c cuda -v 7.5
                    '''
                        stash includes: 'blasbuild/cuda-7.5/blas/', name: 'cuda75-blasbuild'
                    }
                },
                "Stream 3 Build CUDA 8.0" : {
                    dir("stream3") {
                        functions.get_project_code("${LIBPROJECT}")
                        bat '''
                    vcvars64.bat && bash buildnativeoperations.sh -c cuda -v 8.0 && rmdir /s /q blasbuild\\cuda && mklink /J blasbuild\\cuda blasbuild\\cuda-8.0
                    '''
                        stash includes: 'blasbuild/', name: 'cuda80-blasbuild'
                    }
                }
        )
        unstash 'cpu-blasbuild'
        unstash 'cpu-blas'
        unstash 'cuda75-blasbuild'
        unstash 'cuda80-blasbuild'
        unstash 'libnd4j-include'
//    env.TRICK_NVCC = "YES"
        //  env.LIBND4J_HOME = "${PWD}"
        //   functions.upload_libnd4j_snapshot_version_to_snapshot_repository(VERSION, PLATFORM_NAME, PROFILE_TYPE)
    }

