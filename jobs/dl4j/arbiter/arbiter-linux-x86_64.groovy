stage("${ARBITER_PROJECT}-checkout-sources") {
    functions.get_project_code("${ARBITER_PROJECT}")
}

stage("${ARBITER_PROJECT}-build") {
    echo "Building ${ARBITER_PROJECT} version ${VERSION}"
    dir("${ARBITER_PROJECT}") {
        functions.checktag("${ARBITER_PROJECT}")
        functions.verset("${VERSION}", true)
        def listScalaVersion = ["2.10", "2.11"]

        for (int i = 0; i < listScalaVersion.size(); i++) {
            echo "[ INFO ] ++ SET Scala Version to: " + listScalaVersion[i]
            env.SCALA_VERSION = listScalaVersion[i]
            sh "./change-scala-versions.sh ${SCALA_VERSION}"

            configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
                docker.image(dockerImage).resueNode('true').inside(dockerParams) {
                    functions.getGpg()
                    sh '''
                      export GPG_TTY=$(tty)
                      mvn -U -B -PtrimSnapshots -s ${MAVEN_SETTINGS} clean deploy -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID} -Dgpg.useagent=false -DperformRelease=${GpgVAR} -Dmaven.test.skip=${SKIP_TEST}
                      '''
                }
            }
        }
    }
    if (SONAR.toBoolean()) {
        functions.sonar("${ARBITER_PROJECT}")
    }
}

ansiColor('xterm') {
    echo "\033[42m MARK: end of arbiter.groovy \033[0m"
}
