timestamps {
  node('master') {
    git 'https://github.com/jenkinsci/parallel-test-executor-plugin-sample.git'
    stash name: 'sources', includes: 'pom.xml,src/'
  }
  def splits = splitTests count(2)
  def branches = [:]
  for (int i = 0; i < splits.size(); i++) {
    def index = i // fresh variable per iteration; i will be mutated
    branches["split${i}"] = {
      node('remote') {
        deleteDir()
        unstash 'sources'
        def exclusions = splits.get(index);
        writeFile file: 'exclusions.txt', text: exclusions.join("\n")
        sh "${tool 'M339'}/bin/mvn -B -Dmaven.test.failure.ignore test"
        junit 'target/surefire-reports/*.xml'
      }
    }
  }
}
parallel branches
