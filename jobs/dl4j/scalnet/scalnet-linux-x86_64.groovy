stage("${SCALNET_PROJECT}-checkout-sources") {
    functions.get_project_code("${SCALNET_PROJECT}")
}

stage("${SCALNET_PROJECT}-build") {
    echo "Building ${SCALNET_PROJECT} version ${VERSION}"
    dir("${SCALNET_PROJECT}") {
        functions.checktag("${SCALNET_PROJECT}")
        sh("sed -i '0,/<artifactId>.*<\\/artifactId>/s//<artifactId>scalnet<\\/artifactId>/' pom.xml")
        functions.verset("${VERSION}", false)

        sh("sed -i '0,/<artifactId>.*<\\/artifactId>/s//<artifactId>scalnet_\${scala.binary.version}<\\/artifactId>/' pom.xml")

        def listScalaVersion = ["2.10", "2.11"]

        for (int i = 0; i < listScalaVersion.size(); i++) {
            echo "[ INFO ] ++ SET Scala Version to: " + listScalaVersion[i]
            env.SCALA_VERSION = listScalaVersion[i]

            sh("./change-scala-versions.sh ${SCALA_VERSION}")

            configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
                docker.image(dockerImage).withRun(dockerParams) {
                    functions.getGpg()
                    sh '''
                export GPG_TTY=$(tty)
                mvn -U -B -PtrimSnapshots -s ${MAVEN_SETTINGS} clean deploy -Dscalastyle.skip -DscalaVersion=${SCALA_VERSION} -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID} -Dgpg.useagent=false -DperformRelease=${GpgVAR} -Dmaven.test.skip=${SKIP_TEST}
                '''
                }

            }
        }
    }
    if (SONAR.toBoolean()) {
        functions.sonar("${GYM_JAVA_CLIENT_PROJECT}")
    }
}


ansiColor('xterm') {
    echo "\033[42m MARK: end of scalnet-linux-x86_64.groovy \033[0m"
}
