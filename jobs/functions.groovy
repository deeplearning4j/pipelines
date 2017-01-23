def get_project_code(PROJ) {
  checkout([$class: 'GitSCM',
             branches: [[name: '*/intropro']],
             doGenerateSubmoduleConfigurations: false,
            //  extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${PROJ}"], [$class: 'CloneOption', honorRefspec: true, noTags: true, reference: '', shallow: true]],
             extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${PROJ}"], [$class: 'CloneOption', honorRefspec: true, noTags: false, reference: '', shallow: true]],
             submoduleCfg: [],
             userRemoteConfigs: [[url: "git@github.com:${ACCOUNT}/${PROJ}.git", credentialsId: "${GITCREDID}"]]])
}

def release(PROJ) {
  echo "Adding tag ${PROJ}-${RELEASE_VERSION} to github.com/${ACCOUNT}/${PROJ}"
  dir("${PROJ}") {
    sshagent(credentials: ["${GITCREDID}"]) {
      sh "ls -al"
      echo "hello from ${PROJ}"
      sh 'git config user.email "jenkins@skymind.io"'
      sh 'git config user.name "Jenkins"'
      sh 'git status'
      // DO NOT ENABLE COMMIT AND TAGGING UNTIL IT IS NEEDED FOR REAL RELEASE
      // sh 'git commit -a -m "Update to version ${RELEASE_VERSION}"'
      // sh 'git tag -a ${SCALNET_PROJECT}-${RELEASE_VERSION} -m ${SCALNET_PROJECT}-${RELEASE_VERSION}'
      // sh 'git push origin ${SCALNET_PROJECT}-${RELEASE_VERSION}'
    }
  }
}

return this;
