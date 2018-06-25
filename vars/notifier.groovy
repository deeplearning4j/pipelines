#!/usr/bin/env groovy

void sendSlackNotification() {
    String buildResult = currentBuild.result ?: 'UNKNOWN'
    String jobName = currentBuild.rawBuild.fullDisplayName
    String buildUrl = env.RUN_DISPLAY_URL
    String subject = "${buildResult.toLowerCase().capitalize()} | <${buildUrl}|${jobName}>"
    String notificationColor

    switch (buildResult) {
        case 'STARTED':
            notificationColor = 'info'
            break
        case 'SUCCESS':
            notificationColor = "good"
            break
        case 'ABORTED':
            notificationColor = "#D4DADF"
            break
        case 'UNSTABLE':
            notificationColor = "warning"
            break
        case 'FAILURE':
        default:
            notificationColor = "danger"
            break
    }

    /* Send slack notification */
    slackSend color: notificationColor, message: "${subject}"
}

void sendEmailNotification() {
    String buildResult = currentBuild.result ?: 'UNKNOWN'
    String jobName = currentBuild.rawBuild.fullDisplayName
    String changeId = env.CHANGE_ID
    String changeTitle = env.CHANGE_TITLE
    String changeAuthor = env.CHANGE_AUTHOR
    String changeAuthorEmail = env.CHANGE_AUTHOR_EMAIL
    String buildUrl = env.RUN_DISPLAY_URL

    String subject = "${buildResult.toLowerCase().capitalize()} | ${jobName}"
    String details = ((changeId) ? "<p>Changes: ${changeId}, ${changeTitle}</p>\n": '') +
            ((changeId) ? "<p>Author: ${changeAuthor} (${changeAuthorEmail})</p>\n" : '') +
            "<p>Check run details at <a href=\"${buildUrl}\">${jobName}</a></p>"

//        def recipients = emailextrecipients([
//                [$class: 'CulpritsRecipientProvider'],
//                [$class: 'DevelopersRecipientProvider'],
//                [$class: 'RequesterRecipientProvider'],
//                [$class: 'UpstreamComitterRecipientProvider'],
//                [$class: 'FirstFailingBuildSuspectsRecipientProvider']
//        ])

    /* Send e-mail notification */
    emailext(
//                to: recipients,
            to: 'serhii@skymind.io',
            subject: subject,
            body: details,
            mimeType: 'text/html'
    )
}
