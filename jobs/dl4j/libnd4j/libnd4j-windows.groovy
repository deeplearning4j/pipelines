dir("${LIBPROJECT}") {
    parallel (
        "Stream 1 Build CPU" : {
            dir("stream1") {
                git 'https://github.com/deeplearning4j/libnd4j.git'
                bat 'bash buildnativeoperations.sh'
                stash includes: 'blasbuild/cpu/blas/', name: 'cpu-blasbuild'
                stash includes: 'blas/', name: 'cpu-blas'
                stash includes: 'include/', name: 'libnd4j-include'
            }
        },
        "Stream 2 Build CUDA 7.5" : {
            dir("stream2") {
                git 'https://github.com/deeplearning4j/libnd4j.git'
                bat '''
                vcvars64.bat && bash buildnativeoperations.sh -c cuda -v 7.5
                '''
                stash includes: 'blasbuild/cuda-7.5/blas/', name: 'cuda75-blasbuild'
            }
        },
        "Stream 3 Build CUDA 8.0" : {
            dir("stream3") {
                git 'https://github.com/deeplearning4j/libnd4j.git'
                bat '''
                vcvars64.bat && bash buildnativeoperations.sh -c cuda -v 8.0
                '''
                stash includes: 'blasbuild/cuda-8.0/blas/', name: 'cuda80-blasbuild'
            }
        }
    )
    unstash 'cpu-blasbuild'
    unstash 'cpu-blas'
    unstash 'cuda75-blasbuild'
    unstash 'cuda80-blasbuild'
    unstash 'libnd4j-include'
    env.TRICK_NVCC = "YES"
    env.LIBND4J_HOME = "${PWD}"
    functions.upload_libnd4j_snapshot_version_to_snapshot_repository(VERSION, PLATFORM_NAME, PROFILE_TYPE)
}