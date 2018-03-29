package skymind.pipelines.projects

import groovy.transform.InheritConstructors

@InheritConstructors
class ArbiterProject extends Project {
    private final List scalaVersions = ['2.10', '2.11']

    void initPipeline() {
        allocateBuildNode { dockerImageName, dockerImageParams ->
            script.dir(projectName) {
                script.docker.image(dockerImageName).inside(dockerImageParams) {
                    script.stage('Build') {
                        runBuild()
                    }

                    script.stage('Test') {
                        /* FIXME: Timeout requested by Alex Black because of flappy tests behavior */
                        script.timeout(15) {
                            runTests()
                        }
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