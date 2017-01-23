def mvnHome = tool 'M339'

def get_project_code(proj) {
  checkout([$class: 'GitSCM',
             branches: [[name: '*/intropro']],
             doGenerateSubmoduleConfigurations: false,
            //  extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${proj}"], [$class: 'CloneOption', honorRefspec: true, noTags: true, reference: '', shallow: true]],
             extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${proj}"], [$class: 'CloneOption', honorRefspec: true, noTags: false, reference: '', shallow: true]],
             submoduleCfg: [],
             userRemoteConfigs: [[url: "git@github.com:${ACCOUNT}/${proj}.git", credentialsId: "${GITCREDID}"]]])
}

def release(proj) {
  echo "Adding tag ${proj}-${RELEASE_VERSION} to github.com/${ACCOUNT}/${proj}"
  dir("${proj}") {
    sshagent(credentials: ["${GITCREDID}"]) {
      sh "ls -al"
      echo "hello from ${proj}"
      sh 'git config user.email "jenkins@skymind.io"'
      sh 'git config user.name "Jenkins"'
      sh 'git status'
      // DO NOT ENABLE COMMIT AND TAGGING UNTIL IT IS NEEDED FOR REAL RELEASE
      // sh 'git commit -a -m "Update to version ${RELEASE_VERSION}"'
      // sh 'git tag -a ${proj}-${RELEASE_VERSION} -m ${SCALNET_PROJECT}-${RELEASE_VERSION}'
      // sh 'git push origin ${proj}-${RELEASE_VERSION}'
    }
  }
}

return this;
