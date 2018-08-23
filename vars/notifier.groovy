#!/usr/bin/env groovy

import net.sf.json.JSONArray
import net.sf.json.JSONObject
import hudson.tasks.junit.CaseResult
import hudson.tasks.junit.TestResultAction

void sendSlackNotification(Map args) {
    String jobResult = args.containsKey('jobResult') ? args.jobResult ?: 'SUCCESS' :
            error('Missing jobResult argument!') // Job result of null means successful

    Boolean isMember = args.containsKey('isMember') ? args.isMember : false

    Map checkoutDetails = args.containsKey('checkoutDetails') ? args.checkoutDetails :
            error('Missing checkoutDetails argument!')

    List testResults = args.containsKey('testResults') ? args.testResults : []

    String jobName = currentBuild.rawBuild.fullDisplayName
    String jobUrl = env.RUN_DISPLAY_URL
    String subject = "${jobResult.toLowerCase().capitalize()} | ${jobName}"
    String committerFirstName = (checkoutDetails.GIT_COMMITER_NAME =~ /(\w+)/)[0][1]
    String committerEmail = checkoutDetails.GIT_COMMITER_EMAIL
    String committer = checkoutDetails.GIT_COMMITER_NAME + (committerEmail ? " (${committerEmail})" : '')
    String notificationColor

    switch (jobResult) {
        case 'SUCCESS':
            notificationColor = "good"
            break
        case 'STARTED':
        case 'ABORTED':
            notificationColor = "info"
            break
        case 'UNSTABLE':
            notificationColor = "warning"
            break
        case 'FAILURE':
            notificationColor = "danger"
            break
        default:
            notificationColor = "danger"
            break
    }

    // Main job details
    JSONObject jobDetails = new JSONObject()
    List jobDetailsFields = []

    jobDetails.put('title', jobName)
    jobDetails.put('title_link', jobUrl)
    jobDetails.put('color', notificationColor)
    jobDetails.put('pretext', ":gear: Job status: " + jobResult.toLowerCase().capitalize())
    jobDetails.put('fallback', subject)
    jobDetails.put('mrkdwn_in', ["fields"])

    // Git branch details
    JSONObject branch = new JSONObject()
    branch.put('title', 'Branch')
    branch.put('value', checkoutDetails.GIT_BRANCH)
    branch.put('short', true)
    jobDetailsFields.add(branch)

    // Commit author details
    JSONObject author = new JSONObject()
    author.put('title', 'Author')
    author.put('value', committer)
    author.put('short', true)
    jobDetailsFields.add(author)

    // Commit message details
    JSONObject commitMessage = new JSONObject()
    commitMessage.put('title', 'Commit message')
    commitMessage.put('value', checkoutDetails.GIT_COMMIT_MESSAGE)
    commitMessage.put('short', false)
    jobDetailsFields.add(commitMessage)

    // Commit ID details
    JSONObject commitId = new JSONObject()
    commitId.put('title', 'Commit ID')
    commitId.put('value', checkoutDetails.GIT_COMMIT)
    commitId.put('short', false)
    jobDetailsFields.add(commitId)

    // Test results details
    JSONObject failedTestDetails = new JSONObject()

    if (testResults) {
        for (tr in testResults) {
            Map testResult = tr
            String platformName = testResult?.platform
            String testsInfo = testResult?.testResults
            String failedTests = parseFailedTestResults()

            if (testsInfo) {
                JSONObject testsInfoJson = new JSONObject()
                testsInfoJson.put('title', 'Test results for' +
                        (platformName ? ' ' + platformName : '')
                )
                testsInfoJson.put('value', testsInfo)
                testsInfoJson.put('short', false)
                jobDetailsFields.add(testsInfoJson)
            }

            if (failedTests) {
                failedTestDetails.put('title', 'Failed test results')
                failedTestDetails.put('color', notificationColor)
                failedTestDetails.put('text', failedTests)
                failedTestDetails.put('mrkdwn_in', ['text'])
            }
        }
    }

    jobDetails.put('fields', jobDetailsFields)

    JSONArray attachments = new JSONArray()
    attachments.add(jobDetails)
    attachments.add(failedTestDetails)

    /* Send Slack notifications to member */
    if (isMember) {
        slackSend color: notificationColor, channel: "@${committerFirstName}",
                attachments: attachments.toString()
    }

    /* Send Slack notifications to common channel */
    slackSend color: notificationColor, attachments: attachments.toString()
}

void sendEmailNotification(Map args) {
    // Job result of null means successful
    String jobResult = args.containsKey('jobResult') ? args.jobResult ?: 'SUCCESS' :
            error('Missing jobResult argument!')
    String jobName = currentBuild.rawBuild.fullDisplayName
    String changeId = env.CHANGE_ID
    String changeTitle = env.CHANGE_TITLE
    String changeAuthor = env.CHANGE_AUTHOR
    String changeAuthorEmail = env.CHANGE_AUTHOR_EMAIL
    String jobUrl = env.RUN_DISPLAY_URL

    String subject = "${jobResult.toLowerCase().capitalize()} | ${jobName}"
    String details = ((changeId) ? "<p>Changes: ${changeId}, ${changeTitle}</p>\n" : '') +
            ((changeId) ? "<p>Author: ${changeAuthor} (${changeAuthorEmail})</p>\n" : '') +
            "<p>Check run details at <a href=\"${jobUrl}\">${jobName}</a></p>"

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

private String parseFailedTestResults() {
    TestResultAction testResult = currentBuild.rawBuild.getAction(TestResultAction.class)

    String failedTestResults = ''

    if (testResult != null) {
        def failedTests = testResult.getFailedTests()

        if (failedTests) {
            if (failedTests.size() > 9) {
                failedTests = failedTests.subList(0, 8)
            }

            for(CaseResult cr : failedTests) {
                CaseResult failedTest = cr

                String failedTestDescription = [
                        '\n',
                        "_*Test name*_",
                        failedTest.getFullDisplayName(),
                        "_*Error*_",
                        '```',
                        failedTest.getErrorDetails(),
                        '```',
                        "_*Stacktrace*_",
                        '```',
                        failedTest.getErrorStackTrace().trim(),
                        '```'
                ].findAll().join('\n')

                failedTestResults = failedTestResults + failedTestDescription
            }
        }

    }

    return failedTestResults
}