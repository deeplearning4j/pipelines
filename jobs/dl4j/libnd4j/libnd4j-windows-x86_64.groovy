stage("${LIBPROJECT}-build") {
    if (CBUILD.toBoolean()) {
        functions.get_project_code("${LIBPROJECT}")
        dir("libnd4j") {
            parallel(
                    "Stream 1 Build CPU": {
                        dir("stream1") {
                            functions.get_project_code("${LIBPROJECT}")
                            dir("${LIBPROJECT}") {
                                bat 'bash buildnativeoperations.sh'
                                stash includes: 'blasbuild/cpu/blas/', name: 'cpu-blasbuild'
                                stash includes: 'blas/', name: 'cpu-blas'
                                stash includes: 'include/', name: 'libnd4j-include'
                            }
                        }
                    },
                    "Stream 2 Build CUDA 8.0": {
                        dir("stream2") {
                            functions.get_project_code("${LIBPROJECT}")
                            dir("${LIBPROJECT}") {
                                bat '''
                    vcvars64.bat && bash buildnativeoperations.sh -c cuda -v 8.0 %BUILD_CUDA_PARAMS% && rmdir /s /q blasbuild\\cuda && mklink /J blasbuild\\cuda blasbuild\\cuda-8.0
                    '''
                                stash includes: 'blasbuild/', name: 'cuda80-blasbuild'
                            }
                        }
                    },
                    "Stream 3 Build CUDA 9.0": {
                        dir("stream3") {
                            functions.get_project_code("${LIBPROJECT}")
                            dir("${LIBPROJECT}") {
                                bat '''
                    vcvars64.bat && bash buildnativeoperations.sh -c cuda -v 9.0 %BUILD_CUDA_PARAMS% && rmdir /s /q blasbuild\\cuda && mklink /J blasbuild\\cuda blasbuild\\cuda-9.0
                    '''
                                stash includes: 'blasbuild/', name: 'cuda90-blasbuild'
                            }
                        }
                    }
            )
            unstash 'cpu-blasbuild'
            unstash 'cpu-blas'
            unstash 'cuda80-blasbuild'
            unstash 'cuda90-blasbuild'
            unstash 'libnd4j-include'

            if ( PUSH_LIBND4J_LOCALREPO.toBoolean() ) {
                functions.upload_libnd4j_snapshot_version_to_snapshot_repository(VERSION, PLATFORM_NAME, PROFILE_TYPE)
            }
        }
    }

    if (SONAR.toBoolean()) {
        functions.sonar("${LIBPROJECT}")
    }
}

ansiColor('xterm') {
    echo "\033[42m MARK: end of libnd4j-windows-x86_64.groovy \033[0m"
}
