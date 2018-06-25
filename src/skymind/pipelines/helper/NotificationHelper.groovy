package skymind.pipelines.helper

class NotificationHelper implements Serializable {
    private script
    private buildResult

    NotificationHelper(script) {
        this.script = script
        buildResult = this.script.currentBuild.result
    }

    protected void sendEmail() {
        String jobName = script.currentBuild.rawBuild.fullDisplayName
        String changeId = script.env.CHANGE_ID
        String changeTitle = script.env.CHANGE_TITLE
        String changeAuthor = script.env.CHANGE_AUTHOR
        String changeAuthorEmail = script.env.CHANGE_AUTHOR_EMAIL
        String buildUrl = script.env.RUN_DISPLAY_URL

        String subject = "${buildResult.toLowerCase().capitalize()} | ${jobName}"
        String details = ((changeId) ? "<p>Changes: ${changeId}, ${changeTitle}</p>\n": '') +
                ((changeId) ? "<p>Author: ${changeAuthor} (${changeAuthorEmail})</p>\n" : '') +
                "<p>Check run details at <a href=\"${buildUrl}\">${jobName}</a></p>"

//        def recipients = script.emailextrecipients([
//                [$class: 'CulpritsRecipientProvider'],
//                [$class: 'DevelopersRecipientProvider'],
//                [$class: 'RequesterRecipientProvider'],
//                [$class: 'UpstreamComitterRecipientProvider'],
//                [$class: 'FirstFailingBuildSuspectsRecipientProvider']
//        ])

        def recipients = script.emailextrecipients([
                [$class: 'DevelopersRecipientProvider']
        ])

        /* Send e-mail notification */
        script.emailext(
//                to: recipients,
                to: 'serhii@gmail.com',
                subject: subject,
                body: details,
                mimeType: 'text/html'
        )
    }
}
