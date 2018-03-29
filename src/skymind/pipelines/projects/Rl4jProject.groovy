package skymind.pipelines.projects

import groovy.transform.InheritConstructors

@InheritConstructors
class Rl4jProject extends Project {
    void initPipeline() {
        allocateBuildNode { dockerImageName, dockerImageParams ->
            script.dir(projectName) {
                script.docker.image(dockerImageName).inside(dockerImageParams) {
                    script.stage('Build') {
                        runBuild()
                    }

                    script.stage('Test') {
                        runTests()
                    }

                    if (branchName == 'master') {
                        script.stage('Deploy') {
                            runDeploy()
                        }
                    }
                }
            }
        }
    }
}