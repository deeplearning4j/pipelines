tool name: 'CM372', type: 'hudson.plugins.cmake.CmakeTool'
def cmBin = tool 'CM372'
def cmHome = sh returnStdout: true, script: "printf `dirname '${cmBin}'`"


functions = load "${PDIR}/functions.groovy"

stage("${LIBPROJECT}-CheckoutSources") {
    functions.get_project_code("${LIBPROJECT}")
}

stage("${LIBPROJECT}-Build") {

  echo 'Build Native Operations'

  dir("${LIBPROJECT}") {
    functions.checktag("${LIBPROJECT}")

    withEnv(["PATH=${cmHome}:${PATH}", 'TRICK_NVCC=YES', "LIBND4J_HOME=${WORKSPACE}/${LIBPROJECT}"]) {
      echo "Building ${LIBPROJECT} version ${RELEASE_VERSION}"
      // Check TRICK_NVCC and LIBND4J_HOME existence
      sh("env")

      sh("ls -al")

      // Enable devtoolset-3 to use right gcc version
      // sh ("scl enable devtoolset-3 bash || true")

      // sh("./buildnativeoperations.sh -c cpu")
      // sh("./buildnativeoperations.sh -c cuda -v 7.5")
      // sh("./buildnativeoperations.sh -c cuda -v 8.0")

      // sh 'git tag -a ${LIBPROJECT}-${RELEASE_VERSION} -m ${LIBPROJECT}-${RELEASE_VERSION}'
    }
  }
}

stage("${LIBPROJECT}-Codecheck") {
  def scannerHome = tool 'SS28';
  dir("${LIBPROJECT}") {
    // withSonarQubeEnv("${SQS}") {
    withSonarQubeEnv('SonarQubeServer') {
      sh "${scannerHome}/bin/sonar-scanner -Dsonar.projectKey=${ACCOUNT}:${LIBPROJECT} \
          -Dsonar.projectName=${LIBPROJECT} -Dsonar.projectVersion=${RELEASE_VERSION} \
          -Dsonar.sources=. -Dsonar.binaries=${WORKSPACE}/${LIBPROJECT}/cpu/blas,${WORKSPACE}/${LIBPROJECT}/cuda-7.5/blas,${WORKSPACE}/${LIBPROJECT}/cuda-8.0/blas"
          // -Dsonar.sources=. -Dsonar.exclusions=**/*reduce*.h"
    }
  }
}

// stage("${LIBPROJECT}-Codecheck") {
//   functions.sonar("${LIBPROJECT}")
// }

// stage("${LIBPROJECT}-CleanupDiskSpace") {
//   step([$class: 'WsCleanup'])
// }

// Messages for debugging
echo 'MARK: end of libnd4j.groovy'
