stage("${RL4J_PROJECT}-checkout-sources") {
    functions.get_project_code("${RL4J_PROJECT}")
}

stage("${RL4J_PROJECT}-build") {
    echo "Building ${RL4J_PROJECT} version ${VERSION}"
    dir("${RL4J_PROJECT}") {
        functions.checktag("${RL4J_PROJECT}")
        functions.verset("${VERSION}", true)
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
                mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID}  -DperformRelease=${GpgVAR} -Dmaven.test.skip=true
                '''
                        }
                    }

                    break

                case "linux-ppc64le":
                    if (SKIP_TEST.toBoolean()) {
                        docker.image(dockerImage).inside(dockerParams) {
                            functions.getGpg()
                            sh '''
                mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID} -DperformRelease=${GpgVAR}
                '''
                        }
                    } else {
                        docker.image(dockerImage).inside(dockerParams) {
                            functions.getGpg()
                            sh '''
              mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID} -DperformRelease=${GpgVAR} -Dmaven.test.skip=true
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
        functions.sonar("${RL4J_PROJECT}")
    }
}

echo 'MARK: end of rl4j.groovy'
