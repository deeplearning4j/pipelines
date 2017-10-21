stage("${DEEPLEARNING4J_PROJECT}-checkout-sources") {

    functions.get_project_code("${DEEPLEARNING4J_PROJECT}")
    dir("${DEEPLEARNING4J_PROJECT}") {
        functions.checktag("${DEEPLEARNING4J_PROJECT}")
    }

    checkout([$class                           : 'GitSCM',
              branches                         : [[name: '*/master']],
              doGenerateSubmoduleConfigurations: false,
              extensions                       : [[$class: 'RelativeTargetDirectory', relativeTargetDir: "dl4j-test-resources"], [$class: 'CloneOption', honorRefspec: true, noTags: true, reference: '', shallow: true]],
              submoduleCfg                     : [],
              userRemoteConfigs                : [[url: "https://github.com/${ACCOUNT}/dl4j-test-resources.git"]]
    ])
}

stage("build test resources on ${PLATFORM_NAME}") {
    configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
        dir('dl4j-test-resources') {
            docker.image(dockerImage).inside(dockerParams) {
                sh("mvn -U -B -PtrimSnapshots clean install")

            }
        }
    }
}

stage("${DEEPLEARNING4J_PROJECT}-build") {

    echo "Building ${DEEPLEARNING4J_PROJECT} version ${VERSION}"

    dir("${DEEPLEARNING4J_PROJECT}") {
        functions.checktag("${DEEPLEARNING4J_PROJECT}")
/*
        // remove sed functions after setting project.version variable in pom.xml
        // nd4j.version in pom.xml
        functions.sed("${PROJECT}")
        // deeplearning4j.version in pom.xml
        functions.sed("${DEEPLEARNING4J_PROJECT}")
        // datavec.version in pom.xml
        functions.sed("${DATAVEC_PROJECT}")
        // dl4j-test-resources.version in pom.xml
        // functions.sed("dl4j-test-resources")

        // debug versions setting
        // sh("cat pom.xml")
*/

        // set spark version in all pom.xml files
        functions.sed_spark_1()

        functions.verset("${VERSION}", true)

        def listScalaVersion = ["2.10", "2.11", "2.11"] * 2
        def listCudaVersion = ["8.0"] * 3 + ["9.0"] * 3
        def listSparkVersion = ["1", "1", "2"] * 2

        for (int i = 0; i < listScalaVersion.size(); i++) {
            echo "[ INFO ] ++ SET Scala Version to: " + listScalaVersion[i]
            env.SCALA_VERSION = listScalaVersion[i]
            echo "[ INFO ] ++ SET Cuda Version to: " + listCudaVersion[i]
            env.CUDA_VERSION = listCudaVersion[i]
            echo "[ INFO ] ++ SET Spark Version to: " + listSparkVersion[i]
            env.SPARK_VERSION = listSparkVersion[i]

            sh("./change-scala-versions.sh ${SCALA_VERSION}")
            sh("./change-cuda-versions.sh ${CUDA_VERSION}")
            sh("./change-spark-versions.sh ${SPARK_VERSION}")

            configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
                docker.image(dockerImage).inside(dockerParams) {
                    functions.getGpg()
                    sh '''
                export GPG_TTY=$(tty)
                if [ -f /etc/redhat-release ]; then source /opt/rh/devtoolset-3/enable ; fi
                mvn -U -B -PtrimSnapshots -s ${MAVEN_SETTINGS} clean deploy -Dlocal.software.repository=${PROFILE_TYPE} -DstagingRepositoryId=${STAGE_REPO_ID} -Dgpg.useagent=false -DperformRelease=${GpgVAR} -Dmaven.test.skip=${SKIP_TEST} -Dnd4j.version=${VERSION} -Ddeeplearning4j.version=${VERSION} -Ddatavec.version=${VERSION} -Ddl4j-test-resources.version=${VERSION}
                '''
                }
            }
        }
    }
    if (SONAR.toBoolean()) {
        functions.sonar("${DEEPLEARNING4J_PROJECT}")
    }
}

ansiColor('xterm') {
    echo "\033[42m MARK: end of deeplearning4j.groovy \033[0m"
}
