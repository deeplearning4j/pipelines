stage("${LIBPROJECT}-Checkout-Sources") {
    functions.get_project_code("${LIBPROJECT}")
}

// stage("${LIBPROJECT}-Codecheck") {
//   functions.sonar("${LIBPROJECT}")
// }

stage("${LIBPROJECT}-Build") {

    echo 'Build Native Operations'

    dir("${LIBPROJECT}") {
        functions.checktag("${LIBPROJECT}")

        withEnv(['TRICK_NVCC=YES', "LIBND4J_HOME=${WORKSPACE}/${LIBPROJECT}"]) {
            echo "Building ${LIBPROJECT} version ${RELEASE_VERSION}"
            // Check TRICK_NVCC and LIBND4J_HOME existence
            // sh "env"

            sh "./buildnativeoperations.sh -c cpu"
            sh "./buildnativeoperations.sh -c cuda -v 7.5"
            sh "./buildnativeoperations.sh -c cuda -v 8.0"

            // sh 'git tag -a ${LIBPROJECT}-${RELEASE_VERSION} -m ${LIBPROJECT}-${RELEASE_VERSION}'
        }
    }
}

stage("${LIBPROJECT}-package") {
    dir("${LIBPROJECT}") {

        if (fileExists('outFileCount')) {
            sh "rm -f outFileCount ; rm -f *.tar ; find  . -name *.so  | wc -l >>  ${WORKSPACE}/outFileCount"
        } else {
            sh "find  . -name *.so  | wc -l >>  ${WORKSPACE}/outFileCount"
        }

        def lenthFileCount = readFile("${WORKSPACE}/outFileCount")
        def numberOfLines = lenthFileCount.toInteger()

        if (numberOfLines != 0) {
            sh("for i in `find  . -name *.so` ; do tar -uf ${LIBPROJECT}-${RELEASE_VERSION}.tar \$i; done")
        } else {
            echo "[WARNING] There is no files to proceed"
            echo "[INFO] Build marked as failure"
            currentBuild.result = 'FAILURE'
        }

//            sh ("curl -i -u ${ARTFACT_USER}:${ARTFACT_PASS} -X PUT \"${ARTFACT_URL}/${ARTFACT_SNAPSHOT}/${ARTFACT_GROUP_ID}/${LIBPROJECT}/${RELEASE_VERSION}/${LIBPROJECT}-${RELEASE_VERSION}.tar\"  -T  ${LIBPROJECT}-${RELEASE_VERSION}.tar")

        def server = Artifactory.newServer url: "${ARTFACT_URL}", username: "${ARTFACT_USER}", password: "${ARTFACT_PASS}"

        def uploadSpec = """{
                    "files": [{
                    "pattern": "${LIBPROJECT}-${RELEASE_VERSION}.tar",
                    "target": "${ARTFACT_SNAPSHOT}/${ARTFACT_GROUP_ID}/${LIBPROJECT}/${RELEASE_VERSION}/"
                     }]}"""
        server.upload(uploadSpec)
    }
}

/*stage("${LIBPROJECT}-package") {
  dir("${LIBPROJECT}") {

    if (fileExists('outFileCount')) {
      sh "rm -f outFileCount ; rm -f *.tar ; find  . -name *.so  | wc -l >> outFileCount"
    } else {
      sh "find  . -name *.so  | wc -l >> outFileCount"
    }

    def lenthFileCount = readFile('outFileCount')
    def numberOfLines = lenthFileCount.toInteger()

    if (numberOfLines != 0) {
      sh("for i in `find  . -name *.so` ; do tar -uf ${LIBPROJECT}-${RELEASE_VERSION}.tar \$i; done")
    } else {
      echo "[WARNING] There is no files to proceed"
      echo "[INFO] Build marked as failure"
      currentBuild.result = 'FAILURE'
    }

//            sh ("curl -i -u ${ARTFACT_USER}:${ARTFACT_PASS} -X PUT \"${ARTFACT_URL}/${ARTFACT_SNAPSHOT}/${ARTFACT_GROUP_ID}/${LIBPROJECT}/${RELEASE_VERSION}/${LIBPROJECT}-${RELEASE_VERSION}.tar\"  -T  ${LIBPROJECT}-${RELEASE_VERSION}.tar")

    def server = Artifactory.newServer url: "${ARTFACT_URL}", username: "${ARTFACT_USER}", password: "${ARTFACT_PASS}"

    def uploadSpec = """{
                    "files": [{
                    "pattern": "${LIBPROJECT}-${RELEASE_VERSION}.tar",
                    "target": "${ARTFACT_SNAPSHOT}/${ARTFACT_GROUP_ID}/${LIBPROJECT}/${RELEASE_VERSION}/"
                     }]}"""
    server.upload(uploadSpec)
  }
}*/

// stage("${LIBPROJECT}-CleanupDiskSpace") {
//   step([$class: 'WsCleanup'])
//   sh ("rm -f /tmp/tmpxft_*")
// }

// Messages for debugging
echo 'MARK: end of libnd4j.groovy'
