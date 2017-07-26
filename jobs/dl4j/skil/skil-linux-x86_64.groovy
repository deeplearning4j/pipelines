package skil

stage("${SKIL_PROJECT}-checkout-sources") {
    env.ACCOUNT = "skymindio"
    functions.get_project_code("lagom-skil-api")
}

stage("${SKIL_PROJECT}-build") {
    echo "Building ${SKIL_PROJECT} version ${VERSION}"
    dir("lagom-skil-api") {
        functions.checktag("${SKIL_PROJECT}")
        functions.verset("${VERSION}", true)
        configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
            docker.image(dockerImage).inside(dockerParams) {
                functions.getGpg()
                if (env.CREATE_RPM.toBoolean()){
                  RPM_PROFILE = "-Pgenerate-rpm"
                } else {
                  RPM_PROFILE = ""
                }
                sh '''
                export GPG_TTY=$(tty)
                mvn -U -B -s ${MAVEN_SETTINGS} ${RPM_PROFILE} clean deploy -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID} -Dgpg.useagent=false -DperformRelease=${GpgVAR} -Dmaven.test.skip=${SKIP_TEST}
                curl -T skil-distro-parent/skildistro/target/skil-distro-${VERSION}-dist.tar.gz -uhuitseeker:e0208f45cc328d3980ab4162e6ae368fa458d1c9 https://api.bintray.com/content/skymindio/SKIL-archive/SKIL/1.1-SNAPSHOT/skil-distro-${VERSION}-dist-$(date +%Y-%m-%d).tar.gz
                '''
                if (env.CREATE_RPM.toBoolean()){
                    dir("contrib"){
                        sh("push_to_bintray.sh huitseeker e0208f45cc328d3980ab4162e6ae368fa458d1c9 skymindio /home/huitseeker/DL4J/lagom-skil-api/skil-distro-parent/skil-distro-rpm/target/rpm/skil-server/RPMS/x86_64/skil-server-* https://api.bintray.com/content/skymindio/rpm/")
                    }
                }
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
