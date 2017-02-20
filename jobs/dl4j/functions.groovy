def get_code(proj) {
  if(isSnapshot) {
    echo "Do not fetch tags for snapshot"
    notags = 'true'
    echo notags
  }
  else {
    echo "Fetch tags for current build"
    notags = 'false'
    echo notags
  }
  checkout([$class: 'GitSCM',
             branches: [[name: "*/${GIT_BRANCHNAME}"]],
             doGenerateSubmoduleConfigurations: false,
             extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${proj}"], [$class: 'CloneOption', honorRefspec: true, noTags: "${notags}", reference: '', shallow: true]],
            //  extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${proj}"], [$class: 'CloneOption', honorRefspec: true, noTags: false, reference: '', shallow: true]],
             submoduleCfg: [],
             userRemoteConfigs: [[url: "git@github.com:${ACCOUNT}/${proj}.git", credentialsId: "${GITCREDID}"]]])
}

def get_project_code(proj) {
  checkout([$class: 'GitSCM',
             branches: [[name: "*/${GIT_BRANCHNAME}"]],
             doGenerateSubmoduleConfigurations: false,
            //  extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${proj}"], [$class: 'CloneOption', honorRefspec: true, noTags: "${notags}", reference: '', shallow: true]],
             extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${proj}"], [$class: 'CloneOption', honorRefspec: true, noTags: false, reference: '', shallow: true]],
             submoduleCfg: [],
             userRemoteConfigs: [[url: "git@github.com:${ACCOUNT}/${proj}.git", credentialsId: "${GITCREDID}"]]])
}

// Remove .git folder and other unneeded files from workspace
def rm() {
  echo "Remove .git folder from workspace - ${WORKSPACE}"
  dir("${WORKSPACE}") {
    sh("rm -rf ${WORKSPACE}/.git")
    sh("rm -f ${WORKSPACE}/.gitignore")
    sh("rm -rf ${WORKSPACE}/docs")
    sh("rm -rf ${WORKSPACE}/imgs")
    sh("rm -rf ${WORKSPACE}/ansible")
    sh("rm -f ${WORKSPACE}/README.md")
  }
}

def checktag(proj) {
  echo "Check if ${proj}-${RELEASE_VERSION} has been released already"
  def check_tag = sh(returnStdout: true, script: "git tag -l ${proj}-${RELEASE_VERSION}")
    if (check_tag) {
      echo ("Version exists: " + check_tag)
      error("Failed to proceed with current version: " + check_tag)
    }
    else {
      echo ("There is no tag with provided value: ${proj}-${RELEASE_VERSION}" )
    }
}

def dirm2() {
  sh ("mkdir ${WORKSPACE}/.m2 || true")
}

def def_docker() {
  echo "Setting docker parameters and image for ${PLATFORM_NAME}"
  switch("${PLATFORM_NAME}") {
    case "linux-ppc64le":
      dockerImage = "${DOCKER_CUDA_PPC}"
      dockerParams = dockerParams_ppc

    break

    case "linux-x86_64":
      dockerImage = "${DOCKER_CENTOS6_CUDA80_AMD64}"
      // def dockerParams = dockerParams

    break

    default:
      error("Platform name is not defined or unsupported")

    break
  }
}

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
  sh ("'${mvnHome}/bin/mvn' -q versions:set -DallowSnapshots=${allowss} -DgenerateBackupPoms=false -DnewVersion=${ver}")
}

def release(proj) {
  // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
  // Here you need to put stuff for atrifacts releasing

  // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
  // Tag builded branch with new version
  if (CREATE_TAG) {
    echo ("Parameter CREATE_TAG is defined and it is: ${CREATE_TAG}")
    echo ("Adding tag ${proj}-${RELEASE_VERSION} to github.com/${ACCOUNT}/${proj}")
    dir("${proj}") {
      sshagent(credentials: ["${GITCREDID}"]) {
        sh 'git config user.email "jenkins@skymind.io"'
        sh 'git config user.name "Jenkins"'
        sh 'git status'
        // DO NOT ENABLE COMMIT AND TAGGING UNTIL IT IS NEEDED FOR REAL RELEASE
        sh('git commit -a -m \"Update to version ${RELEASE_VERSION}\"')
        sh("git tag -a test-${proj}-${RELEASE_VERSION} -m test-${proj}-${RELEASE_VERSION}")
        // sh("git push origin test-${proj}-${RELEASE_VERSION}")
      }
    }
  }
  else {
      echo ("Parameter CREATE_TAG is undefined so tagging has been skipped")
  }
}

return this;
