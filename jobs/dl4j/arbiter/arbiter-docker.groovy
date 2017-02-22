stage("${ARBITER_PROJECT}-checkout-sources") {
    functions.get_project_code("${ARBITER_PROJECT}")
}

stage("${ARBITER_PROJECT}-build") {
    echo "Releasing ${ARBITER_PROJECT} version ${RELEASE_VERSION}"
    dir("${ARBITER_PROJECT}") {

        functions.checktag("${ARBITER_PROJECT}")

        functions.verset("${RELEASE_VERSION}", true)

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
                        if (TESTS.toBoolean()) {
                            docker.image(dockerImage).inside(dockerParams) {
                                sh '''
                      mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dnd4j.version=${ND4J_VERSION} \
                      -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION} -Dmaven.deploy.skip=false -Dlocal.software.repository=${PROFILE_TYPE}
                      '''
                            }
                        } else {
                            docker.image(dockerImage).inside(dockerParams) {
                                sh '''
                      mvn -B -s ${MAVEN_SETTINGS} clean deploy -DskipTests -Dmaven.test.skip \
                      -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION} \
                      -Dmaven.deploy.skip=false -Dlocal.software.repository=${PROFILE_TYPE}
                      '''
                            }
                        }
                        break
                    case "linux-ppc64le":
                        if (TESTS.toBoolean()) {
                            docker.image(dockerImage).inside(dockerParams) {
                                sh '''
                      mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dnd4j.version=${ND4J_VERSION} \
                      -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION} -Dmaven.deploy.skip=false -Dlocal.software.repository=${PROFILE_TYPE}
                      '''
                            }
                        } else {
                            docker.image(dockerImage).inside(dockerParams) {
                                sh '''
                      mvn -B -s ${MAVEN_SETTINGS} clean deploy -DskipTests -Dmaven.test.skip \
                      -Dnd4j.version=${ND4J_VERSION} -Ddatavec.version=${DATAVEC_VERSION} -Ddl4j.version=${DL4J_VERSION} \
                      -Dmaven.deploy.skip=false -Dlocal.software.repository=${PROFILE_TYPE}
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
