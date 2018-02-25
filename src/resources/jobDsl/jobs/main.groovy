#!/usr/bin/env groovy
String organizationFolderName = 'deeplearning4j'
String organizationFolderDisplayName = 'Eclipse Deeplearning4j'
String organizationFolderDescription = 'Open-source, distributed deep learning for the JVM on Spark with GPUs'
String organizationName = 'sshepel'
String organizationCredentialsId = 'b78c4354-1c49-43fd-a813-b48bf89e79a2'

organizationFolder(organizationFolderName) {
    description(organizationFolderDescription)
    displayName(organizationFolderDisplayName)
    properties {
        folderCredentialsProperty {
            domainCredentials {
                domainCredentials {
                    credentials {
                        usernamePasswordCredentialsImpl {
                            scope(String value)
                            id(String value)
                            description(String value)
                            username(String value)
                            password(String value)
                        }
                    }
                }
            }

        }
        noTriggerOrganizationFolderProperty {
            branches('master|release|PR-\\d+')
        }
    }
    orphanedItemStrategy {
        // Trims dead items by the number of days or the number of items.
        discardOldItems {
            // Sets the number of days to keep old items.
            daysToKeep(7)
            // Sets the number of old items to keep.
            numToKeep(42)
        }
    }
    organizations {
        github {
            // Specify the name of the GitHub Organization or GitHub User Account.
            repoOwner(organizationName)
            // The server to connect to.
            apiUri(String value)
            buildForkPRHead(boolean value)
            buildForkPRMerge(boolean value)
            buildOriginBranch(boolean value)
            buildOriginBranchWithPR(boolean value)
            buildOriginPRHead(boolean value)
            buildOriginPRMerge(boolean value)
            // Credentials used to scan branches and pull requests, check out sources and mark commit statuses.
            credentialsId(organizationCredentialsId)
            excludes(String value)
            includes(String value)
            pattern(String value)
            scanCredentialsId(String value)
        }

    }
    triggers {
        periodic(7200000)
    }
}