stage("${DATAVEC_PROJECT}-checkout-sources") {
    functions.get_project_code("${DATAVEC_PROJECT}")

    // Workaround to fetch the latest docker image
    docker.image(dockerImages.centos6cuda80).pull()
}

stage("${DATAVEC_PROJECT}-build") {

    echo "Building ${DATAVEC_PROJECT} version ${VERSION}"

    dir("${DATAVEC_PROJECT}") {
        functions.checktag("${DATAVEC_PROJECT}")
/*
        // line <nd4j.version>${project.parent.version}</nd4j.version> in pom.xml
        functions.sed("${PROJECT}")
*/
        // set spark version in all pom.xml files
        functions.sed_spark_1()

        // set project version
        functions.verset("${VERSION}", true)

        // debug versions setting
        // sh("cat pom.xml")
        // error("no need to run further")

//        def listVersion = ["2.10", "2.11"]

        final listVersion = [
                [sparkVersion: "1", scalaVersion: "2.10"],
                [sparkVersion: "1", scalaVersion: "2.11"],
                [sparkVersion: "2", scalaVersion: "2.11"]
        ]

        for ( lib in listVersion) {
            echo "[ INFO ] ++ SET Scala Version to: " + lib.scalaVersion
            env.SCALA_VERSION = lib.scalaVersion
            env.SPARK_VERSION = lib.sparkVersion
            sh "./change-scala-versions.sh ${SCALA_VERSION}"
            sh "./change-spark-versions.sh ${SPARK_VERSION}"


            configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
                docker.image(dockerImages.centos6cuda80).inside(dockerParams) {
                    functions.getGpg()
                    sh '''
                    export GPG_TTY=$(tty)
                    mvn -U -B -PtrimSnapshots -s ${MAVEN_SETTINGS} clean deploy -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID} -Dgpg.useagent=false -DperformRelease=${GpgVAR} -Dmaven.test.skip=${SKIP_TEST}
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
