env.LIBBND4J_SNAPSHOT = env.LIBBND4J_SNAPSHOT ?: "${VERSION}"
env.CUDA_VERSION = env.CUDA_VERSION ?: "7.5"


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
                    /**
                     * HI MAN - this is HARD CODE for URL
                     */
                        sh("mvn -B dependency:get -DrepoUrl=http://ec2-54-200-65-148.us-west-2.compute.amazonaws.com:8088/nexus/content/repositories/snapshots  \\\n" +
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
        // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
        // Temporary section - please remove once it commited updates to source code
        // configFileProvider(
        //         [configFile(fileId: 'MAVEN_POM_DO-192', variable: 'POM_XML')
        //     ]) {
        //     sh "cp ${POM_XML} pom.xml"
        // }
        // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
        // echo 'Set Project Version'
        //// sh("'mvn' versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=${VERSION}")

        functions.verset("${VERSION}", true)
        env.LIBND4J_HOME="${WORKSPACE}/libnd4j"

        def listScalaVersion = ["2.10", "2.11"]
        def listCudaVersion = ["7.5", "8.0"]

        for (int i = 0; i < listScalaVersion.size(); i++) {
            echo "[ INFO ] ++ SET Scala Version to: " + listScalaVersion[i]
            env.SCALA_VERSION = listScalaVersion[i]
            echo "[ INFO ] ++ SET Cuda Version to: " + listCudaVersion[i]
            env.CUDA_VERSION = listCudaVersion[i];

            sh("./change-scala-versions.sh ${SCALA_VERSION}")
            sh("./change-cuda-versions.sh ${CUDA_VERSION}")
            configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
                switch(PLATFORM_NAME) {
                    case "linux-x86_64":
                        if (TESTS.toBoolean()) {
                          docker.image(dockerImage).inside(dockerParams) {
                              sh'''
                              if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                              mvn -B -s ${MAVEN_SETTINGS} clean deploy
                              '''
                          }
                        }
                        else {
                            docker.image(dockerImage).inside(dockerParams) {
                                withCredentials([
                                file(credentialsId: 'gpg-pub-key-test-1', variable: 'GPG_PUBRING'),
                                file(credentialsId: 'gpg-private-key-test-1', variable: 'GPG_SECRING'),
                                usernameColonPassword(credentialsId: 'gpg-password-test-1', variable: 'GPG_PASS')]) {
                                    sh'''
                                    gpg --list-keys
                                    cp {$GPG_PUBRING,$GPG_SECRING} $HOME/.gnupg/
                                    gpg --list-keys
                                    if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                                    mvn -B -s ${MAVEN_SETTINGS} clean install -DskipTests
                                    '''
                                }
                            }
                        }
                        break

                    case "linux-ppc64le":
                        if (TESTS.toBoolean()) {
                            docker.image(dockerImage).inside(dockerParams) {
                                sh'''
                                if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                                mvn -B -s ${MAVEN_SETTINGS} clean deploy
                                '''
                          }
                        }
                        else {
                            withCredentials([
                                file(credentialsId: 'gpg-pub-key-test-1', variable: 'GPG_PUBRING'),
                                file(credentialsId: 'gpg-private-key-test-1', variable: 'GPG_SECRING'),
                                usernameColonPassword(credentialsId: 'gpg-password-test-1', variable: 'GPG_PASS')]) {
                                docker.image(dockerImage).inside(dockerParams) {
                                    sh'''
                                    gpg --list-keys
                                    cp {$GPG_PUBRING,$GPG_SECRING} $HOME/.gnupg/
                                    gpg --list-keys
                                    if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                                    #mvn -B -s ${MAVEN_SETTINGS} clean deploy -DskipTests
                                    mvn clean deploy -Dlocal.software.repository=${PROFILE_TYPE} -Dmaven.test.skip=true
                                    '''
                                }
                            }
                        }
                        break

                    case ["android-arm", "android-x86"]:
                        if (TESTS.toBoolean()) {
                          docker.image(dockerImage).inside(dockerParams) {
                              sh'''
                              if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                              mvn clean install -Djavacpp.platform=${PLATFORM_NAME} -Dlocal.software.repository=${PROFILE_TYPE} -DskipTests -pl '!:nd4j-cuda-8.0,!:nd4j-cuda-8.0-platform'
                              '''
                          }
                        }
                        else {
                          docker.image(dockerImage).inside(dockerParams) {
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
    }
    if (SONAR.toBoolean()) {
        functions.sonar("${PROJECT}")
    }

}
/*
    sh "./change-scala-versions.sh 2.11"
    sh "./change-cuda-versions.sh 8.0"

    configFileProvider(
            [configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')
            ]) {
        sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean install -DskipTests  ")
        // sh("'${mvnHome}/bin/mvn' -s ${MAVEN_SETTINGS} clean install -DskipTests  " + "-Denv.LIBND4J_HOME=/var/lib/jenkins/workspace/Pipelines/build_nd4j/libnd4j ")
    }
*//*


}
*/

echo 'MARK: end of nd4j.groovy'
