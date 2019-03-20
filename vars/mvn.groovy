#!/usr/bin/env groovy

def call(String command, Boolean inK8s = false) {
    Boolean isUnixNode = isUnix()
    String shell = isUnixNode ? 'sh' : 'bat'
    String configFileName = (env.BRANCH_NAME =~ /^master$|^latest_release$/) ?
            'global_mvn_settings_xml' : 'deeplearning4j-maven-global-settings'
//    String fixedPath = (inK8s) ?
//            (isUnixNode) ? sh(script: 'echo ${PATH}', returnStdout: true).trim() :
//                    bat(script: 'echo ${PATH}', returnStdout: true).trim() :
//            ''

//    withMaven(
//            /* Maven installation declared in the Jenkins "Global Tool Configuration" */
////            maven: 'maven-3.6.0',
//            /* -XX:+TieredCompilation -XX:TieredStopAtLevel=1 options should make JVM start a bit faster */
//            mavenOpts: "-Djava.awt.headless=true -XX:+TieredCompilation -XX:TieredStopAtLevel=1",
//            globalMavenSettingsConfig: configFileName,
//            options: [
//                    artifactsPublisher(disabled: true),
//                    junitPublisher(disabled: true), // This option does not allow to distinguish tests results in parallel step, whereas simple junit step call does.
//                    findbugsPublisher(disabled: true),
//                    openTasksPublisher(disabled: true),
//                    dependenciesFingerprintPublisher(disabled: true),
//                    concordionPublisher(disabled: true),
//                    invokerPublisher(disabled: true),
//                    jgivenPublisher(disabled: true)
//            ]
//    ) {
        /* Workaround to fix wrong value of PATH env variable that picked by pipeline
            (value from specific container is ignored)
         */
//        if (fixedPath) {
//            env.PATH = "${fixedPath}"
//        }
//
//        /* Run the maven build */
//        if (isUnixNode) {
//            "$shell" command
//        }
//        /*
//            Workaround for windows, because there is no way to redefine location of settings.xml
//            and we are using bash to invoke maven, and bash doesn't work with windows like paths
//        */
//        else {
//            configFileProvider([configFile(fileId: configFileName, variable: 'MAVEN_SETTINGS')]) {
//                String mavenSettingsFilePath = env.MAVEN_SETTINGS.replaceAll('\\\\', '/')
//
//                withEnv(["MAVEN_SETTINGS=$mavenSettingsFilePath"]) {
//                    "$shell" command
//                }
//            }
//        }
//    }


    /* Run the maven build */
    if (isUnixNode) {
        configFileProvider([
                configFile(
                    fileId: configFileName,
//                    targetLocation: "${env.HOME}/.m2/settings.xml"
//                    targetLocation: ".m2/settings.xml"
                    variable: 'MAVEN_SETTINGS'
                )
        ]) {
            sh command
        }
    }
    /*
        Workaround for windows, because there is no way to redefine location of settings.xml
        and we are using bash to invoke maven, and bash doesn't work with windows like paths
    */
    else {
        configFileProvider([configFile(fileId: configFileName, variable: 'MAVEN_SETTINGS')]) {
            String mavenSettingsFilePath = env.MAVEN_SETTINGS.replaceAll('\\\\', '/')

            withEnv(["MAVEN_SETTINGS=$mavenSettingsFilePath"]) {
                sh command
            }
        }
    }
}
