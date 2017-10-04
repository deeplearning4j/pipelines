stage("${PROJECT}-Resolve-Dependencies") {
    docker.image(dockerImage).reuseNode(true).inside(dockerParams) {
        functions.resolve_dependencies_for_nd4j()
    }
}

stage("${PROJECT}-checkout-sources") {
    functions.get_project_code("${PROJECT}")
}

stage("${PROJECT}-build") {
    dir("${PROJECT}") {
        functions.checktag("${PROJECT}")
        functions.verset("${VERSION}", true)

        configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
            docker.image(dockerImage).reuseNode(true).inside(dockerParams) {
                functions.getGpg()
                sh '''
                export GPG_TTY=$(tty)
                gpg --list-keys
                if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                mvn -U -B -PtrimSnapshots -s ${MAVEN_SETTINGS} clean deploy -Dmaven.repo.local=${HOME}/.m2/${PROFILE_TYPE}/repository -Djavacpp.platform=${PLATFORM_NAME} -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID} -Dgpg.useagent=false -DperformRelease=${GpgVAR} -Dmaven.test.skip=${SKIP_TEST} -pl '!:nd4j-cuda-8.0,!:nd4j-cuda-8.0-platform,!:nd4j-tests'
                '''
            }
        }
        // if (!isSnapshot) {
        if (PARENT_JOB.length() > 0) {
            functions.copy_nd4j_native_to_user_content()
        }
    }

    if (SONAR.toBoolean()) {
        functions.sonar("${PROJECT}")
    }
}

ansiColor('xterm') {
    echo "\033[42m MARK: end of nd4j-android-x86.groovy \033[0m"
}
