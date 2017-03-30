stage("${DATAVEC_PROJECT}-checkout-sources") {
    functions.get_project_code("${DATAVEC_PROJECT}")
}

stage("${DATAVEC_PROJECT}-build") {

    echo "Building ${DATAVEC_PROJECT} version ${VERSION}"

    dir("${DATAVEC_PROJECT}") {
        functions.checktag("${DATAVEC_PROJECT}")
        functions.verset("${VERSION}", true)
//        def listVersion = ["2.10", "2.11"]

        final listVersion = [[sparkVersion: "1", scalaVersion: "2.11"],
                             [sparkVersion: "2", scalaVersion: "2.11"],
                             [sparkVersion: "1", scalaVersion: "2.10"]]

        for ( lib in listVersion) {
            echo "[ INFO ] ++ SET Scala Version to: " + lib.scalaVersion
            env.SCALA_VERSION = lib.scalaVersion
            env.SPARK_VERSION = lib.sparkVersion
            sh "./change-scala-versions.sh ${SCALA_VERSION}"
            sh "./change-spark-versions.sh ${SPARK_VERSION}"


            configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
                docker.image(dockerImage).inside(dockerParams) {
                    functions.getGpg()
                    sh '''
                    mvn -B -s ${MAVEN_SETTINGS} clean deploy -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID} -DperformRelease=${GpgVAR} -Dmaven.test.skip=${SKIP_TEST}
                    '''
                }

            }
        }
    }
    if (SONAR.toBoolean()) {
        functions.sonar("${DATAVEC_PROJECT}")
    }
}

ansiColor('xterm') {
    echo "\033[42m MARK: end of datavec.groovy \033[0m"
}
