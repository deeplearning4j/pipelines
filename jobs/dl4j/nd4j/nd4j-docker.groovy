//functions.resolve_dependencies_for_nd4j()
// linux-x86_64 android-arm android-x86 linux-ppc64le macosx-x86_64 windows-x86_64

stage("${PROJECT}-ResolveDependencies") {
    switch ("${PLATFORM_NAME}") {
        case "linux-x86_64":
        case "android-arm":
        case "android-x86":
        case "linux-ppc64le":
            echo('[ INFO ] PLATFORM_NAME Value set to: ' + "${PLATFORM_NAME}")
            echo('[ INFO ] Current build will be executed inside docker container')
            docker.image(dockerImage).inside(dockerParams) {
                functions.resolve_dependencies_for_nd4j()
            }
            break
        case "macosx-x86_64":
        case "windows-x86_64":
            echo('[ INFO ] PLATFORM_NAME Value set to:' + "${PLATFORM_NAME}")
            echo('[ INFO ] Current build will be executed under platform shell')
            functions.resolve_dependencies_for_nd4j()
            break
        default:
            error("Platform name is not defined or unsupported")
            break
    }
}

stage("${PROJECT}-checkout-sources") {
    functions.get_project_code("${PROJECT}")
}



stage("${PROJECT}-build") {
    dir("${PROJECT}") {
        functions.verset("${VERSION}", true)
        env.LIBND4J_HOME = "${WORKSPACE}/libnd4j"

        final nd4jlibs = [[cudaVersion: "7.5", scalaVersion: "2.10"],
                          [cudaVersion: "8.0", scalaVersion: "2.11"]]

        for (lib in nd4jlibs) {
            env.CUDA_VERSION = lib.cudaVersion
            env.SCALA_VERSION = lib.scalaVersion
            echo "[ INFO ] ++ Building nd4j with cuda " + lib.cudaVersion + " and scala " + lib.scalaVersion
            if (isUnix()) {
//                sh("ln -s ${WORKSPACE}/${LIBPROJECT}/blasbuild/cuda-${CUDA_VERSION} ${WORKSPACE}/${LIBPROJECT}/blasbuild/cuda")
                sh("if [ -L ${WORKSPACE}/${LIBPROJECT}/blasbuild/cuda ] ; then rm -f ${WORKSPACE}/${LIBPROJECT}/blasbuild/cuda && ln -s ${WORKSPACE}/${LIBPROJECT}/blasbuild/cuda-${CUDA_VERSION} ${WORKSPACE}/${LIBPROJECT}/blasbuild/cuda ; else  ln -s ${WORKSPACE}/${LIBPROJECT}/blasbuild/cuda-${CUDA_VERSION} ${WORKSPACE}/${LIBPROJECT}/blasbuild/cuda ; fi")
                sh("./change-scala-versions.sh ${SCALA_VERSION}")
                sh("./change-cuda-versions.sh ${CUDA_VERSION}")
            } else {
                bat("IF EXIST ${WORKSPACE}\\${LIBPROJECT}\\blasbuild\\cuda (RD /q /s ${WORKSPACE}\\${LIBPROJECT}\\blasbuild\\cuda && XCOPY /E /I /Q ${WORKSPACE}\\${LIBPROJECT}\\blasbuild\\cuda-${CUDA_VERSION} ${WORKSPACE}\\${LIBPROJECT}\\blasbuild\\cuda) ELSE ( XCOPY /E /I /Q ${WORKSPACE}\\${LIBPROJECT}\\blasbuild\\cuda-${CUDA_VERSION} ${WORKSPACE}\\${LIBPROJECT}\\blasbuild\\cuda )")
                bat("""bash
./change-scala-versions.sh ${SCALA_VERSION}""")
                bat("bash change-cuda-versions.sh ${CUDA_VERSION}")
            }
            configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
                switch (PLATFORM_NAME) {
                    case "linux-x86_64":
                    case "linux-ppc64le":
                        docker.image(dockerImage).inside(dockerParams) {
                            functions.getGpg()
                            sh '''
                                gpg --list-keys
                                if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                                mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID} -DperformRelease=${GpgVAR} -Dmaven.test.skip=${TESTS}
                                '''
                        }
                        break
                    case "android-arm":
                    case "android-x86":
                        docker.image(dockerImage).inside(dockerParams) {
                            functions.getGpg()

                            sh '''
 gpg --list-keys
env
                              if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                              mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID} -DperformRelease=${GpgVAR} -Dmaven.test.skip=${TESTS} -pl '!:nd4j-cuda-${CUDA_VERSION},!:nd4j-cuda-${CUDA_VERSION}-platform'
                              '''
                        }
                        break
                    case "macosx-x86_64":
                        functions.getGpg()
                        sh '''
                              mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID} -DperformRelease=${GpgVAR} -Dmaven.test.skip=${TESTS} -pl '!:nd4j-cuda-${CUDA_VERSION},!:nd4j-cuda-${CUDA_VERSION}-platform'
                              '''
                        break
                    case "windows-x86_64":
                        functions.getGpg()
                        bat("mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID} -DperformRelease=${GpgVAR} -Dmaven.test.skip=${TESTS} ")

                    default:
                        break
                }
            }
        }

        if (SONAR.toBoolean()) {
            functions.sonar("${PROJECT}")
        }
    }
}

echo 'MARK: end of nd4j.groovy'

