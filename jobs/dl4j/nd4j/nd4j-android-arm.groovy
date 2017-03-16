stage("${PROJECT}-ResolveDependencies") {
    docker.image(dockerImage).inside(dockerParams) {
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
            docker.image(dockerImage).inside(dockerParams) {
                functions.getGpg()
                sh("gpg --list-keys")
                sh("if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi")
                sh('''mvn -B -s ${MAVEN_SETTINGS} clean deploy -Djavacpp.platform=${PLATFORM_NAME} -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID} -DperformRelease=${GpgVAR} -Dmaven.test.skip=${SKIP_TEST} -pl '!:nd4j-cuda-8.0,!:nd4j-cuda-8.0-platform' ''')
            }
        }

        stash includes: "nd4j-backends/nd4j-backend-impls/nd4j-native/target/nd4j-native-${VERSION}-${PLATFORM_NAME}.jar", name: "nd4j${PLATFORM_NAME}"
        node("master") {
            unstash "nd4j${PLATFORM_NAME}"
            functions.copy_nd4j_native_to_user_content()
        }
    }

    if (SONAR.toBoolean()) {
        functions.sonar("${PROJECT}")
    }
}
