properties([
        [$class: "BuildDiscarderProperty", strategy: [$class: "LogRotator", artifactDaysToKeepStr: "", artifactNumToKeepStr: "", daysToKeepStr: "", numToKeepStr: "10"]],
        [$class: "ParametersDefinitionProperty", parameterDefinitions:
                [
                        [$class: "StringParameterDefinition", name: "VERSION", defaultValue: "0.7.3-SNAPSHOT", description: "Deeplearning component release version"],
                        [$class: "ChoiceParameterDefinition", name: "PLATFORM_NAME", choices: "linux-x86_64\nlinux-ppc64le\nandroid-arm\nandroid-x86\nlinux-x86", description: "Build project on architecture"],
                        [$class: "BooleanParameterDefinition", name: "TESTS", defaultValue: false, description: "Select to run tests during mvn execution"],
                        [$class: "BooleanParameterDefinition", name: "SONAR", defaultValue: false, description: "Select to check code with SonarQube"],
                        [$class: "StringParameterDefinition", name: "GIT_BRANCHNAME", defaultValue: "intropro072-01", description: "Default Git branch value"],
                        [$class: "CredentialsParameterDefinition", name: "GITCREDID", required: false, defaultValue: "github-private-deeplearning4j-id-1", description: "Credentials to be used for cloning, pushing and tagging deeplearning4j repositories"],
                        [$class: "StringParameterDefinition", name: "PDIR", defaultValue: "jobs/dl4j", description: "Path to groovy scripts"],
                ]
        ]
])

node(PLATFORM_NAME) {

    step([$class: 'WsCleanup'])
    checkout scm

    echo "Load variables"
    load "${PDIR}/vars.groovy"

    echo "load functions"
    functions = load "${PDIR}/functions.groovy"

    // Remove .git folder from workspace
    functions.rm()

    // Create .m2 direcory
    // functions.dirm2()

    // Set docker image and parameters for current platform
    functions.def_docker()

    functions.get_project_code("${LIBPROJECT}")
    functions.get_project_code("${PROJECT}")

    docker.image(dockerImage).inside(dockerParams) {
        sh '''
        cd libnd4j && ./buildnativeoperations.sh -platform android-arm
        cd ../nd4j && mvn clean install -Djavacpp.platform=android-arm -DskipTests -pl '!:nd4j-cuda-8.0,!:nd4j-cuda-8.0-platform'
        '''
        // stash includes: 'nd4j/', name: 'nd4j-arm'
     }
}

// node("linux-x86_64") {
//     stage("unstash android artifacts") {
//         dir("/var/lib/jenkins/local-storage") {
//             unstash 'nd4j-arm';
//         }
//     }
// }
