package skymind.pipelines.projects

import groovy.transform.InheritConstructors

@InheritConstructors
class LagomSkilApiProject extends Project {
    void initPipeline() {
        allocateBuildNode {
            script.dir(projectName) {
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

    protected void runBuild() {
        script.mvn getMvnCommand('build', [
                (script.env.CREATE_RPM) ? '-P generate-rpm' : ''
        ])
    }
}