def get_project_code(proj) {
  checkout([$class: 'GitSCM',
             branches: [[name: '*/intropro072']],
             doGenerateSubmoduleConfigurations: false,
             // DO NOT FORGET TO SET noTags TO FALSE !!!
            //  extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${proj}"], [$class: 'CloneOption', honorRefspec: true, noTags: true, reference: '', shallow: true]],
             extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${proj}"], [$class: 'CloneOption', honorRefspec: true, noTags: false, reference: '', shallow: true]],
             submoduleCfg: [],
             userRemoteConfigs: [[url: "git@github.com:${ACCOUNT}/${proj}.git", credentialsId: "${GITCREDID}"]]])
}

def checktag(proj) {
  def check_tag = sh(returnStdout: true, script: "git tag -l ${proj}-${RELEASE_VERSION}")
    if (!check_tag) {
        println ("There is no tag with provided value: ${proj}-${RELEASE_VERSION}" )
    }
    else {
        println ("Version exists: " + check_tag)
        error("Failed to proceed with current version: " + check_tag)
    }
}

// def check_suff()
// if (${SNAPSHOT_VERSION} != '*-SNAPSHOT' ) {
//    // error statement stops pipeline if if is true
//    error("Error: Version ${SNAPSHOT_VERSION} should finish with -SNAPSHOT")
// }

def sonar(proj) {
  echo "Check ${ACCOUNT}/${proj} code with SonarQube Scanner"
  // requires SonarQube Scanner 2.8+
  def scannerHome = tool 'SS28';
  dir("${proj}") {
    // withSonarQubeEnv("${SQS}") {
    withSonarQubeEnv('SonarQubeServer') {
      sh "${scannerHome}/bin/sonar-scanner -Dsonar.projectKey=${ACCOUNT}:${proj} \
          -Dsonar.projectName=${proj} -Dsonar.projectVersion=${RELEASE_VERSION} \
          -Dsonar.sources=."
          // -Dsonar.sources=. -Dsonar.exclusions=**/*reduce*.h"
    }
  }
}

// mvn versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=$RELEASE_VERSION
def verset(ver, allowss) {
  def mvnHome = tool 'M339'
  sh ("'${mvnHome}/bin/mvn' versions:set -DallowSnapshots=${allowss} -DgenerateBackupPoms=false -DnewVersion=${ver}")
}

def release(proj) {
  echo "Adding tag ${proj}-${RELEASE_VERSION} to github.com/${ACCOUNT}/${proj}"
  dir("${proj}") {
    sshagent(credentials: ["${GITCREDID}"]) {
      sh 'git config user.email "jenkins@skymind.io"'
      sh 'git config user.name "Jenkins"'
      sh 'git status'
      // DO NOT ENABLE COMMIT AND TAGGING UNTIL IT IS NEEDED FOR REAL RELEASE
      // sh 'git commit -a -m "Update to version ${RELEASE_VERSION}"'
      // sh 'git tag -a ${proj}-${RELEASE_VERSION} -m ${proj}-${RELEASE_VERSION}'
      // sh 'git push origin ${proj}-${RELEASE_VERSION}'
    }
  }
}


return this;
