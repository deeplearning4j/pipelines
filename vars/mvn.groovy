#!/usr/bin/env groovy

def call(String command) {
    Boolean unixNode = isUnix()
    String shell = unixNode ? 'sh' : 'bat'

    withMaven(
            /* Maven installation declared in the Jenkins "Global Tool Configuration" */
            maven: 'maven-3.3.9',
            options: [
                    artifactsPublisher(disabled: true),
                    junitPublisher(disabled: false),
                    findbugsPublisher(disabled: true),
                    openTasksPublisher(disabled: true),
                    dependenciesFingerprintPublisher(disabled: true),
                    concordionPublisher(disabled: true),
                    invokerPublisher(disabled: true),
                    jgivenPublisher(disabled: true)
            ]
    ) {
        /* Run the maven build */
        if (unixNode) {
            "$shell" command
        }
        /*
            Workaround for windows, because there is no way to redefine location of settings.xml
            and we are using bash to invoke maven, and bash doesn't work with windows like paths
        */
        else {
            configFileProvider([configFile(fileId: 'global_mvn_settings_xml', variable: 'MAVEN_SETTINGS')]) {
                String mavenSettingsFilePath = env.MAVEN_SETTINGS.replaceAll('\\\\', '/')

                withEnv(["MAVEN_SETTINGS=$mavenSettingsFilePath"]) {
                    "$shell" command
                }
            }
        }
    }
}
