package skymind.pipelines.projects

import groovy.transform.InheritConstructors

@InheritConstructors
class DataVecProject extends Project {
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
        List dependencyMappings = [
                [sparkVersion: "1", scalaVersion: "2.10"],
                [sparkVersion: "1", scalaVersion: "2.11"],
                [sparkVersion: "2", scalaVersion: "2.11"]
        ]

        for (Map mapping : dependencyMappings) {
            String scalaVersion = mapping.scalaVersion
            String sparkVersion = mapping.sparkVersion

            script.echo "[INFO] Setting Scala version to: $scalaVersion"
            script.sh "./change-scala-versions.sh $scalaVersion"

            script.echo "[INFO] Setting Spark version to: $sparkVersion"
            script.sh "./change-spark-versions.sh $sparkVersion"

            script.mvn getMvnCommand('build')
        }
    }
}