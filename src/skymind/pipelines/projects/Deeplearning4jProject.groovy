package skymind.pipelines.projects

import groovy.transform.InheritConstructors

@InheritConstructors
class Deeplearning4jProject extends Project {
    static {
        /* Override default platforms */
        defaultPlatforms = [
                [backends: [], compillers: [], name: 'linux-x86_64']
        ]
    }

    void initPipeline() {
        allocateBuildNode { dockerImageName, dockerImageParams ->
            script.dir(projectName) {
                script.docker.image(dockerImageName).inside(dockerImageParams) {
                    script.stage('Build') {
                        runBuild()
                    }

                    if (branchName == 'master') {
                        script.stage('Build Test Resources') {
                            runBuildTestResources()
                        }

                        script.stage('Test') {
                            runTests()
                        }

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
                [sparkVersion: "1", scalaVersion: "2.10", cudaVersion: "8.0"],
                [sparkVersion: "2", scalaVersion: "2.11", cudaVersion: "9.0"],
                [sparkVersion: "2", scalaVersion: "2.11", cudaVersion: "9.1"]
        ]

        for (Map mapping : dependencyMappings) {
            String cudaVersion = mapping.cudaVersion
            String scalaVersion = mapping.scalaVersion
            String sparkVersion = mapping.sparkVersion

            script.echo "[INFO] Setting CUDA version to: $cudaVersion"
            script.sh "./change-cuda-versions.sh $cudaVersion"

            script.echo "[INFO] Setting Scala version to: $scalaVersion"
            script.sh "./change-scala-versions.sh $scalaVersion"

            script.echo "[INFO] Setting Spark version to: $sparkVersion"
            script.sh "./change-spark-versions.sh $sparkVersion"

            script.mvn getMvnCommand('build')
        }
    }

    protected void runTests() {
        String testProfile
        List dependencyMappings = [
                [sparkVersion: "1", scalaVersion: "2.10", cudaVersion: ""],
                [sparkVersion: "1", scalaVersion: "2.10", cudaVersion: "8.0"],
                [sparkVersion: "2", scalaVersion: "2.11", cudaVersion: "9.0"],
                [sparkVersion: "2", scalaVersion: "2.11", cudaVersion: "9.1"]
        ]

        for (Map mapping : dependencyMappings) {
            String cudaVersion = mapping.cudaVersion
            String scalaVersion = mapping.scalaVersion
            String sparkVersion = mapping.sparkVersion

            if (cudaVersion) {
                script.echo "[INFO] Setting CUDA version to: $cudaVersion"
                script.sh "./change-cuda-versions.sh $cudaVersion"
                testProfile = "-P test-nd4j-cuda-$cudaVersion"
            } else {
                testProfile = '-P test-nd4j-native'
            }

            script.echo "[INFO] Setting Scala version to: $scalaVersion"
            script.sh "./change-scala-versions.sh $scalaVersion"

            script.echo "[INFO] Setting Spark version to: $sparkVersion"
            script.sh "./change-spark-versions.sh $sparkVersion"

            script.mvn getMvnCommand('test', [
                    testProfile,
                    '-Dmaven.test.failure.ignore=true'
            ]) + ' || true'
        }
    }
}