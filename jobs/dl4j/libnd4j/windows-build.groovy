node ('windows-slave') {
    git 'https://github.com/deeplearning4j/libnd4j.git'
    dir("libnd4j") {
        parallel (
            "Stream 1 Build CPU" : {
                dir("stream1") {
                    bat 'bash pwd'
                    stash includes: 'blasbuild/cpu/blas/', name: 'cpu-blasbuild'
                    stash includes: 'blas/', name: 'cpu-blas'
                    stash includes: 'include/', name: 'libnd4j-include'
                }
            },    
            "Stream 2 Build CUDA 7.5" : {
                dir("stream2") {
                    bat '''
                    "C:\\Program Files (x86)\\Microsoft Visual Studio 12.0\\VC\\bin\\amd64\\vcvars64.bat" && bash ../buildnativeoperations.sh -c cuda -v 7.5
                    '''    
                    stash includes: 'blasbuild/cuda-7.5/blas/', name: 'cuda75-blasbuild'
                    stash includes: 'blas/', name: 'cuda75-blas'
                }
            },
            "Stream 3 Build CUDA 8.0" : {
                dir("stream3") {
                    bat '''
                    "C:\\Program Files (x86)\\Microsoft Visual Studio 12.0\\VC\\bin\\amd64\\vcvars64.bat" && bash ../buildnativeoperations.sh -c cuda -v 8.0
                    '''
                    stash includes: 'blasbuild/cuda-8.0/blas/', name: 'cuda80-blasbuild'
                    stash includes: 'blas/', name: 'cuda80-blas'
                }    
            }
        )
    
    unstash 'cpu-blasbuild'
    unstash 'cpu-blas'
    unstash 'cuda75-blasbuild'
    unstash 'cuda75-blas'
    unstash 'cuda80-blasbuild'
    unstash 'cuda80-blas'
    unstash 'libnd4j-include'
    }
}