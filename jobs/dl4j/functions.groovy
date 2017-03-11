def get_project_code(proj) {
  if (isUnix()) {
    checkout([$class: 'GitSCM',
              branches: [[name: "*/${GIT_BRANCHNAME}"]],
              doGenerateSubmoduleConfigurations: false,
              extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${proj}"],
                          [$class: 'CloneOption', honorRefspec: true, noTags: isSnapshot, reference: '', shallow: true, timeout: 30]],
              //  extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${proj}"], [$class: 'CloneOption', honorRefspec: true, noTags: false, reference: '', shallow: true]],
              submoduleCfg: [],
              userRemoteConfigs: [[url: "git@github.com:${ACCOUNT}/${proj}.git", credentialsId: "${GITCREDID}"]]])
  } else {
      // it says - Running on Windowslinux
      // echo "Running on Windows" + System.properties['os.name'].toLowerCase()
      echo "Running on Windows"
      // git 'https://github.com/deeplearning4j/libnd4j.git'
      checkout([$class: 'GitSCM',
                branches: [[name: "*/${GIT_BRANCHNAME}"]],
                doGenerateSubmoduleConfigurations: false,
                extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${proj}"],
                            [$class: 'CloneOption', honorRefspec: true, noTags: isSnapshot, reference: '', shallow: true, timeout: 30]],
                //  extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${proj}"], [$class: 'CloneOption', honorRefspec: true, noTags: false, reference: '', shallow: true]],
                submoduleCfg: [],
                userRemoteConfigs: [[url: "git@github.com:${ACCOUNT}/${proj}.git", credentialsId: "${GITCREDID}"]]])
  }
}

// Remove .git folder and other unneeded files from workspace
def rm() {
  echo "Remove .git folder from workspace - ${WORKSPACE}"
  dir("${WORKSPACE}") {
    if (isUnix()) {
        sh("rm -rf {.git,.gitignore,docs,imgs,ansible,README.md,.gnupg}")
    } else {
        echo "Skipping .git deletion because it is windows"
    }

  }
}

def checktag(proj) {
  echo "Check if ${proj}-${VERSION} has been released already"
  def check_tag = sh(returnStdout: true, script: "git tag -l ${proj}-${VERSION}")
    if (check_tag) {
      echo ("Version exists: " + check_tag)
      error("Failed to proceed with current version: " + check_tag)
    }
    else {
      echo ("There is no tag with provided value: ${proj}-${VERSION}" )
    }
}

def dirm2() {
  sh ("mkdir -p ${WORKSPACE}/.m2 #/var/lib/jenkins/tools/docker_m2 /var/lib/jenkins/tools/docker_ivy2")
}

def def_docker() {
  echo "Setting docker parameters and image for ${PLATFORM_NAME}"
  switch(PLATFORM_NAME) {
    case "linux-ppc64le":
      dockerImage = "${DOCKER_CUDA_PPC}"
      dockerParams = dockerParams_ppc
      sh ("mkdir -p ${JENKINS_M2DIR_PPC64LE} ${JENKINS_SBTDIR_PPC64LE}")
      break

    case "linux-x86_64":
      dockerImage = "${DOCKER_CENTOS6_CUDA80_AMD64}"
      dockerParams = dockerParams_nvidia
      sh ("mkdir -p ${JENKINS_M2DIR_AMD64} ${JENKINS_SBTDIR_AMD64}")
      break

    case ["android-arm", "android-x86"]:
        dockerImage = "${DOCKER_ANDROID_IMAGE}"
        sh ("mkdir -p ${JENKINS_M2DIR_AMD64} ${JENKINS_SBTDIR_AMD64}")
        break

    case ["macosx"]:
        echo "Running on OS X, skipping docker part"
        break

    case ["windows-x86_64"]:
        echo "Running on windows, skipping docker part"
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
          -Dsonar.projectName=${proj} -Dsonar.projectVersion=${VERSION} \
          -Dsonar.sources=."
          // -Dsonar.sources=. -Dsonar.exclusions=**/*reduce*.h"
    }
  }
}

// mvn versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=$VERSION
def verset(ver, allowss) {
  def mvnHome = tool 'M339'
  sh ("'${mvnHome}/bin/mvn' -q versions:set -DallowSnapshots=${allowss} -DgenerateBackupPoms=false -DnewVersion=${ver}")
}

def release(proj) {
  // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
  // Here you need to put stuff for atrifacts releasing

  // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
  // Tag builded branch with new version
  if (CREATE_TAG.toBoolean()) {
    echo ("Parameter CREATE_TAG is defined and it is: ${CREATE_TAG}")
    echo ("Adding tag ${proj}-${VERSION} to github.com/${ACCOUNT}/${proj}")
    dir("${proj}") {
      sshagent(credentials: ["${GITCREDID}"]) {
        sh 'git config user.email "jenkins@skymind.io"'
        sh 'git config user.name "Jenkins"'
        sh 'git status'
        // DO NOT ENABLE COMMIT AND TAGGING UNTIL IT IS NEEDED FOR REAL RELEASE
        sh('git commit -a -m \"Update to version ${VERSION}\"')
        sh("git tag -a test-${proj}-${VERSION} -m test-${proj}-${VERSION}")
        // sh("git push origin test-${proj}-${VERSION}")
      }
    }
  }
  else {
      echo ("Parameter CREATE_TAG is undefined so tagging has been skipped")
  }
}

def getGpg() {
    withCredentials([
            file(credentialsId: 'gpg-pub-key-test-1', variable: 'GPG_PUBRING'),
            file(credentialsId: 'gpg-private-key-test-1', variable: 'GPG_SECRING'),
            usernameColonPassword(credentialsId: 'gpg-password-test-1', variable: 'GPG_PASS')]) {
            if (isUnix()) {
                sh("rm -rf ${HOME}/.gnupg/*.gpg")
                sh'''
                gpg --list-keys
                cp ${GPG_PUBRING} ${HOME}/.gnupg/
                cp ${GPG_SECRING} ${HOME}/.gnupg/
                chmod 700 $HOME/.gnupg
                chmod 600 $HOME/.gnupg/secring.gpg $HOME/.gnupg/pubring.gpg
                gpg --list-keys
                '''
            } else {
                sh("env")
                // It says - Running on Windowslinux
                // echo "Running on Windows" + System.properties['os.name'].toLowerCase()
                echo "Running on Windows"
                bat'''
                rm -rf %USERPROFILE%/.gnupg/*.gpg
                ls -la %USERPROFILE%/.gnupg
                gpg.exe --list-keys
                echo %GPG_PUBRING% %GPG_SECRING% %HOME%
                cp %GPG_PUBRING% %USERPROFILE%/.gnupg/
                cp %GPG_SECRING% %USERPROFILE%/.gnupg/
                ls -la %USERPROFILE%/.gnupg
                gpg.exe --list-keys
                '''
            }
        }
}

return this;
