timestamps {
  node ('master') {
    step([$class: 'WsCleanup'])

    def str1 = '2.3.4-SNAPSHOT'
    def isSnapshot = str1.contains(SNAPSHOT)

    echo "00-${str1}"
    echo "01-${isSnapshot}"

    sh([script: 'env'])

    // Messages for debugging
    echo 'MARK: end of test_string.groovy'
    step([$class: 'WsCleanup'])
  }
}
