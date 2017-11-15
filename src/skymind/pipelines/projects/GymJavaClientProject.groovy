package skymind.pipelines.projects

import groovy.transform.InheritConstructors

@InheritConstructors
class GymJavaClientProject extends Project {
    void initPipeline() {
        allocateBuildNode { dockerImageName, dockerImageParams ->
            script.stage('Build') { runBuild(dockerImageName, dockerImageParams) }
//            script.stage('Test') { runTests(dockerImageName, dockerImageParams) }
        }
    }

    private void runBuild(String dockerImageName, String dockerImageParams) {
        script.dir(projectName) {
            script.docker.image(dockerImageName).inside(dockerImageParams) {
                projectVersion = projectObjectModel?.version

                script.isVersionReleased(projectName, projectVersion)
                script.setProjectVersion(projectVersion, true)
                script.mvn getMvnCommand('build')
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