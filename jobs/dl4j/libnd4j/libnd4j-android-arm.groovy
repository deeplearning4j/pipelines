stage("${LIBPROJECT}-build") {
    if (CBUILD.toBoolean()) {
        functions.get_project_code("${LIBPROJECT}")
        dir("${LIBPROJECT}") {
            docker.image(dockerImage).inside(dockerParams) {
                sh '''
                        if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                        ./buildnativeoperations.sh -platform ${PLATFORM_NAME}
                        '''
            }
        }
        if (SONAR.toBoolean()) {
            functions.sonar("${LIBPROJECT}")
        }
    } else {
        echo "Skipping libnd4j build, using snapshot"
    }
}

echo 'MARK: end of libnd4j.groovy'