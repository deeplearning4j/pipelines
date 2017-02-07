timestamps {
    node('local-slave') {

        step([$class: 'WsCleanup'])

        checkout scm

        load 'jobs/dl4j/vars.groovy'
        functions = load 'jobs/dl4j/functions.groovy'

        stage("${LIBPROJECT}-Build") {
/*
            parallel(
                    "Stream CPU     ": { load 'jobs/dl4j/amd64/libnd4j/libnd4j_cpu.groovy' },
                    "Stream cuda 7.5": { load 'jobs/dl4j/amd64/libnd4j/libnd4j_cuda75.groovy' },
                    "Stream cuda 8.0": { load 'jobs/dl4j/amd64/libnd4j/libnd4j_cuda80.groovy' }
            )
*/
            load 'jobs/dl4j/amd64/libnd4j/libnd4j.groovy'
        }


        sh("find . -type f -name '*.so' | wc -l > ${WORKSPACE}/resultCountFile")
        def varResultCountFile = readFile("${WORKSPACE}/resultCountFile").toInteger()
        println varResultCountFile
        if (varResultCountFile == 0) {
            unstash "cpu"
            unstash "cuda75"
            unstash "cuda80"
        }

        /**
         * This stage required only for SNAPSHOT
         * In this stage we collect cuda *.so files and publish them to artifactory
         * commented solution with @curl can be used as workaround
         * @lenthFileCount get count of available files with *.so extention for package
         * @server is a reference to  https://wiki.jenkins-ci.org/display/JENKINS/Artifactory+-+Working+With+the+Pipeline+Jenkins+Plugin
         */

    }
}
