def express(text) {
  ansiColor('xterm') {
      echo "\033[1;43m ${text} \033[0m"
  }
}

/*
def info(text) {
  ansiColor('xterm') {
      echo "\033[32m ${text} \033[0m"
  }
}

def warn(text) {
  ansiColor('xterm') {
      echo "\033[43m ${text} \033[0m"
  }
}

def error(text) {
  ansiColor('xterm') {
      echo "\033[41m ${text} \033[0m"
  }
}
*/

def get_project_code(proj) {
    if (isUnix()) {
        if (PLATFORM_NAME == "linux-ppc64le") {
            sh("git clone -b ${GIT_BRANCHNAME} --single-branch https://github.com/${ACCOUNT}/${proj}.git --depth=1")
        } else {
            checkout([$class                           : 'GitSCM',
                      branches                         : [[name: "*/${GIT_BRANCHNAME}"]],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${proj}"],
                                                          [$class: 'CloneOption', honorRefspec: true, noTags: isSnapshot, reference: '', shallow: true, timeout: 30]],
                      //  extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${proj}"], [$class: 'CloneOption', honorRefspec: true, noTags: false, reference: '', shallow: true]],
                      submoduleCfg                     : [],
                      userRemoteConfigs                : [[url: "git@github.com:${ACCOUNT}/${proj}.git", credentialsId: "${GITCREDID}"]]])
        }
    } else {
        // it says - Running on Windowslinux
        // echo "Running on Windows" + System.properties['os.name'].toLowerCase()
        echo "Running on Windows"
        // git 'https://github.com/deeplearning4j/libnd4j.git'
        checkout([$class                           : 'GitSCM',
                  branches                         : [[name: "*/${GIT_BRANCHNAME}"]],
                  doGenerateSubmoduleConfigurations: false,
                  extensions                       : [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${proj}"],
                                                      [$class: 'CloneOption', honorRefspec: true, noTags: isSnapshot, reference: '', shallow: true, timeout: 30]],
                  //  extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${proj}"], [$class: 'CloneOption', honorRefspec: true, noTags: false, reference: '', shallow: true]],
                  submoduleCfg                     : [],
                  userRemoteConfigs                : [[url: "git@github.com:${ACCOUNT}/${proj}.git", credentialsId: "${GITCREDID}"]]])
    }
}

// Remove .git folder and other unneeded files from workspace
def rm() {
    echo "Remove .git folder from workspace - ${WORKSPACE}"
    dir("${WORKSPACE}") {
        if (isUnix()) {
            sh("rm -rf .git* docs docker imgs ansible README.md .gnupg")
        } else {
            echo "Skipping .git deletion because it is windows"
        }

    }
}

def checktag(proj) {
    echo "Check if ${proj}-${VERSION} has been released already"
    def check_tag = sh(returnStdout: true, script: "git tag -l ${proj}-${VERSION}")
    if (check_tag) {
        echo("Version exists: " + check_tag)
        error("Failed to proceed with current version: " + check_tag)
    } else {
        echo("There is no tag with provided value: ${proj}-${VERSION}")
    }
}

def def_docker() {
    echo "Setting docker parameters and image for ${PLATFORM_NAME}"
    switch (PLATFORM_NAME) {
        case "linux-x86_64":
            def nvidia_docker_volume = sh(returnStdout: true, script: "docker volume ls -f DRIVER=nvidia-docker -q").trim()
            if (sh(returnStdout: true, script: "ls -A `docker volume inspect -f \"{{.Mountpoint}}\" ${nvidia_docker_volume}` && true || false")) {
                dockerParams = dockerParams_tmpfs_nvidia + " --volume="+ nvidia_docker_volume + ":/usr/local/nvidia:ro"
            } else {
                sh("ls -A `docker volume inspect -f \"{{.Mountpoint}}\" ${nvidia_docker_volume}`")
                dockerParams = dockerParams_tmpfs_nvidia
            }
            dockerImage = "${DOCKER_CENTOS6_CUDA80_AMD64}"
            break

        case "linux-ppc64le":
            dockerImage = "${DOCKER_CUDA_PPC}"
            dockerParams = dockerParams
            break

        case ["android-arm", "android-x86"]:
            dockerImage = "${DOCKER_ANDROID_IMAGE}"
            dockerParams = dockerParams
            break

        case ["macosx-x86_64", "windows-x86_64"]:
            echo "Running on ${PLATFORM_NAME}, skipping docker part"
            break

        default:
            error("Platform name is not defined or unsupported")
            break
    }
}

// def def_docker(platform, docker_image, docker_params, jenkins_storage) {
//   echo "Setting docker parameters and image for ${PLATFORM_NAME}"
//   switch(platform) {
//     case ["linux-x86_64", "linux-ppc64le", "android-arm", "android-x86"]:
//         echo ("[ INFO ] Parameters: ${platform} ${docker_image} ${docker_params} ${jenkins_storage}")
//         dockerImage = docker_image
//         dockerParams = docker_params
//         sh ("mkdir -p ${jenkins_storage}/docker_m2 ${jenkins_storage}/docker_ivy2")
//         break
//
//     case ["macosx-x86_64","windows-x86_64"]:
//         echo "Running on ${platform}, skipping docker part"
//         break
//
//     default:
//         error("Platform name is not defined or unsupported")
//         break
//   }
// }

def sonar(proj) {
    echo "Check ${ACCOUNT}/${proj} code with SonarQube Scanner"
    // requires SonarQube Scanner 2.8+
    // def scannerHome = tool "${SONAR_SCANNER}";
    dir("${proj}") {
        if (isUnix()) {
          def scannerHome = tool "${SONAR_SCANNER}";
            withSonarQubeEnv("${SONAR_SERVER}") {
              sh "${scannerHome}/bin/sonar-scanner -Dsonar.projectKey=${ACCOUNT}:${proj}:${PLATFORM_NAME} \
                  -Dsonar.projectName=${PLATFORM_NAME}:${proj} -Dsonar.projectVersion=${VERSION} \
                  -Dsonar.sources=."
                // -Dsonar.sources=. -Dsonar.exclusions=**/*reduce*.h"
            }
        } else {
            def scannerHome = tool "${SONAR_SCANNER}";
            withSonarQubeEnv("${SONAR_SERVER}") {
              bat("${scannerHome}/bin/sonar-scanner -Dsonar.projectKey=${ACCOUNT}:${proj}:${PLATFORM_NAME} \
                  -Dsonar.projectName=${PLATFORM_NAME}:${proj} -Dsonar.projectVersion=${VERSION} \
                  -Dsonar.sources=.")

        }
    }
}

// mvn versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=$VERSION
def verset(ver, allowss) {
    def mvnHome = tool 'M339'
    if (isUnix()) {
        sh("'${mvnHome}/bin/mvn' -q versions:set -DallowSnapshots=${allowss} -DgenerateBackupPoms=false -DnewVersion=${ver}")
    } else {
        bat("mvn -q versions:set -DallowSnapshots=${allowss} -DgenerateBackupPoms=false -DnewVersion=${ver}")
    }
}

def tag(proj) {
    // Tag builded branch with new version
    if (CREATE_TAG.toBoolean()) {
        echo("Parameter CREATE_TAG is defined and it is: ${CREATE_TAG}")
        ansiColor('xterm') {
            echo("\033[43m Adding tag ${proj}-${VERSION} to github.com/${ACCOUNT}/${proj} \033[0m")
        }
        dir("${proj}") {
            sshagent(credentials: ["${GITCREDID}"]) {
                sh 'git config user.email "jenkins@skymind.io"'
                sh 'git config user.name "Jenkins"'
                sh 'git status'
                // Disabled commit to avoid
                // 'nothing to commit, working directory clean' which returns 1
                // sh('git commit -a -m \"Update to version ${VERSION}\"')
                ansiColor('xterm') {
                    sh("git tag -a ${proj}-${VERSION} -m ${proj}-${VERSION}")
                    echo("\033[42m Tag ${proj}-${VERSION} has been added to locally copied github.com/${ACCOUNT}/${proj} \033[0m")
                }
                if(TAG.toBoolean()) {
                    ansiColor('xterm') {
                        sh("git push origin ${proj}-${VERSION}")
                        echo("\033[42m Tag ${proj}-${VERSION} has been pushed to github.com/${ACCOUNT}/${proj} \033[0m")
                    }
                }
            }
        }
    } else {
          ansiColor('xterm') {
              echo("\033[33m Parameter CREATE_TAG is undefined so tagging has been skipped \033[0m")
          }
    }
}

def getGpg() {
    withCredentials([
            file(credentialsId: 'gpg-pub-key-test-1', variable: 'GPG_PUBRING'),
            file(credentialsId: 'gpg-private-key-test-1', variable: 'GPG_SECRING'),
            usernameColonPassword(credentialsId: 'gpg-password-test-1', variable: 'GPG_PASS')]) {
        if (isUnix()) {
            sh("rm -rf ${HOME}/.gnupg/*.gpg")
            sh '''
                gpg --list-keys
                cp ${GPG_PUBRING} ${HOME}/.gnupg/
                cp ${GPG_SECRING} ${HOME}/.gnupg/
                chmod 700 $HOME/.gnupg
                chmod 600 $HOME/.gnupg/secring.gpg $HOME/.gnupg/pubring.gpg
                gpg --list-keys
                '''
        } else {
//            sh("env")
            // It says - Running on Windowslinux
            // echo "Running on Windows" + System.properties['os.name'].toLowerCase()
            echo "Running on Windows"
            bat '''
                bash -c "rm -rf ${HOME}/.gnupg/*.gpg"
                bash -c "gpg --list-keys"
                bash -c "cp ${GPG_PUBRING} ${HOME}/.gnupg/"
                bash -c "cp ${GPG_SECRING} ${HOME}/.gnupg/"
                bash -c "chmod 700 $HOME/.gnupg"
                bash -c "chmod 600 $HOME/.gnupg/secring.gpg $HOME/.gnupg/pubring.gpg"
                bash -c "gpg --list-keys"
                bash -c "gpg.exe --list-keys"
                '''
        }
    }
}

def upload_libnd4j_snapshot_version_to_snapshot_repository(version, platform, profile_type) {
    configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
        if (isUnix()) {
//            sh("tar -cvf ${LIBPROJECT}-${version}-${platform}.tar blasbuild")
            zip dir: "${WORKSPACE}/libnd4j/blasbuild", zipFile: "${LIBPROJECT}-${version}-${platform}.zip"
            switch (profile_type) {
                case "nexus":
                    sh("mvn -B -s ${MAVEN_SETTINGS} deploy:deploy-file -Durl=http://master-jenkins.eastus.cloudapp.azure.com:8088/nexus/content/repositories/snapshots " +
                            "-DgroupId=org.nd4j " +
                            "-DartifactId=${LIBPROJECT} " +
                            "-Dversion=${version} " +
                            "-Dpackaging=zip " +
                            "-DrepositoryId=local-nexus " +
                            "-Dclassifier=${platform} " +
                            "-Dfile=${LIBPROJECT}-${version}-${platform}.zip")
                    break
                case "sonatype":
                    sh("mvn -B -s ${MAVEN_SETTINGS} deploy:deploy-file -Durl=https://oss.sonatype.org/content/repositories/snapshots " +
                            "-DgroupId=org.nd4j " +
                            "-DartifactId=${LIBPROJECT} " +
                            "-Dversion=${version} " +
                            "-Dpackaging=zip " +
                            "-DrepositoryId=sonatype-nexus-snapshots " +
                            "-Dclassifier=${platform} " +
                            "-Dfile=${LIBPROJECT}-${version}-${platform}.zip")
                    break
                case "bintray":
                    sh("mvn -B -s ${MAVEN_SETTINGS} deploy:deploy-file -Durl=https://oss.jfrog.org/artifactory/oss-snapshot-local " +
                            "-DgroupId=org.nd4j " +
                            "-DartifactId=${LIBPROJECT} " +
                            "-Dversion=${version} " +
                            "-Dpackaging=zip " +
                            "-DrepositoryId=bintray-deeplearning4j-maven " +
                            "-Dclassifier=${platform} " +
                            "-Dfile=${LIBPROJECT}-${version}-${platform}.zip")
                    break
                case "jfrog":
                    sh("mvn -B -s ${MAVEN_SETTINGS} deploy:deploy-file -Durl=http://master-jenkins.eastus.cloudapp.azure.com:8081/artifactory/libs-snapshot-local " +
                            "-DgroupId=org.nd4j " +
                            "-DartifactId=${LIBPROJECT} " +
                            "-Dversion=${version} " +
                            "-Dpackaging=zip " +
                            "-DrepositoryId=local-jfrog " +
                            "-Dclassifier=${platform} " +
                            "-Dfile=${LIBPROJECT}-${version}-${platform}.zip")
                    break
            }
        } else {
//            bat("tar -cvf %LIBPROJECT%-%version%-%platform%.tar blasbuild")
            zip dir: "${WORKSPACE}\\libnd4j\\blasbuild", zipFile: "${LIBPROJECT}-${version}-${platform}.zip"
            switch (profile_type) {
                case "nexus":
                    bat("mvn -B -s ${MAVEN_SETTINGS} deploy:deploy-file -Durl=http://master-jenkins.eastus.cloudapp.azure.com:8088/nexus/content/repositories/snapshots " +
                            "-DgroupId=org.nd4j " +
                            "-DartifactId=${LIBPROJECT} " +
                            "-Dversion=${version} " +
                            "-Dpackaging=zip " +
                            "-DrepositoryId=local-nexus " +
                            "-Dclassifier=${platform} " +
                            "-Dfile=${LIBPROJECT}-${version}-${platform}.zip")
                    break
                case "sonatype":
                    bat("mvn -B -s ${MAVEN_SETTINGS} deploy:deploy-file -Durl=https://oss.sonatype.org/content/repositories/snapshots " +
                            "-DgroupId=org.nd4j " +
                            "-DartifactId=${LIBPROJECT} " +
                            "-Dversion=${version} " +
                            "-Dpackaging=zip " +
                            "-DrepositoryId=sonatype-nexus-snapshots " +
                            "-Dclassifier=${platform} " +
                            "-Dfile=${LIBPROJECT}-${version}-${platform}.zip")
                    break
                case "bintray":
                    bat("mvn -B -s ${MAVEN_SETTINGS} deploy:deploy-file -Durl=https://oss.jfrog.org/artifactory/oss-snapshot-local " +
                            "-DgroupId=org.nd4j " +
                            "-DartifactId=${LIBPROJECT} " +
                            "-Dversion=${version} " +
                            "-Dpackaging=zip " +
                            "-DrepositoryId=bintray-deeplearning4j-maven " +
                            "-Dclassifier=${platform} " +
                            "-Dfile=${LIBPROJECT}-${version}-${platform}.zip")
                    break
                case "jfrog":
                    bat("mvn -B -s ${MAVEN_SETTINGS} deploy:deploy-file -Durl=http://master-jenkins.eastus.cloudapp.azure.com:8081/artifactory/libs-snapshot-local " +
                            "-DgroupId=org.nd4j " +
                            "-DartifactId=${LIBPROJECT} " +
                            "-Dversion=${version} " +
                            "-Dpackaging=zip " +
                            "-DrepositoryId=local-jfrog " +
                            "-Dclassifier=${platform} " +
                            "-Dfile=${LIBPROJECT}-${version}-${platform}.zip")
                    break
            }
        }
    }
}

def get_libnd4j_artifacts_snapshot_ball(version, platform, profile_type) {
    switch (profile_type) {
        case "nexus":
            if (isUnix()) {
                sh("mvn -B dependency:get -DrepoUrl=http://master-jenkins.eastus.cloudapp.azure.com:8088/nexus/content/repositories/snapshots " +
                        "-DgroupId=org.nd4j -DartifactId=${LIBPROJECT} -Dversion=${VERSION} -Dpackaging=zip " +
                        "-Dtransitive=false " +
                        "-Dclassifier=${platform} " +
                        "-Ddest=${LIBPROJECT}-${version}-${platform}.zip ")
            } else {
                bat("mvn -B dependency:get -DrepoUrl=http://master-jenkins.eastus.cloudapp.azure.com:8088/nexus/content/repositories/snapshots " +
                        "-DgroupId=org.nd4j -DartifactId=${LIBPROJECT} -Dversion=${VERSION} -Dpackaging=zip " +
                        "-Dtransitive=false " +
                        "-Dclassifier=${platform} " +
                        "-Ddest=${LIBPROJECT}-${version}-${platform}.zip ")
            }
            break
        case "sonatype":

            if (isUnix()) {
                sh("mvn -B dependency:get -DrepoUrl=https://oss.sonatype.org/content/repositories/snapshots " +
                        "-DgroupId=org.nd4j -DartifactId=${LIBPROJECT} -Dversion=${VERSION} -Dpackaging=zip " +
                        "-Dtransitive=false " +
                        "-Dclassifier=${platform} " +
                        "-Ddest=${LIBPROJECT}-${version}-${platform}.zip ")
            } else {
                bat("mvn -B dependency:get -DrepoUrl=https://oss.sonatype.org/content/repositories/snapshots " +
                        "-DgroupId=org.nd4j -DartifactId=${LIBPROJECT} -Dversion=${VERSION} -Dpackaging=zip " +
                        "-Dtransitive=false " +
                        "-Dclassifier=${platform} " +
                        "-Ddest=${LIBPROJECT}-${version}-${platform}.zip ")
            }
            break
        case "bintray":
            if (isUnix()) {
                sh("mvn -B dependency:get -DrepoUrl=https://oss.jfrog.org/artifactory/oss-snapshot-local " +
                        "-DgroupId=org.nd4j -DartifactId=${LIBPROJECT} -Dversion=${VERSION} -Dpackaging=zip " +
                        "-Dtransitive=false " +
                        "-Dclassifier=${platform} " +
                        "-Ddest=${LIBPROJECT}-${version}-${platform}.zip ")
            } else {
                bat("mvn -B dependency:get -DrepoUrl=https://oss.jfrog.org/artifactory/oss-snapshot-local " +
                        "-DgroupId=org.nd4j -DartifactId=${LIBPROJECT} -Dversion=${VERSION} -Dpackaging=zip " +
                        "-Dtransitive=false " +
                        "-Dclassifier=${platform} " +
                        "-Ddest=${LIBPROJECT}-${version}-${platform}.zip ")
            }
            break
        case "jfrog":
            if (isUnix()) {
                sh("mvn -B dependency:get -DrepoUrl=http://master-jenkins.eastus.cloudapp.azure.com:8081/artifactory/libs-snapshot-local " +
                        "-DgroupId=org.nd4j -DartifactId=${LIBPROJECT} -Dversion=${VERSION} -Dpackaging=zip " +
                        "-Dtransitive=false " +
                        "-Dclassifier=${platform} " +
                        "-Ddest=${LIBPROJECT}-${version}-${platform}.zip ")
            } else {
                bat("mvn -B dependency:get -DrepoUrl=http://master-jenkins.eastus.cloudapp.azure.com:8081/artifactory/libs-snapshot-local " +
                        "-DgroupId=org.nd4j -DartifactId=${LIBPROJECT} -Dversion=${VERSION} -Dpackaging=zip " +
                        "-Dtransitive=false " +
                        "-Dclassifier=${platform} " +
                        "-Ddest=${LIBPROJECT}-${version}-${platform}.zip ")
            }
            break
    }
}

def download_nd4j_native_from_jenkins_user_content(version) {
//    def listPlatformVersion = ["linux-x86_64", "android-arm", "android-x86", "linux-ppc64le", "macosx-x86_64", "windows-x86_64"]
    def listPlatformVersion = ["android-arm", "android-x86", "linux-ppc64le", "macosx-x86_64", "windows-x86_64"]
    for (int i = 0; i < listPlatformVersion.size(); i++) {
        echo("[ INFO ] Try download nd4j-native version : " + "nd4j-native-${version}-${listPlatformVersion[i]}.jar")
        sh("wget --no-verbose ${JENKINS_URL}/userContent/nd4j-native-${version}-${listPlatformVersion[i]}.jar")
    }
}

def install_nd4j_native_to_local_maven_repository(version) {
//    def listPlatformVersion = ["linux-x86_64", "android-arm", "android-x86", "linux-ppc64le", "macosx-x86_64", "windows-x86_64"]
    def listPlatformVersion = ["android-arm", "android-x86", "linux-ppc64le", "macosx-x86_64", "windows-x86_64"]
    for (int i = 0; i < listPlatformVersion.size(); i++) {
        echo("[ INFO ] Try install nd4j-native version  : " + "nd4j-native-${version}-${listPlatformVersion[i]}.jar " + " - into local Maven repository")
        sh("mvn -B install:install-file -Dfile=nd4j-native-${version}-${listPlatformVersion[i]}.jar -DgroupId=org.nd4j -DartifactId=nd4j-native -Dversion=${version} -Dpackaging=jar -Dclassifier=${listPlatformVersion[i]}")
    }
}

def open_staging_repository(profile_type) {
    switch (profile_type) {
        case "nexus":
            withCredentials([[$class          : 'UsernamePasswordMultiBinding', credentialsId: 'local-nexus-admin-user-1',
                              usernameVariable: 'LOCAL_NEXUS_USER', passwordVariable: 'LOCAL_NEXUS_USER_PASSWORD']]) {
                env.STAGE_REPO_ID = sh(
                        script: "curl -u ${LOCAL_NEXUS_USER}:${LOCAL_NEXUS_USER_PASSWORD} -H 'Accept: application/xml' -H 'Content-Type: application/xml' " +
                                "-X POST \"http://master-jenkins.eastus.cloudapp.azure.com:8088/nexus/service/local/staging/profiles/1a9c575a8e8c/start\" " +
                                "-d \"<promoteRequest><data><description>Jenkins(Skymind) :: version:${VERSION} :: job:${JOB_NAME} :: build:${BUILD_NUMBER}</description></data></promoteRequest>\" | grep stagedRepositoryId | sed -e 's,.*<stagedRepositoryId>\\([^<]*\\)</stagedRepositoryId>.*,\\1,g'",
                        returnStdout: true
                ).trim()
                if (env.STAGE_REPO_ID != null && env.STAGE_REPO_ID.length() > 0) {
                  ansiColor('xterm') {
                    echo("[ LOCAL-NEXUS ]")
                    echo("\033[1;43m [ INFO ] local-nexus stagingRepositoryId is:" + "${STAGE_REPO_ID} \033[0m")
                  }
                } else {
                    error "[ ERROR ] Error appear in local-nexus REST API call during to OPEN request..."
                }
            }
            break
        case "sonatype":
            withCredentials([[$class          : 'UsernamePasswordMultiBinding', credentialsId: 'sonatype-nexus-intropro-user-1',
                              usernameVariable: 'LOCAL_NEXUS_USER', passwordVariable: 'LOCAL_NEXUS_USER_PASSWORD']]) {
                env.STAGE_REPO_ID = sh(
                        script: "curl -u ${LOCAL_NEXUS_USER}:${LOCAL_NEXUS_USER_PASSWORD} -H 'Accept: application/xml' -H 'Content-Type: application/xml' " +
                                "-X POST \"https://oss.sonatype.org:443/service/local/staging/profiles/747720f52eb29e/start\" " +
                                "-d \"<promoteRequest><data><description>Jenkins(Skymind) :: version:${VERSION} :: job:${JOB_NAME} :: build:${BUILD_NUMBER}</description></data></promoteRequest>\" | grep stagedRepositoryId | sed -e 's,.*<stagedRepositoryId>\\([^<]*\\)</stagedRepositoryId>.*,\\1,g'",
                        returnStdout: true
                ).trim()
                if (env.STAGE_REPO_ID != null && env.STAGE_REPO_ID.length() > 0) {
                    echo("[ LOCAL-NEXUS ]")
                    ansiColor('xterm') {
                        echo("\033[1;43m [ INFO ] local-nexus stagingRepositoryId is:" + "${STAGE_REPO_ID} \033[0m")
                    }
                } else {
                    error "[ ERROR ] Error appear in local-nexus REST API call..."
                }
            }
            break
        case "jfrog":
            break
        case "bintray":
            break
        default:
            ansiColor('xterm') {
                echo("\033[41m Unknown repository \033[0m")
            }
            break
    }
}

def close_staging_repository(profile_type) {
    switch (profile_type) {
        case "nexus":
            echo("[ LOCAL-NEXUS ]")
            echo("[ INFO ] Try to CLOSE stagingRepositoryId :" + "${STAGE_REPO_ID}")
            withCredentials([[$class          : 'UsernamePasswordMultiBinding', credentialsId: 'local-nexus-admin-user-1',
                              usernameVariable: 'LOCAL_NEXUS_USER', passwordVariable: 'LOCAL_NEXUS_USER_PASSWORD']]) {
                // do something that fails
                env.CLOSE_RESULT = sh(script: "curl -u ${LOCAL_NEXUS_USER}:${LOCAL_NEXUS_USER_PASSWORD} -H 'Accept: application/xml' -H 'Content-Type: application/xml' " +
                        "-X POST \"http://master-jenkins.eastus.cloudapp.azure.com:8088/nexus/service/local/staging/bulk/close\"" +
                        " -d \"<stagingActionRequest><data><stagedRepositoryIds><string>${STAGE_REPO_ID}</string></stagedRepositoryIds><autoDropAfterRelease>false</autoDropAfterRelease></data></stagingActionRequest>\" | wc -l",
                        returnStdout: true
                ).trim()
                echo "CLOSE_RESULT:" + " ${CLOSE_RESULT}"
                if (env.CLOSE_RESULT != null && env.CLOSE_RESULT.toInteger() > 0) {
                    sh(script: "curl -u ${LOCAL_NEXUS_USER}:${LOCAL_NEXUS_USER_PASSWORD} -H 'Accept: application/xml' -H 'Content-Type: application/xml' " +
                            "-X POST \"http://master-jenkins.eastus.cloudapp.azure.com:8088/nexus/service/local/staging/bulk/close\"" +
                            " -d \"<stagingActionRequest><data><stagedRepositoryIds><string>${STAGE_REPO_ID}</string></stagedRepositoryIds><autoDropAfterRelease>false</autoDropAfterRelease></data></stagingActionRequest>\""
                    )
                    error "[ ERROR ] Error appear in local-nexus REST API call during CLOSE request..."
                } else {
                    echo("[ LOCAL-NEXUS ]")
                    echo("[ INFO ] local-nexus stagingRepositoryId :" + "${STAGE_REPO_ID}" + " is CLOSED")
                }
            }
            break
        case "sonatype-nexus":
            echo("[ SONATYPE ]")
            echo("[ INFO ] sonatype-nexusTry to CLOSE stagingRepositoryId :" + "${STAGE_REPO_ID}")
            withCredentials([[$class          : 'UsernamePasswordMultiBinding', credentialsId: 'local-nexus-admin-user-1',
                              usernameVariable: 'LOCAL_NEXUS_USER', passwordVariable: 'LOCAL_NEXUS_USER_PASSWORD']]) {
                // do something that fails
                CLOSE_RESULT = sh(script: "curl -u ${LOCAL_NEXUS_USER}:${LOCAL_NEXUS_USER_PASSWORD} -H 'Accept: application/xml' -H 'Content-Type: application/xml' " +
                        "-X POST \"https://oss.sonatype.org:443/service/local/staging/bulk/close\"" +
                        " -d \"<stagingActionRequest><data><stagedRepositoryIds><string>${STAGE_REPO_ID}</string></stagedRepositoryIds><autoDropAfterRelease>false</autoDropAfterRelease></data></stagingActionRequest>\" | wc -l",
                        returnStdout: true
                ).trim()
                if (env.CLOSE_RESULT != null && env.CLOSE_RESULT.toInteger() > 0) {
                    sh(script: "curl -u ${LOCAL_NEXUS_USER}:${LOCAL_NEXUS_USER_PASSWORD} -H 'Accept: application/xml' -H 'Content-Type: application/xml' " +
                            "-X POST \"https://oss.sonatype.org:443/service/local/staging/bulk/close\"" +
                            " -d \"<stagingActionRequest><data><stagedRepositoryIds><string>${STAGE_REPO_ID}</string></stagedRepositoryIds><autoDropAfterRelease>false</autoDropAfterRelease></data></stagingActionRequest>\""
                    )
                    error "[ ERROR ] Error appear in local-nexus REST API call during CLOSE request..."
                } else {
                    echo("[ SONATYPE-NEXUS ]")
                    echo("[ INFO ] sonatype-nexus stagingRepositoryId :" + "${STAGE_REPO_ID}" + " is CLOSED")
                }
            }
            break
        case "local-jfrog":
            break
        case "bintray-jfrog":
            break
        default:
            echo("Unknown repository")
            break
    }
}

def resolve_dependencies_for_nd4j() {
    echo("[ INFO ] Check is there was build for ${LIBPROJECT}")
    Boolean BLASBUILD_CHECK = fileExists "${LIBPROJECT}/blasbuild"

    echo("[ INFO ] BLASBUILD_CHECK is result: " + BLASBUILD_CHECK)

    if (BLASBUILD_CHECK) {
        echo("[ INFO ] ${LIBPROJECT} project was previously builded...")
    } else {
        echo("[ INFO ] ${LIBPROJECT} wasn't build previously")
        echo("[ INFO ] Download sources for ${LIBPROJECT} ")
        functions.get_project_code("${LIBPROJECT}")
        echo("[ INFO ] Resolve dependencies related to ${LIBPROJECT} ")
        functions.get_libnd4j_artifacts_snapshot_ball("${VERSION}", "${PLATFORM_NAME}", "${PROFILE_TYPE}")

        if (isUnix()) {
            unzip zipFile: "${WORKSPACE}/${LIBPROJECT}-${VERSION}-${PLATFORM_NAME}.zip", dir: "${WORKSPACE}/${LIBPROJECT}/blasbuild"
        } else {
            unzip zipFile: "${WORKSPACE}\\${LIBPROJECT}-${VERSION}-${PLATFORM_NAME}.zip", dir: "${WORKSPACE}\\${LIBPROJECT}\\blasbuild"
        }
    }
}

def cleanup_userContent() {
    dir("${JENKINS_HOME}/userContent/nd4j_artifacts") {
        sh("rm -rf *.jar")
    }
}

def copy_nd4j_native_to_user_content() {
    // def listPlatformVersion = ["android-arm", "android-x86", "linux-ppc64le", "macosx-x86_64", "windows-x86_64"]
    // for (int i = 0; i < listPlatformVersion.size(); i++) {
    stash includes: "nd4j-backends/nd4j-backend-impls/nd4j-native/target/nd4j-native-${VERSION}-${PLATFORM_NAME}.jar", name: "nd4j-${PLATFORM_NAME}-${BUILD_NUMBER}"
    node("master") {
        unstash "nd4j-${PLATFORM_NAME}-${BUILD_NUMBER}"
        echo("[ INFO ] Copying nd4j jar to userContent for release")
        sh("find . -name *nd4j-native*.jar")
        sh("cp ./nd4j-backends/nd4j-backend-impls/nd4j-native/target/nd4j-native-${VERSION}-${PLATFORM_NAME}.jar ${JENKINS_HOME}/userContent/nd4j_artifacts/")
    }
    // }
}

def copy_nd4j_native_from_user_content() {
    node("master") {
        dir("${JENKINS_HOME}/userContent/nd4j_artifacts") {
            sh("ls -la")
            stash includes: '*.jar', name: "nd4j-${PLATFORM_NAME}-${BUILD_NUMBER}"
        }
    }
    unstash "nd4j-${PLATFORM_NAME}-${BUILD_NUMBER}"
}

def nd4s_install_snapshot_dependencies_to_maven2_local_repository( group_id, artifact_id, version, packaging, classifier, profile_type ) {
    switch (profile_type) {
        case "sonatype":
            repo_url="http://oss.sonatype.org/content/repositories/snapshots"
            for (int i = 0; i < classifier.size(); i++){
                echo("[ INFO ] try to download  and install dependencies of given artifact: " + group_id + ":" +  artifact_id + ":" +version + ":" +packaging + ":" + classifier[i] )
                if (isUnix()) {
                    sh("mvn -B dependency:get -DrepoUrl=${repo_url} -DgroupId=${group_id} -DartifactId=${artifact_id} -Dversion=${version} -Dpackaging=${packaging} -Dclassifier=${classifier[i]}")
                } else {
                    bat("mvn -B dependency:get -DrepoUrl=${repo_url} -DgroupId=${group_id} -DartifactId=${artifact_id} -Dversion=${version} -Dpackaging=${packaging} -Dclassifier=${classifier[i]}")
                }
            }
            break
        case "nexus":
            repo_url="http://master-jenkins.eastus.cloudapp.azure.com:8088/nexus/content/repositories/snapshots"
            for (int i = 0; i < classifier.size(); i++){
                echo("[ INFO ] try to download  and install dependencies of given artifact: " + group_id + ":" +  artifact_id + ":" +version + ":" +packaging + ":" + classifier[i] )
                if (isUnix()) {
                    sh("mvn -B dependency:get -DrepoUrl=${repo_url} -DgroupId=${group_id} -DartifactId=${artifact_id} -Dversion=${version} -Dpackaging=${packaging} -Dclassifier=${classifier[i]}")
                } else {
                    bat("mvn -B dependency:get -DrepoUrl=${repo_url} -DgroupId=${group_id} -DartifactId=${artifact_id} -Dversion=${version} -Dpackaging=${packaging} -Dclassifier=${classifier[i]}")
                }
            }
            break
        case "jfrog":
            repo_url="http://master-jenkins.eastus.cloudapp.azure.com:8081/artifactory/libs-snapshot-local"
            for (int i = 0; i < classifier.size(); i++){
                echo("[ INFO ] try to download  and install dependencies of given artifact: " + group_id + ":" +  artifact_id + ":" +version + ":" +packaging + ":" + classifier[i] )
                if (isUnix()) {
                    sh("mvn -B dependency:get -DrepoUrl=${repo_url} -DgroupId=${group_id} -DartifactId=${artifact_id} -Dversion=${version} -Dpackaging=${packaging} -Dclassifier=${classifier[i]}")
                } else {
                    bat("mvn -B dependency:get -DrepoUrl=${repo_url} -DgroupId=${group_id} -DartifactId=${artifact_id} -Dversion=${version} -Dpackaging=${packaging} -Dclassifier=${classifier[i]}")
                }
            }
            break
        case "bintray":
            repo_url="https://oss.jfrog.org/artifactory/oss-snapshot-local"
            for (int i = 0; i < classifier.size(); i++){
                echo("[ INFO ] try to download  and install dependencies of given artifact: " + group_id + ":" +  artifact_id + ":" +version + ":" +packaging + ":" + classifier[i] )
                if (isUnix()) {
                    sh("mvn -B dependency:get -DrepoUrl=${repo_url} -DgroupId=${group_id} -DartifactId=${artifact_id} -Dversion=${version} -Dpackaging=${packaging} -Dclassifier=${classifier[i]}")
                } else {
                    bat("mvn -B dependency:get -DrepoUrl=${repo_url} -DgroupId=${group_id} -DartifactId=${artifact_id} -Dversion=${version} -Dpackaging=${packaging} -Dclassifier=${classifier[i]}")
                }
            }
            break
        default:
            break
    }
}

return this;
