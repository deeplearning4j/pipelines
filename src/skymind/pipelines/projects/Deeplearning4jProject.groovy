package skymind.pipelines.projects

import groovy.transform.InheritConstructors

@InheritConstructors
class Deeplearning4jProject extends Project {
    private final List dependencyMappings = [
            [sparkVersion: "1", scalaVersion: "2.10", cudaVersion: "8.0"],
            [sparkVersion: "2", scalaVersion: "2.11", cudaVersion: "9.0"],
            [sparkVersion: "2", scalaVersion: "2.11", cudaVersion: "9.1"]
    ]

    static {
        /* Override default platforms */
        defaultPlatforms = [[name: 'linux-x86_64']]
    }

    void initPipeline() {
        for (Map pltm : platforms) {
            Map platform = pltm
            String platformName = platform.name
            script.node(platformName) {
                pipelineWrapper {
                    try {
                        script.stage('Checkout') {
                            script.deleteDir()

                            script.dir(projectName) {
                                script.checkout script.scm
                            }
                        }

                        Map dockerConf = script.pipelineEnv.getDockerConfig(platformName)
                        String dockerImageName = dockerConf['image'] ?:
                                script.error('Docker image name is missing.')
                        String dockerImageParams = dockerConf?.'params'

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
                                    String cudaVersion = mapping.cudaVersion
                                    String scalaVersion = mapping.scalaVersion
                                    String sparkVersion = mapping.sparkVersion

                                    script.stage("Build | CUDA ${cudaVersion} | Scala ${scalaVersion} | Spark ${sparkVersion}") {
                                        runBuild(cudaVersion, scalaVersion, sparkVersion)
                                    }

                                    if (branchName == 'master' || !branchName.contains(releaseBranchPattern)) {
//                            script.stage('Build Test Resources') {
//                                runBuildTestResources()
//                            }
//
//                            script.stage("Test | CUDA ${cudaVersion} | Scala ${scalaVersion} | Spark ${sparkVersion}") {
//                                runTests(cudaVersion)
//                            }
                                    }

                                    if (branchName == 'master' || branchName.contains(releaseBranchPattern)) {
                                        script.stage("Deploy | CUDA ${cudaVersion} | Scala ${scalaVersion} | Spark ${sparkVersion}") {
                                            runDeploy()
                                        }
                                    }
                                }
                            }
                        }
                    }
                    finally {
                        script.cleanWs deleteDirs: true
                    }
                }
            }
        }
    }

    protected void runBuild(String cudaVersion, String scalaVersion, String sparkVersion) {
        script.echo "[INFO] Setting CUDA version to: $cudaVersion"
        script.sh "./change-cuda-versions.sh $cudaVersion"

        script.echo "[INFO] Setting Scala version to: $scalaVersion"
        script.sh "./change-scala-versions.sh $scalaVersion"

        script.echo "[INFO] Setting Spark version to: $sparkVersion"
        script.sh "./change-spark-versions.sh $sparkVersion"

        script.mvn getMvnCommand('build')
    }

    protected void runTests(String cudaVersion) {
        String testProfile = "-P test-nd4j-cuda-$cudaVersion"

        if (cudaVersion == '8.0') {
            testProfile = '-P test-nd4j-native'

            script.mvn getMvnCommand('test', [
                    testProfile,
                    '-Dmaven.test.failure.ignore=true'
            ]) + ' || true'
        }

        script.mvn getMvnCommand('test', [
                testProfile
        ])
    }

    protected void updateVersions(String version) {
        if (script.isUnix()) {
            script.sh """
                sed -i "s/<nd4j.version>.*<\\/nd4j.version>/<nd4j.version>$version<\\/nd4j.version>/" pom.xml
                sed -i "s/<datavec.version>.*<\\/datavec.version>/<datavec.version>$version<\\/datavec.version>/" pom.xml
                sed -i "s/<deeplearning4j.version>.*<\\/deeplearning4j.version>/<deeplearning4j.version>$version<\\/deeplearning4j.version>/" pom.xml
                sed -i "s/<dl4j-test-resources.version>.*<\\/dl4j-test-resources.version>/<dl4j-test-resources.version>$version<\\/dl4j-test-resources.version>/" pom.xml

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