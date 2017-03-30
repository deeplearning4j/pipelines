stage("${GYM_JAVA_CLIENT_PROJECT}-checkout-sources") {
    functions.get_project_code("${GYM_JAVA_CLIENT_PROJECT}")
}

stage("${GYM_JAVA_CLIENT_PROJECT}-build") {
    echo "Building ${GYM_JAVA_CLIENT_PROJECT} version ${VERSION}"
    dir("${GYM_JAVA_CLIENT_PROJECT}") {
        functions.checktag("${GYM_JAVA_CLIENT_PROJECT}")
        functions.verset("${VERSION}", true)
        configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
            docker.image(dockerImage).inside(dockerParams) {
                functions.getGpg()
                sh '''
                mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID} -DperformRelease=${GpgVAR} -Dmaven.test.skip=${SKIP_TEST}
                '''
            }
        }

    }
    if (SONAR.toBoolean()) {
        functions.sonar("${GYM_JAVA_CLIENT_PROJECT}")
    }
}

ansiColor('xterm') {
    echo "\033[42m MARK: end of gym-java-client.groovy \033[0m"
}
