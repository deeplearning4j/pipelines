package skymind.pipelines.projects

import groovy.transform.InheritConstructors

@InheritConstructors
class ScalNetProject extends Project {
    private final List scalaVersions = ['2.10', '2.11']

    void initPipeline() {
        allocateBuildNode { dockerImageName, dockerImageParams ->
            script.stage('Build') { runBuild(dockerImageName, dockerImageParams) }
//            script.stage('Test') { runTests(dockerImageName, dockerImageParams) }
        }
    }

    private void runBuild(String dockerImageName, String dockerImageParams) {
        script.dir(projectName) {
            script.docker.image(dockerImageName).inside(dockerImageParams) {
                script.sh "sed -i '0,/<artifactId>.*<\\/artifactId>/s//<artifactId>scalnet<\\/artifactId>/' " +
                        "pom.xml"
                script.sh "sed -i '0,/<artifactId>.*<\\/artifactId>/s//<artifactId>scalnet_" +
                        '${scala.binary.version}' + "<\\/artifactId>/' pom.xml"

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