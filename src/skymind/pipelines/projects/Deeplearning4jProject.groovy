package skymind.pipelines.projects

import groovy.transform.InheritConstructors

@InheritConstructors
class Deeplearning4jProject extends Project {
    void initPipeline() {
        allocateBuildNode { dockerImageName, dockerImageParams ->
            script.stage('Build Tests') { runBuildTests(dockerImageName, dockerImageParams) }
            script.stage('Build') { runBuild(dockerImageName, dockerImageParams) }
//            script.stage('Test') { runTests(dockerImageName, dockerImageParams) }
        }
    }

    private void runBuildTests(String dockerImageName, String dockerImageParams) {
        String dl4jTestResourcesGitFolderName = 'dl4j-test-resources'
        String dl4jTestResourcesGitUrl = 'https://github.com/deeplearning4j/dl4j-test-resources.git'

        script.checkout([
                $class                           : 'GitSCM',
                branches                         : [[name: '*/master']],
                doGenerateSubmoduleConfigurations: false,
                extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                     relativeTargetDir: "$dl4jTestResourcesGitFolderName"],
                                                    [$class      : 'CloneOption',
                                                     honorRefspec: true,
                                                     noTags      : true,
                                                     reference   : '',
                                                     shallow     : true]],
                submoduleCfg                     : [],
                userRemoteConfigs                : [[url: "$dl4jTestResourcesGitUrl"]]
        ])

        script.dir(dl4jTestResourcesGitFolderName) {
            script.docker.image(dockerImageName).inside(dockerImageParams) {
                script.mvn 'export PATH=$MVN_CMD_DIR:$PATH && mvn -U clean install'
            }
        }
    }

    private void runBuild(String dockerImageName, String dockerImageParams) {
        script.dir(projectName) {
            script.docker.image(dockerImageName).inside(dockerImageParams) {
                projectVersion = projectObjectModel?.version

                script.isVersionReleased(projectName, projectVersion)
                script.setProjectVersion(projectVersion, true)

                List dependencyMappings = [
                        [sparkVersion: "1", scalaVersion: "2.10", cudaVersion: "8.0"],
                        [sparkVersion: "1", scalaVersion: "2.11", cudaVersion: "8.0"],
                        [sparkVersion: "2", scalaVersion: "2.11", cudaVersion: "8.0"],
                        [sparkVersion: "1", scalaVersion: "2.10", cudaVersion: "9.0"],
                        [sparkVersion: "1", scalaVersion: "2.11", cudaVersion: "9.0"],
                        [sparkVersion: "2", scalaVersion: "2.11", cudaVersion: "9.0"]
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

                    script.mvn getMvnCommand('build', ["-Dnd4j.version=${projectVersion}",
                                                       "-Ddeeplearning4j.version=${projectVersion}",
                                                       "-Ddatavec.version=${projectVersion}",
                                                       "-Ddl4j-test-resources.version=${projectVersion}"])
                }
            }
        }
    }

    private void runTests(String dockerImageName, String dockerImageParams) {
    }
}