tool name: 'CM372', type: 'hudson.plugins.cmake.CmakeTool'
def cmakeBin = tool 'CM372'
def cmHome = sh returnStdout: true, script: "dirname '${cmakeBin}'" 


functions = load 'jobs/dl4j/functions.groovy'

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

    withEnv(['TRICK_NVCC=YES', "LIBND4J_HOME=${WORKSPACE}/${LIBPROJECT}"]) {
      echo "Building ${LIBPROJECT} version ${RELEASE_VERSION}"
      // Check TRICK_NVCC and LIBND4J_HOME existence
      sh ("env")


      sh ("dirname '${cmakeBin}'")
      // sh ("which cmake")

      // sh ("which cmake3")

      sh ("'${cmakeBin}' -version")

      // sh "./buildnativeoperations.sh -c cpu"
      // sh "./buildnativeoperations.sh -c cuda -v 7.5"
      // sh "./buildnativeoperations.sh -c cuda -v 8.0"

      // sh 'git tag -a ${LIBPROJECT}-${RELEASE_VERSION} -m ${LIBPROJECT}-${RELEASE_VERSION}'
    }
  }
}

// stage("${LIBPROJECT}-CleanupDiskSpace") {
//   step([$class: 'WsCleanup'])
//   sh ("rm -f /tmp/tmpxft_*")
// }

// Messages for debugging
echo 'MARK: end of libnd4j.groovy'
