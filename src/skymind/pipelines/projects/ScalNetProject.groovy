package skymind.pipelines.projects

import groovy.transform.InheritConstructors

@InheritConstructors
class ScalNetProject extends Project {
    private final List scalaVersions = ['2.10', '2.11']

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

                for (String scalaVersion : scalaVersions) {
                    script.stage("Build | Scala ${scalaVersion}") {
                        runBuild(scalaVersion)
                    }

                    if (!branchName.contains(releaseBranchPattern)) {
                        script.stage("Test | Scala ${scalaVersion}") {
                            runTests()
                        }
                    }

                    if (branchName == 'master' || branchName.contains(releaseBranchPattern)) {
                        script.stage("Deploy | Scala ${scalaVersion}") {
                            runDeploy()
                        }
                    }
                }
            }
        }
    }

    protected void runBuild(String scalaVersion) {
        script.echo "[INFO] Setting Scala version to: $scalaVersion"
        script.sh "./change-scala-versions.sh $scalaVersion"

        script.mvn getMvnCommand('build')
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