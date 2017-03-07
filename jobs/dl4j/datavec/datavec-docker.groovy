stage("${DATAVEC_PROJECT}-checkout-sources") {
    functions.get_project_code("${DATAVEC_PROJECT}")
}

stage("${DATAVEC_PROJECT}-build") {

    echo "Building ${DATAVEC_PROJECT} version ${VERSION}"

    dir("${DATAVEC_PROJECT}") {
        functions.checktag("${DATAVEC_PROJECT}")
        functions.verset("${VERSION}", true)
        //sh "sed -i 's/<nd4j.version>.*<\\/nd4j.version>/<nd4j.version>${VERSION}<\\/nd4j.version>/' pom.xml"

        // Below FOR loop is required per needs to consloidate Cuda and Scala Version and contain hard coded values
        def listScalaVersion = ["2.10", "2.11"]
//        def listCudaVersion = ["7.5", "8.0"]

        for (int i = 0; i < listScalaVersion.size(); i++) {
            echo "[ INFO ] ++ SET Scala Version to: " + listScalaVersion[i]
            env.SCALA_VERSION = listScalaVersion[i]

            sh "./change-scala-versions.sh ${SCALA_VERSION}"


            configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
                switch (PLATFORM_NAME) {
                    case "linux-x86_64":
                        if (TESTS.toBoolean()) {
                            docker.image(dockerImage).inside(dockerParams) {
                                sh '''
                    mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID} 
                    '''
                            }
                        } else {
                            docker.image(dockerImage).inside(dockerParams) {
                                sh '''
                    mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID} -Dmaven.test.skip=true
                    '''
                            }
                        }
                        break
                    case "linux-ppc64le":
                        if (TESTS.toBoolean()) {
                            docker.image(dockerImage).inside(dockerParams) {
                                sh '''
                    mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID} 
                    '''
                            }
                        } else {
                            docker.image(dockerImage).inside(dockerParams) {
                                sh '''
                    mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID} -Dmaven.test.skip=true
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
        functions.sonar("${DATAVEC_PROJECT}")
    }
}

echo 'MARK: end of datavec.groovy'
