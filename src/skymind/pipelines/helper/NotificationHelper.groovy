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
        String jobName = script.currentBuild.rawBuild.fullDisplayName
        String changeId = script.env.CHANGE_ID
        String changeTitle = script.env.CHANGE_TITLE
        String changeAuthor = script.env.CHANGE_AUTHOR
        String changeAuthorEmail = script.env.CHANGE_AUTHOR_EMAIL
        String buildUrl = script.env.RUN_DISPLAY_URL
        /* Build status of null means successful */
        buildStatus = buildStatus ?: 'SUCCESS'

        String subject = "${buildStatus.toLowerCase().capitalize()} | ${jobName}"
        String details = ((changeId) ? "<p>Changes: ${changeId}, ${changeTitle}</p>\n": '') +
                ((changeId) ? "<p>Author: ${changeAuthor} (${changeAuthorEmail})</p>\n" : '') +
                "<p>Check run details at <a href=\"${buildUrl}\">${jobName}</a></p>"

        def recipients = script.emailextrecipients([
                [$class: 'CulpritsRecipientProvider'],
                [$class: 'DevelopersRecipientProvider'],
                [$class: 'RequesterRecipientProvider'],
                [$class: 'UpstreamComitterRecipientProvider'],
                [$class: 'FirstFailingBuildSuspectsRecipientProvider']
        ])

        /* Send e-mail notification */
        script.emailext(
                to: recipients,
                subject: subject,
                body: details,
                mimeType: 'text/html'
        )
//        script.step([
//                $class                  : 'Mailer',
//                notifyEveryUnstableBuild: true,
//                recipients              : 'serhii@skymind.io'
//        ])
    }
}
