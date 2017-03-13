stage("${LIBPROJECT}-build") {
    sh("env")
    if(CBUILD.toBoolean()) {
        functions.get_project_code("${LIBPROJECT}")
        switch(PLATFORM_NAME) {
            case ["linux-x86_64", "linux-ppc64le"]:
                parallel (
                    "Stream 0 ${LIBPROJECT}-BuildCuda-CPU-${PLATFORM_NAME}" : {
                        dir("stream0") {
                            sh("cp -a ${WORKSPACE}/${LIBPROJECT} ./")
                            dir("${LIBPROJECT}") {
                                env.TRICK_NVCC = "YES"
                                env.LIBND4J_HOME = "${PWD}"

                                docker.image(dockerImage).inside(dockerParams) {
                                    sh '''
                                    if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                                    ./buildnativeoperations.sh -c cpu
                                    '''
                                    stash includes: 'blasbuild/cpu/blas/', name: 'cpu-blasbuild'
                                    stash includes: 'blas/', name: 'cpu-blas'
                                }
                            }
                        }
                    },
                    "Stream 1 ${LIBPROJECT}-BuildCuda-7.5-${PLATFORM_NAME}" : {
                        dir("stream1") {
                            sh("cp -a ${WORKSPACE}/${LIBPROJECT} ./")
                            dir("${LIBPROJECT}") {
                                env.TRICK_NVCC = "YES"
                                env.LIBND4J_HOME = "${PWD}"
                                sh ("for i in `ls -la /tmp/ | grep jenkins | awk  -v env_var=\"${USER}\"  '\$3== env_var {print}' | awk '{print \$9}'`; do rm -rf \${i}; done")

                                docker.image(dockerImage).inside(dockerParams) {
                                    sh '''
                                    if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                                    ./buildnativeoperations.sh -c cuda -v 7.5
                                    '''
                                    stash includes: 'blasbuild/cuda-7.5/blas/', name: 'cuda75-blasbuild'
                                    stash includes: 'blas/', name: 'cuda75-blas'
                                }
                            }
                        }
                    },
                    "Stream 2 ${LIBPROJECT}-BuildCuda-8.0-${PLATFORM_NAME}" : {
                        dir("stream2") {
                            sh("cp -a ${WORKSPACE}/${LIBPROJECT} ./")
                            dir("${LIBPROJECT}") {
                                env.TRICK_NVCC = "YES"
                                env.LIBND4J_HOME = "${PWD}"
                                sh ("for i in `ls -la /tmp/ | grep jenkins | awk  -v env_var=\"${USER}\"  '\$3== env_var {print}' | awk '{print \$9}'`; do rm -rf \${i}; done")

                                docker.image(dockerImage).inside(dockerParams) {
                                    sh '''
                                    if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                                    ./buildnativeoperations.sh -c cuda -v 8.0
                                    '''
                                    stash includes: 'blasbuild/cuda-8.0/blas/', name: 'cuda80-blasbuild'
                                    stash includes: 'blas/', name: 'cuda80-blas'
                                }
                            }
                        }
                    }
                )
                dir("libnd4j") {
                    unstash 'cpu-blasbuild'
                    unstash 'cpu-blas'
                    unstash 'cuda75-blasbuild'
                    unstash 'cuda75-blas'
                    unstash 'cuda80-blasbuild'
                    unstash 'cuda80-blas'
                }
                break

            case ["android-arm", "android-x86"]:
                dir("${LIBPROJECT}") {
                    env.TRICK_NVCC = "YES"
                    env.LIBND4J_HOME = "${PWD}"

                    docker.image(dockerImage).inside(dockerParams) {
                        sh '''
                        if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                        ./buildnativeoperations.sh -platform ${PLATFORM_NAME}
                        '''
                    }
                    docker.image(dockerImage).inside(dockerParams){
                        functions.putLibnd4j()
                    }
                }
                break

            default:
                break
        }

        // sh'''
        // mvn deploy:deploy-file \
        // -Durl=http://jenkins-master.eastus.cloudapp.azure.com:8088/nexus/content/repositories/snapshots \
        // -DgroupId=org.nd4j \
        // -DartifactId=${LIBPROJECT} \
        // -Dversion=${VERSION}  \
        // -Dpackaging=tar \
        // -DrepositoryId=local-nexus \
        // -Dclassifier=${PLATFORM_NAME}\
        // -Dfile=${LIBPROJECT}-${VERSION}-${PLATFORM_NAME}.tar
        // '''

        if(SONAR.toBoolean()) {
          functions.sonar("${LIBPROJECT}")
        }
    }
    else {
        env.LIBBND4J_SNAPSHOT = env.LIBBND4J_SNAPSHOT ?: "${VERSION}"
        env.CUDA_VERSION = env.CUDA_VERSION ?: "7.5"

        dir("${LIBPROJECT}") {
            sh("find . -type f -name '*.so' | wc -l > ${WORKSPACE}/resultCountFile")
        }
        def varResultCountFile = readFile("${WORKSPACE}/resultCountFile").toInteger()
        echo varResultCountFile.toString()

        if (varResultCountFile == 0) {
            functions.get_project_code("${LIBPROJECT}")

            dir("${LIBPROJECT}") {
                // if ( PLATFORM_NAME == "linux-ppc64le" ) {
                //     sh ("rm -rf ${WORKSPACE}/libnd4j && cp -a /srv/jenkins/libnd4j ${WORKSPACE}/")
                // }
                // else {
                    docker.image(dockerImage).inside(dockerParams) {
                        configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
                            sh("mvn -B -s ${MAVEN_SETTINGS} dependency:get -DrepoUrl=${NEXUS_LOCAL}/nexus/content/repositories/snapshots  \\\n" +
                                    " -Dartifact=org.nd4j:${LIBPROJECT}:${LIBBND4J_SNAPSHOT}:tar \\\n" +
                                    " -Dtransitive=false \\\n" +
                                    " -Ddest=${LIBPROJECT}-${VERSION}-${PLATFORM_NAME}.tar")
                            sh("tar -xvf ${LIBPROJECT}-${LIBBND4J_SNAPSHOT}-${PLATFORM_NAME}.tar;")
                        }
                    }
                // }
            }
        }
    }
}
