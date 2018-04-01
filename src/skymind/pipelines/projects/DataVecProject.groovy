package skymind.pipelines.projects

import groovy.transform.InheritConstructors

@InheritConstructors
class DataVecProject extends Project {
    private final List dependencyMappings = [
            [sparkVersion: "1", scalaVersion: "2.10"],
            [sparkVersion: "1", scalaVersion: "2.11"],
            [sparkVersion: "2", scalaVersion: "2.11"]
    ]

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

                    for (Map mapping : dependencyMappings) {
                        String scalaVersion = mapping.scalaVersion
                        String sparkVersion = mapping.sparkVersion

                        script.stage("Build | Scala ${scalaVersion} | Spark ${sparkVersion}") {
                            runBuild(scalaVersion, sparkVersion)
                        }

                        if (!branchName.contains(releaseBranchPattern)) {
                            script.stage("Test | Scala ${scalaVersion} | Spark ${sparkVersion}") {
                                runTests()
                            }
                        }

                        if (branchName == 'master' || branchName.contains(releaseBranchPattern)) {
                            script.stage("Deploy | Scala ${scalaVersion} | Spark ${sparkVersion}") {
                                runDeploy()
                            }
                        }
                    }
                }
            }
        }
    }

    protected void runBuild(String scalaVersion, String sparkVersion) {
        script.echo "[INFO] Setting Scala version to: $scalaVersion"
        script.sh "./change-scala-versions.sh $scalaVersion"

        script.echo "[INFO] Setting Spark version to: $sparkVersion"
        script.sh "./change-spark-versions.sh $sparkVersion"

        script.mvn getMvnCommand('build')
    }

    protected void updateVersions(String version) {
        if (script.isUnix()) {
            script.sh """
                sed -i "s/<nd4j.version>.*<\\/nd4j.version>/<nd4j.version>$version<\\/nd4j.version>/" pom.xml
                # Spark versions, like <version>xxx_spark_2-SNAPSHOT</version>
                for f in \$(find . -name 'pom.xml' -not -path '*target*'); do
                    sed -i "s/version>.*_spark_.*</version>${version}_spark_1</g" \$f
                done
            """.stripIndent()
        }
        else {
            /* TODO: Add windows support */
            script.error "[ERROR] Windows is not supported yet."
        }
    }
}