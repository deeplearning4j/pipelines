stage("${PROJECT}-Resolve-Dependencies") {
    functions.resolve_dependencies_for_nd4j()
}

stage("${PROJECT}-checkout-sources") {
    functions.get_project_code("${PROJECT}")
}

stage("${PROJECT}-build") {
    dir("${PROJECT}") {
        functions.checktag("${PROJECT}")
        functions.verset("${VERSION}", true)
        env.WORKSPACE_BASH = "/" + WORKSPACE.replace('\\','/').replaceFirst(':','')
        env.LIBND4J_HOME = "/" + WORKSPACE.replace('\\','/') + "/" + "${LIBPROJECT}"

        final nd4jlibs = [[cudaVersion: "8.0", scalaVersion: "2.11"], [cudaVersion: "9.0", scalaVersion: "2.11"]]

        for (lib in nd4jlibs) {
            env.CUDA_VERSION = lib.cudaVersion
            env.SCALA_VERSION = lib.scalaVersion
            echo "[ INFO ] ++ Building nd4j with cuda " + CUDA_VERSION + " and scala " + SCALA_VERSION
            bat('''IF EXIST %WORKSPACE%\\%LIBPROJECT%\\blasbuild\\cuda (RD /q /s %WORKSPACE%\\%LIBPROJECT%\\blasbuild\\cuda && XCOPY /E /Q /I %WORKSPACE%\\%LIBPROJECT%\\blasbuild\\cuda-%CUDA_VERSION% %WORKSPACE%\\%LIBPROJECT%\\blasbuild\\cuda) ELSE ( XCOPY /E /Q /I %WORKSPACE%\\%LIBPROJECT%\\blasbuild\\cuda-%CUDA_VERSION% %WORKSPACE%\\%LIBPROJECT%\\blasbuild\\cuda )''')
            bat('''"C:\\Program Files\\Git\\bin\\bash.exe" -c "./change-scala-versions.sh %SCALA_VERSION%" ''')
            bat('''"C:\\Program Files\\Git\\bin\\bash.exe" -c "./change-cuda-versions.sh %CUDA_VERSION%" ''')

            configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
                functions.getGpg()
                bat ("cp %MAVEN_SETTINGS% %WORKSPACE%\\settings.xml")
                bat (
                        'vcvars64.bat' +
                                '&&' +
                                'bash -c "export PATH=$PATH:/c/msys64/mingw64/bin && mvn -U -B -PtrimSnapshots -s %WORKSPACE_BASH%/settings.xml clean deploy -Dmaven.repo.local=%TEMP%\\.m2\\%PROFILE_TYPE% -Dscala.binary.version=${SCALA_VERSION} -Dlocal.software.repository=%PROFILE_TYPE% -DstagingRepositoryId=%STAGE_REPO_ID% -Dgpg.useagent=false -DperformRelease=%GpgVAR% -Dmaven.test.skip=%SKIP_TEST% -Denv.LIBND4J_HOME=%LIBND4J_HOME%'
                )

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
    echo "\033[42m MARK: end of nd4j-windows-x86_64.groovy \033[0m"
}
