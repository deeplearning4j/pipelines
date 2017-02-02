tool name: 'CM372', type: 'hudson.plugins.cmake.CmakeTool'
def cmBin = tool 'CM372'
def cmHome = sh returnStdout: true, script: "printf `dirname '${cmBin}'`"


functions = load "${PDIR}/functions.groovy"

stage("${LIBPROJECT}-CheckoutSources") {
    functions.get_project_code("${LIBPROJECT}")
}

// stage("${LIBPROJECT}-Codecheck") {
//   functions.sonar("${LIBPROJECT}")
// }

stage("${LIBPROJECT}-Build") {

  echo 'Build Native Operations'

  dir("${LIBPROJECT}") {
    functions.checktag("${LIBPROJECT}")

    withEnv(["PATH=/opt/rh/devtoolset-3/root/usr/bin:${cmHome}:${PATH}",
            "PYTHONPATH=/opt/rh/devtoolset-3/root/usr/lib64/python2.7/site-packages:/opt/rh/devtoolset-3/root/usr/lib/python2.7/site-packages",
            'TRICK_NVCC=YES', "LIBND4J_HOME=${WORKSPACE}/${LIBPROJECT}"]) {
      echo "Building ${LIBPROJECT} version ${RELEASE_VERSION}"
      // Check TRICK_NVCC and LIBND4J_HOME existence
      sh ("env")

      // Enable devtoolset-3 to use right gcc version
      sh ("scl enable devtoolset-3 bash || true")

      sh "./buildnativeoperations.sh -c cpu"
      sh "./buildnativeoperations.sh -c cuda -v 7.5"
      sh "./buildnativeoperations.sh -c cuda -v 8.0"

      // sh 'git tag -a ${LIBPROJECT}-${RELEASE_VERSION} -m ${LIBPROJECT}-${RELEASE_VERSION}'
    }
  }
}

// stage("${LIBPROJECT}-CleanupDiskSpace") {
//   step([$class: 'WsCleanup'])
// }

// Messages for debugging
echo 'MARK: end of libnd4j.groovy'
