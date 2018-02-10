package skymind.pipelines.helper

class NotificationHelper implements Serializable {
    private script

    NotificationHelper(script) {
        this.script = script
    }

    private String getColorName(String buildStatus) {
        switch (buildStatus) {
            case 'STARTED':
                return 'YELLOW'
                break
            case 'SUCCESSFUL':
                return 'GREEN'
                break
            default:
                return 'RED'
                break
        }
    }

    private String getColorCode(String buildStatus) {
        switch (buildStatus) {
            case 'STARTED':
                return '#FFFF00'
                break
            case 'SUCCESSFUL':
                return '#00FF00'
                break
            default:
                return '#FF0000'
                break
        }
    }

    protected void sendEmail(String buildStatus = 'STARTED') {
        String jobName = script.env.JOB_NAME
        String buildNumber = script.env.BUILD_NUMBER
        String buildUrl = script.env.BUILD_URL
        /* Build status of null means successful */
        buildStatus = buildStatus ?: 'SUCCESSFUL'

        String subject = "${buildStatus}: Job '${jobName} [${buildNumber}]'"
        String details = """\
            <p>STARTED: Job '${jobName} [${buildNumber}]':</p>
            <p>Check console output at "<a href="${buildUrl}">${jobName} [${buildNumber}]</a>"</p>
        """.stripIndent()

        def recipients = script.emailextrecipients([
                [$class: 'CulpritsRecipientProvider'],
                [$class: 'DevelopersRecipientProvider'],
                [$class: 'RequesterRecipientProvider'],
                [$class: 'UpstreamComitterRecipientProvider'],
                [$class: 'FirstFailingBuildSuspectsRecipientProvider']
        ])

        /* Send e-mail notification */
//        script.emailext(
//                to: recipients,
//                subject: subject,
//                body: details,
//                mimeType: 'text/html'
//        )
        script.step([
                $class                  : 'Mailer',
                notifyEveryUnstableBuild: true,
                recipients              : 'serhii@skymind.io'
        ])
    }
}
