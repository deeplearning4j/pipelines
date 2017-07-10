package skil

stage("${SKIL_PROJECT}-checkout-sources") {
    env.ACCOUNT = "skymindio"
    functions.get_project_code("lagom-skil-api")
}

stage("${SKIL_PROJECT}-build") {
    echo "Building ${SKIL_PROJECT} version ${VERSION}"
    dir("${SKIL_PROJECT}") {
        functions.checktag("${SKIL_PROJECT}")
        functions.verset("${VERSION}", true)
        configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
            docker.image(dockerImage).inside(dockerParams) {
                functions.getGpg()
                sh '''
                mvn -U -B -s ${MAVEN_SETTINGS} clean deploy -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID} -DperformRelease=${GpgVAR} -Dmaven.test.skip=${SKIP_TEST}
                '''
            }
        }

    }
    if (SONAR.toBoolean()) {
        functions.sonar("${SKIL_PROJECT}")
    }
}

ansiColor('xterm') {
    echo "\033[42m MARK: end of skil.groovy \033[0m"
}
