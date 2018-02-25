package skymind.pipelines.projects

import groovy.transform.InheritConstructors

@InheritConstructors
class ArbiterProject extends Project {
    private final List scalaVersions = ['2.10', '2.11']

    void initPipeline() {
        allocateBuildNode { dockerImageName, dockerImageParams ->
            script.stage('Build') { runBuild(dockerImageName, dockerImageParams) }
            script.timeout(15) {
                script.stage('Test') { runTests(dockerImageName, dockerImageParams) }
            }
        }
    }

    private void runBuild(String dockerImageName, String dockerImageParams) {
        script.dir(projectName) {
            script.docker.image(dockerImageName).inside(dockerImageParams) {
                for (String scalaVersion : scalaVersions) {
                    script.echo "[INFO] Setting Scala version to: $scalaVersion"
                    script.sh "./change-scala-versions.sh $scalaVersion"
                    script.mvn getMvnCommand('build')
                }
            }
        }
    }

    private void runTests(String dockerImageName, String dockerImageParams) {
        script.dir(projectName) {
            script.docker.image(dockerImageName).inside(dockerImageParams) {
                script.mvn getMvnCommand('test')
            }
        }
    }
}