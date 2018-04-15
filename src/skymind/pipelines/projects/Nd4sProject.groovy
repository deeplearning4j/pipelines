package skymind.pipelines.projects

import groovy.transform.InheritConstructors

@InheritConstructors
class Nd4sProject extends Project {
    private String projectVersion = '0.9.2-SNAPSHOT'

    void initPipeline() {
        allocateBuildNode {
            script.dir(projectName) {
                script.stage('Get nd4j artifacts') {
                    getNd4jArtifacts()
                }

                script.stage('Build') {
                    runBuild()
                }

//                    script.stage('Test') {
//                        runTests()
//                    }
//
//                    if (branchName == 'master') {
//                        script.stage('Deploy') {
//                            runDeploy()
//                        }
//                    }
            }
        }
    }

    @NonCPS
    protected void runBuild() {
        Map sbtCredentialsIds = [
                nexus: 'sbt-local-nexus-id-1',
                jfrog: 'sbt-local-jfrog-id-1',
                sonatype: 'sbt-oss-sonatype-id-1',
                bintray: 'sbt-oss-bintray-id-1'
        ]
//        String ivyHome = "${script.env.WORKSPACE}/.ivy2"
//        /*
//            Mount point of ivy2 folder for Docker container.
//            Because by default Jenkins mounts current working folder in Docker container,
//            we need to add custom mount.
//         */
//        script.env.IVY_DOCKER_FOLDER = '/tmp/.ivy2'
//        String ivy2Mount = " -v ${ivyHome}:${script.env.IVY_DOCKER_FOLDER}:rw,z"
//
//        /* Create IVY_HOME folder if it doesn't exists */
//        script.sh "test -d ${ivyHome} || mkdir -p ${ivyHome}"

        /* Populate SBT credentials */
        for (def sbtCredentialsId : sbtCredentialsIds) {
            String credentialsId = sbtCredentialsId.value
            String repositoryName = sbtCredentialsId.key

            script.configFileProvider([
                    script.configFile(fileId: "${credentialsId}", variable: 'SBT_CREDENTIALS')
            ]) {
//                script.sh "cp ${script.env.SBT_CREDENTIALS} ${ivyHome}/.${repositoryName}"
                /* FIXME: not working logic below */
                script.sh 'test -d $HOME/.' + repositoryName + ' && mkdir -p $HOME/.' + repositoryName
                script.sh "cp ${script.env.SBT_CREDENTIALS} " + '$HOME' + "/.${repositoryName}"
            }
        }

//        script.env.PROJECT_VERSION = projectVersion
//        script.env.STAGE_REPOSITORY_ID = stageRepositoryId
//
//        script.sh '''\
//            cp -a ${IVY_DOCKER_FOLDER} ${HOME}/
//            cp ${HOME}/.${MAVEN_PROFILE_ACTIVATION_NAME} ${HOME}/.credentials
//            sbt -DrepoType=${MAVEN_PROFILE_ACTIVATION_NAME} -DstageRepoId=${STAGE_REPO_ID} -DcurrentVersion=${PROJECT_VERSION} -Dnd4jVersion=${PROJECT_VERSION} +publishSigned
//            find ${HOME}/.ivy2 -type f -name  ".credentials"  -delete -o -name ".nexus"  -delete -o -name ".jfrog" -delete -o -name ".sonatype" -delete -o -name ".bintray" -delete;
//        '''.stripIndent()
    }

    private void getNd4jArtifacts() {
        /* FIXME: Implement method for chaning repo url depending on repo id */
        String repositoryUrl = 'http://oss.sonatype.org/content/repositories/snapshots'
        String artifactGroupId = 'org.nd4j'
        String artifactId = 'nd4j-native'
        String artifactVersion = projectVersion
        String artifactPackaging = 'jar'
        List artifactClassifiers = [
                'android-arm',
                'android-arm64',
                'android-x86',
                'android-x86_64',
                'linux-ppc64le',
                'linux-x86_64',
                'linux-x86_64-avx2',
                'linux-x86_64-avx512',
                'ios-arm64',
                'ios-x86_64',
                'macosx-x86_64',
                'macosx-x86_64-avx2',
                'windows-x86_64',
                'windows-x86_64-avx2'
        ]
        Closure mvnCommand = { artifactClassifier ->
            return [
                    'mvn -U -B dependency:get',
                    "-DremoteRepositories=$repositoryUrl",
                    "-DgroupId=$artifactGroupId",
                    "-DartifactId=$artifactId",
                    "-Dversion=$artifactVersion",
                    "-Dpackaging=$artifactPackaging",
                    "-Dclassifier=${artifactClassifier}"
            ].findAll().join(' ')
        }

        for (artifactClassifier in artifactClassifiers) {
            script.sh mvnCommand(artifactClassifier)
        }
    }
}