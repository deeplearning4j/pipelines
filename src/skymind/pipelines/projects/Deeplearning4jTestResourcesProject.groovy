package skymind.pipelines.projects

import groovy.transform.InheritConstructors

@InheritConstructors
class Deeplearning4jTestResourcesProject extends Project {
    void initPipeline() {
        allocateBuildNode {
            script.dir(projectName) {
                if (branchName.contains(releaseBranchPattern)) {
                    script.stage("Perform Release") {
                        getReleaseParameters()
                    }

                    script.stage("Prepare for Release") {
                        setupEnvForRelease()
                    }
                }

                script.stage("Build") {
                    runBuild()
                }
            }
        }
    }

    protected void runBuild() {
        script.mvn getMvnCommand('build-test-resources')
    }
}
