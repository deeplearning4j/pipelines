package skymind.pipelines.projects

import groovy.transform.InheritConstructors

@InheritConstructors
class Rl4jProject extends Project {
    void initPipeline() {
        allocateBuildNode { dockerImageName, dockerImageParams ->
            script.dir(projectName) {
                script.docker.image(dockerImageName).inside(dockerImageParams) {
                    if (branchName.contains(releaseBranchPattern)) {
                        script.stage("Perform Release") {
                            getReleaseParameters()
                        }

                        script.stage("Prepare for Release") {
                            setupEnvForRelease()
                        }
                    }

                    script.stage('Build') {
                        runBuild()
                    }

                    if (!branchName.contains(releaseBranchPattern)) {
                        script.stage('Test') {
                            runTests()
                        }
                    }

                    if (branchName == 'master' || branchName.contains(releaseBranchPattern)) {
                        script.stage('Deploy') {
                            runDeploy()
                        }
                    }
                }
            }
        }
    }

    protected void updateVersions(String version) {
        if (script.isUnix()) {
            script.sh """
                sed -i "s/<nd4j.version>.*<\\/nd4j.version>/<nd4j.version>$version<\\/nd4j.version>/" pom.xml
                sed -i "s/<datavec.version>.*<\\/datavec.version>/<datavec.version>$version<\\/datavec.version>/" pom.xml
                sed -i "s/<dl4j.version>.*<\\/dl4j.version>/<dl4j.version>$version<\\/dl4j.version>/" pom.xml
            """.stripIndent()
        }
        else {
            /* TODO: Add windows support */
            script.error "[ERROR] Windows is not supported yet."
        }
    }
}