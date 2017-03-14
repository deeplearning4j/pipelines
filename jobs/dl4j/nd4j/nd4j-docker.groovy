//functions.resolve_dependencies_for_nd4j()
// linux-x86_64 android-arm android-x86 linux-ppc64le macosx-x86_64 windows-x86_64

switch ("${PLATFORM_NAME}") {
    case "linux-x86_64":
    case "android-arm":
    case "android-x86":
    case "linux-ppc64le":
        println("Value between: linux-x86_64 android-arm android-x86 linux-ppc64le " + PLATFORM_NAME)
        break
    case "macosx-x86_64":
    case "windows-x86_64":
        println("Value between: macosx-x86_64 windows-x86_64 and default " + PLATFORM_NAME)
        break
    default:
        break

}
/*docker.image(dockerImage).inside(dockerParams) {
// configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {

//    }
}*/

/*


stage("${PROJECT}-checkout-sources") {
    functions.get_project_code("${PROJECT}")
}

stage("${PROJECT}-build") {
    dir("${LIBPROJECT}/blasbuild") {
        sh("ln -s cuda-${CUDA_VERSION} cuda")
    }

    dir("${PROJECT}") {
        functions.verset("${VERSION}", true)
        env.LIBND4J_HOME="${WORKSPACE}/libnd4j"

        final nd4jlibs = [
            [
                cudaVersion: "7.5",
                scalaVersion: "2.10"
            ],
                [
                cudaVersion: "8.0",
                scalaVersion: "2.11"
            ]
        ]

        for (lib in nd4jlibs) {
            echo "[ INFO ] ++ Building nd4j with cuda " + lib.cudaVersion + " and scala " + lib.scalaVersion
            sh("./change-scala-versions.sh ${lib.scalaVersion}")
            sh("./change-cuda-versions.sh ${lib.cudaVersion}")
            configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
                switch(PLATFORM_NAME) {
                    case ["linux-x86_64", "linux-ppc64le"]:
                        if (TESTS.toBoolean()) {
                            docker.image(dockerImage).inside(dockerParams) {
                                functions.getGpg()
                                sh'''
                                gpg --list-keys
                                if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                                mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID} -DperformRelease=${GpgVAR}
                                '''
                            }
                        }
                        else {
                            docker.image(dockerImage).inside(dockerParams) {
                                functions.getGpg()
                                sh'''
                                gpg --list-keys
                                if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                                mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID} -DperformRelease=${GpgVAR} -Dmaven.test.skip=true
                                '''
                            }
                        }
                        break

                    case ["android-arm", "android-x86"]:
                        if (TESTS.toBoolean()) {
                          docker.image(dockerImage).inside(dockerParams) {
                              functions.getGpg()
                              sh'''
                              if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                              mvn clean install -Djavacpp.platform=${PLATFORM_NAME} -Dlocal.software.repository=${PROFILE_TYPE} -DskipTests -pl '!:nd4j-cuda-8.0,!:nd4j-cuda-8.0-platform'
                              '''
                          }
                        }
                        else {
                          docker.image(dockerImage).inside(dockerParams) {
                              functions.getGpg()
                              sh'''
                              if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                              mvn clean install -Djavacpp.platform=${PLATFORM_NAME} -Dlocal.software.repository=${PROFILE_TYPE} -DskipTests -pl '!:nd4j-cuda-8.0,!:nd4j-cuda-8.0-platform'
                              '''
                          }
                        }
                        break

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
*/
