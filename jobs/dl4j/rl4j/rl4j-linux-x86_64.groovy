stage("${RL4J_PROJECT}-checkout-sources") {
    functions.get_project_code("${RL4J_PROJECT}")
}

stage("${RL4J_PROJECT}-build") {
    echo "Building ${RL4J_PROJECT} version ${VERSION}"
    dir("${RL4J_PROJECT}") {
        functions.checktag("${RL4J_PROJECT}")
        functions.verset("${VERSION}", true)
        configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {

            docker.image(dockerImage).withRun(dockerParams) {
                functions.getGpg()
                sh '''
                export GPG_TTY=$(tty)
                mvn -U -B -PtrimSnapshots -s ${MAVEN_SETTINGS} clean deploy -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID} -Dgpg.useagent=false -DperformRelease=${GpgVAR} -Dmaven.test.skip=${SKIP_TEST}
                '''
            }
        }
    }
    if (SONAR.toBoolean()) {
        functions.sonar("${RL4J_PROJECT}")
    }
}

ansiColor('xterm') {
    echo "\033[42m MARK: end of rl4j-linux-x86_64.groovy \033[0m"
}
