package skymind.pipelines.projects

import groovy.transform.InheritConstructors

@InheritConstructors
class Nd4sProject extends Project {
    private final String stageRepositoryId = 'nexus'

    private final Map sbtCredentialsIds = [
            nexus: 'sbt-local-nexus-id-1', jfrog: 'sbt-local-jfrog-id-1', sonatype: 'sbt-oss-sonatype-id-1',
            bintray: 'sbt-oss-bintray-id-1'
    ]

    void initPipeline() {
        allocateBuildNode { dockerImageName, dockerImageParams ->
            script.stage('Build') { runBuild(dockerImageName, dockerImageParams) }
        }
    }

    private void runBuild(String dockerImageName, String dockerImageParams) {
        script.dir(projectName) {
            String ivyHome = "${script.env.WORKSPACE}/.ivy2"
            /*
                Mount point of ivy2 folder for Docker container.
                Because by default Jenkins mounts current working folder in Docker container,
                we need to add custom mount.
             */
            script.env.IVY_DOCKER_FOLDER = '/tmp/.ivy2'
            String ivy2Mount = " -v ${ivyHome}:${script.env.IVY_DOCKER_FOLDER}:rw,z"

            /* Create IVY_HOME folder if it doesn't exists */
            script.sh "test -d ${ivyHome} || mkdir ${ivyHome}"

            /* Populate SBT credentials */
            for (def sbtCredentials : sbtCredentialsIds) {
                script.configFileProvider([
                        script.configFile(
                                fileId: "${sbtCredentials.value}",
                                variable: 'SBT_CREDENTIALS'
                        )
                ]) {
                    script.sh "cp ${script.env.SBT_CREDENTIALS} ${ivyHome}/.${sbtCredentials.key}"
                }
            }


            script.docker.image(dockerImageName).inside(dockerImageParams) {
                script.wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
                    script.env.PROJECT_VERSION = projectVersion
                    script.env.STAGE_REPOSITORY_ID = stageRepositoryId

                    script.sh '''\
                        cp -a ${IVY_DOCKER_FOLDER} ${HOME}/
                        cp ${HOME}/.${MAVEN_PROFILE_ACTIVATION_NAME} ${HOME}/.credentials
                        sbt -DrepoType=${MAVEN_PROFILE_ACTIVATION_NAME} -DstageRepoId=${STAGE_REPO_ID} -DcurrentVersion=${PROJECT_VERSION} -Dnd4jVersion=${PROJECT_VERSION} +publishSigned
                        find ${HOME}/.ivy2 -type f -name  ".credentials"  -delete -o -name ".nexus"  -delete -o -name ".jfrog" -delete -o -name ".sonatype" -delete -o -name ".bintray" -delete;
                    '''.stripIndent()
                }
            }
        }
    }

    private void getNd4jArtifacts() {
    }
}