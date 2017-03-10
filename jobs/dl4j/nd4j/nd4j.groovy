// env.LIBBND4J_SNAPSHOT = env.LIBBND4J_SNAPSHOT ?: "${VERSION}"
// env.CUDA_VERSION = env.CUDA_VERSION ?: "7.5"

stage("${PROJECT}-checkout-sources") {
    functions.get_project_code("${PROJECT}")
}

stage("${PROJECT}-build") {

    dir("${PROJECT}") {
        // functions.verset("${VERSION}", true)
        env.LIBND4J_HOME="${WORKSPACE}/libnd4j"

        configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
            switch(PLATFORM_NAME) {
                case ["macosx"]:
                    if (TESTS.toBoolean()) {
                          functions.getGpg()
                          sh'''
                          mvn -B clean install
                          '''
                    }
                    else {
                          functions.getGpg()
                          sh'''
                          mvn -B clean install -Dmaven.test.skip=true
                          '''

                    }
                break
                case ["windows-x86_64"]:
                    if (TESTS.toBoolean()) {
                          functions.getGpg()
                          bat (
                              'vcvars64.bat' +
                              '&&' +
                              'mvn -B -s %MAVEN_SETTINGS% clean install'
                          )
                    }
                    else {
                          functions.getGpg()
                          bat (
                              'vcvars64.bat' +
                              '&&' +
                              'mvn -B -s %MAVEN_SETTINGS% clean install -Dmaven.test.skip=true'
                          )
                    }
                break

                default:
                  error("Platform name - ${PLATFORM_NAME} is not defined or unsupported")

                break
            }
        }
/*
        if (SONAR.toBoolean()) {
            functions.sonar("${PROJECT}")
        } else {
            echo "Skipping ${LIBPROJECT} checking with SonarQube"
        }
*/
    }
}

echo 'MARK: end of nd4j.groovy'
