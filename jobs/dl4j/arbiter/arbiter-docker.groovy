stage("${ARBITER_PROJECT}-checkout-sources") {
    functions.get_project_code("${ARBITER_PROJECT}")
}

stage("${ARBITER_PROJECT}-build") {
    echo "Building ${ARBITER_PROJECT} version ${VERSION}"
    dir("${ARBITER_PROJECT}") {

        functions.checktag("${ARBITER_PROJECT}")

        functions.verset("${VERSION}", true)

        // Below FOR loop is required per needs to consloidate Cuda and Scala Version and contain hard coded values
        def listScalaVersion = ["2.10", "2.11"]
//        def listCudaVersion = ["7.5","8.0"]

        for (int i = 0; i < listScalaVersion.size(); i++) {
            echo "[ INFO ] ++ SET Scala Version to: " + listScalaVersion[i]
            env.SCALA_VERSION = listScalaVersion[i]
//            echo "[ INFO ] ++ SET Cuda Version to: " + listCudaVersion[i]
//            env.CUDA_VERSION = listCudaVersion[i] ;
//        }

            sh "./change-scala-versions.sh ${SCALA_VERSION}"



            configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
                switch (PLATFORM_NAME) {
                    case "linux-x86_64":
                        if (SKIP_TEST.toBoolean()) {
                            docker.image(dockerImage).inside(dockerParams) {
                                functions.getGpg()
                                sh '''
                      mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID}  -DperformRelease=${GpgVAR} 
                      '''
                            }
                        } else {
                            docker.image(dockerImage).inside(dockerParams) {
                                functions.getGpg()
                                sh '''
                      mvn -B -s ${MAVEN_SETTINGS} clean  deploy -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID}  -DperformRelease=${GpgVAR} -Dmaven.test.skip=true
                      '''
                            }
                        }
                        break
                    case "linux-ppc64le":
                        if (SKIP_TEST.toBoolean()) {
                            docker.image(dockerImage).inside(dockerParams) {
                                functions.getGpg()
                                sh '''
                      mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID}  -DperformRelease=${GpgVAR}
                      '''
                            }
                        } else {
                            docker.image(dockerImage).inside(dockerParams) {
                                functions.getGpg()
                                sh '''
                      mvn -B -s ${MAVEN_SETTINGS} clean  deploy -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID} -DperformRelease=${GpgVAR} -Dmaven.test.skip=true
                      '''
                            }
                        }
                        break
                    default:
                        break
                }
            }
        }
    }
    if (SONAR.toBoolean()) {
        functions.sonar("${ARBITER_PROJECT}")
    }
}

echo 'MARK: end of arbiter.groovy'
