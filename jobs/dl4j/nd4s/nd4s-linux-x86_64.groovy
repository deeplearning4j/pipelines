stage("${ND4S_PROJECT}-Platform-Builds-Wait") {
    // Workaround to fetch the latest docker image
    docker.image(dockerImages.centos6cuda80).pull()

    // if (!isSnapshot) {
    if (PARENT_JOB.length() > 0) {
        echo "Copying nd4j artifacts from userContent"
        int ND4J_NATIVE_COUNT = 0

        timeout(time:15, unit:'HOURS') {
            waitUntil {
                sh("rm -rf ${WORKSPACE}/nd4j-native-${VERSION}*")

                functions.copy_nd4j_native_from_user_content()

                String checkNumberOfArtifactsScript = 'ls -la ${WORKSPACE}/nd4j-native-${VERSION}* | wc -l'

                ND4J_NATIVE_COUNT = sh(script: checkNumberOfArtifactsScript, returnStdout: true).trim().toInteger()

                echo("${ND4J_NATIVE_COUNT}")

                /*
                    This statement checks if we have required number of artifacts,
                    if not waitUtil step will slow down the delay between attempts.

                    Total timeout set to 30 minutes.
                 */
                return (ND4J_NATIVE_COUNT == 6)
            }
        }

        docker.image(dockerImages.centos6cuda80).inside(dockerParams) {
            functions.install_nd4j_native_to_local_maven_repository("${VERSION}")
        }
    } else {
        docker.image(dockerImages.centos6cuda80).inside(dockerParams) {
            functions.nd4s_install_snapshot_dependencies_to_maven2_local_repository("org.nd4j", "nd4j-native", "${VERSION}", "jar", ["linux-x86_64","android-arm", "android-x86", "linux-ppc64le", "macosx-x86_64", "windows-x86_64"], "${PROFILE_TYPE}")
        }
    }
}


stage("${ND4S_PROJECT}-checkout-sources") {
    functions.get_project_code("${ND4S_PROJECT}")
}

stage("${ND4S_PROJECT}-build") {
    echo "Building ${ND4S_PROJECT} version ${VERSION}"
    dir("${ND4S_PROJECT}") {
        functions.checktag("${ND4S_PROJECT}")
//        sh ("sed -i 's/version := \".*\",/version := \"${VERSION}\",/' build.sbt")
//        sh ("sed -i 's/nd4jVersion := \".*\",/nd4jVersion := \"${ND4J_VERSION}\",/' build.sbt")
        sh("test -d ${WORKSPACE}/.ivy2 || mkdir ${WORKSPACE}/.ivy2")
        configFileProvider([configFile(fileId: "sbt-local-nexus-id-1", variable: 'SBT_CREDENTIALS')]) {
            sh("cp ${SBT_CREDENTIALS}  ${WORKSPACE}/.ivy2/.nexus")
        }
        configFileProvider([configFile(fileId: "sbt-local-jfrog-id-1", variable: 'SBT_CREDENTIALS')]) {
            sh("cp ${SBT_CREDENTIALS}  ${WORKSPACE}/.ivy2/.jfrog")
        }
        configFileProvider([configFile(fileId: "sbt-oss-sonatype-id-1", variable: 'SBT_CREDENTIALS')]) {
            sh("cp ${SBT_CREDENTIALS}  ${WORKSPACE}/.ivy2/.sonatype")
        }
        configFileProvider([configFile(fileId: "sbt-oss-bintray-id-1", variable: 'SBT_CREDENTIALS')]) {
            sh("cp ${SBT_CREDENTIALS}  ${WORKSPACE}/.ivy2/.bintray")
        }

        docker.image(dockerImages.centos6cuda80).inside(dockerParams) {
            wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
                functions.getGpg()
                sh '''
                  export GPG_TTY=$(tty)
                  gpg --list-keys
                  cp -a ${WORKSPACE}/.ivy2 ${HOME}/
                  cp ${HOME}/.ivy2/.${PROFILE_TYPE} ${HOME}/.ivy2/.credentials
                  sbt -DrepoType=${PROFILE_TYPE} -DstageRepoId=${STAGE_REPO_ID} -DcurrentVersion=${VERSION} -Dnd4jVersion=${VERSION} +publishSigned
                  find ${WORKSPACE}/.ivy2 ${HOME}/.ivy2  -type f -name  ".credentials"  -delete -o -name ".nexus"  -delete -o -name ".jfrog" -delete -o -name ".sonatype" -delete -o -name ".bintray" -delete;
                  '''
            }
        }

    }
}

ansiColor('xterm') {
    echo "\033[42m MARK: end of nd4s-linux-x86_64.groovy \033[0m"
}
