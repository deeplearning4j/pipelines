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
      // sh "env"

      // sh "./buildnativeoperations.sh -c cpu"
      // sh "./buildnativeoperations.sh -c cuda -v 7.5"
      // sh "./buildnativeoperations.sh -c cuda -v 8.0"

      // Trying to reduce native operatins build time
      def cmakes = [:]
        cmakes['cpu'] = {
          sh "./buildnativeoperations.sh -c cpu"
        }

        cmakes['cuda-7.5'] = {
          sh "./buildnativeoperations.sh -c cuda -v 7.5"
        }

        cmakes['cuda-8.0'] = {
          sh "./buildnativeoperations.sh -c cuda -v 8.0"
        }
      parallel cmakes

      // sh 'git tag -a ${LIBPROJECT}-${RELEASE_VERSION} -m ${LIBPROJECT}-${RELEASE_VERSION}'
    }
  }
}

// Messages for debugging
echo 'MARK: end of libnd4j.groovy'
