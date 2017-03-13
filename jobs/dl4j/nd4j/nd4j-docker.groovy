env.LIBBND4J_SNAPSHOT = env.LIBBND4J_SNAPSHOT ?: "${VERSION}"
env.CUDA_VERSION = env.CUDA_VERSION ?: "7.5"

dir("${LIBPROJECT}"){
    functions.resolve_dependencies_for_nd4j()
}



/*
dir("${LIBPROJECT}") {
    sh("find . -type f -name '*.so' | wc -l > ${WORKSPACE}/resultCountFile")
}
def varResultCountFile = readFile("${WORKSPACE}/resultCountFile").toInteger()
echo varResultCountFile.toString()


if (varResultCountFile == 0) {
    functions.get_project_code("${LIBPROJECT}")

    stage("${PROJECT}-resolve-dependencies") {

        dir("${LIBPROJECT}") {
            if ( PLATFORM_NAME == "linux-ppc64le" ) {
                sh ("rm -rf ${WORKSPACE}/libnd4j && cp -a /srv/jenkins/libnd4j ${WORKSPACE}/")
            } else {
                    docker.image(dockerImage).inside(dockerParams) {
                        configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
                    */
/**
 * HI MAN - this is HARD CODE for URL
 *//*

                        sh("mvn -B dependency:get -DrepoUrl=${NEXUS_LOCAL}/nexus/content/repositories/snapshots  \\\n" +
                                " -Dartifact=org.nd4j:${LIBPROJECT}:${LIBBND4J_SNAPSHOT}:tar \\\n" +
                                " -Dtransitive=false \\\n" +
                                " -Ddest=${LIBPROJECT}-${LIBBND4J_SNAPSHOT}.tar")
                        //
                        sh("tar -xvf ${LIBPROJECT}-${LIBBND4J_SNAPSHOT}.tar;")
                        sh("cd blasbuild && ln -s cuda-${CUDA_VERSION} cuda")
                    }
                }
            }
        }
    }
}



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