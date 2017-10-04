stage("${LIBPROJECT}-build") {
    if (CBUILD.toBoolean()) {
        functions.get_project_code("${LIBPROJECT}")
        dir("${LIBPROJECT}") {
            docker.image(dockerImage).resueNode('true').inside(dockerParams) {
                sh '''
                        if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                        ./buildnativeoperations.sh -platform ${PLATFORM_NAME}
                        '''
            }
            if ( PUSH_LIBND4J_LOCALREPO.toBoolean() ) {
                docker.image(dockerImage).resueNode('true').inside(dockerParams){
                    functions.upload_libnd4j_snapshot_version_to_snapshot_repository(VERSION, PLATFORM_NAME, PROFILE_TYPE)
                }
            }
        }
    } else {
        echo "Skipping libnd4j build, using snapshot"
    }

    if (SONAR.toBoolean()) {
        functions.sonar("${LIBPROJECT}")
    }
}

ansiColor('xterm') {
    echo "\033[42m MARK: end of libnd4j-android-x86.groovy \033[0m"
}
