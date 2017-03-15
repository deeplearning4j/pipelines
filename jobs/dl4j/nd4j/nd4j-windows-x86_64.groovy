stage("${PROJECT}-ResolveDependencies") {
    functions.resolve_dependencies_for_nd4j()
}


stage("${PROJECT}-checkout-sources") {
    functions.get_project_code("${PROJECT}")
}

stage("${PROJECT}-build") {
    dir("${PROJECT}") {
        functions.verset("${VERSION}", true)
/*        env.LIBND4J_HOME = bat( script: '''bash -c "echo /${WORKSPACE}\\${LIBPROJECT} | sed -e 's/\\\\/\\//g' -e 's/://' "''',
                returnStdout: true
        ).trim()*/

        final nd4jlibs = [[cudaVersion: "7.5", scalaVersion: "2.10"],
                          [cudaVersion: "8.0", scalaVersion: "2.11"]]

        for (lib in nd4jlibs) {
            env.CUDA_VERSION = lib.cudaVersion
            env.SCALA_VERSION = lib.scalaVersion
            echo "[ INFO ] ++ Building nd4j with cuda " + CUDA_VERSION + " and scala " + SCALA_VERSION
            bat('''IF EXIST %WORKSPACE%\\%LIBPROJECT%\\blasbuild\\cuda (RD /q /s %WORKSPACE%\\%LIBPROJECT%\\blasbuild\\cuda && XCOPY /E /Q /I %WORKSPACE%\\%LIBPROJECT%\\blasbuild\\cuda-%CUDA_VERSION% %WORKSPACE%\\%LIBPROJECT%\\blasbuild\\cuda) ELSE ( XCOPY /E /Q /I %WORKSPACE%\\%LIBPROJECT%\\blasbuild\\cuda-%CUDA_VERSION% %WORKSPACE%\\%LIBPROJECT%\\blasbuild\\cuda )''')
            bat('''bash -c "./change-scala-versions.sh %SCALA_VERSION%" ''')
            bat('''bash -c "./change-cuda-versions.sh %CUDA_VERSION%" ''')

            configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
                functions.getGpg()

                bat '''         
                                bash -c "cp "
                                bash -c "gpg --list-keys"
                                mvn -B -s %MAVEN_SETTINGS% clean deploy -Dlocal.software.repository=%PROFILE_TYPE% -DstagingRepositoryId=%STAGE_REPO_ID% -DperformRelease=%GpgVAR% -Dmaven.test.skip=%SKIP_TEST% 
                                '''

            }
        }
    }
    if (SONAR.toBoolean()) {
        functions.sonar("${PROJECT}")
    }
}