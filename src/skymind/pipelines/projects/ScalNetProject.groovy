package skymind.pipelines.projects

import groovy.transform.InheritConstructors

@InheritConstructors
class ScalNetProject extends Project {
    private final List scalaVersions = ['2.10', '2.11']

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

    protected void runBuild() {
        for (String scalaVersion : scalaVersions) {
            script.echo "[INFO] Setting Scala version to: $scalaVersion"
            script.sh "./change-scala-versions.sh $scalaVersion"
            script.mvn getMvnCommand('build')
        }
    }
}