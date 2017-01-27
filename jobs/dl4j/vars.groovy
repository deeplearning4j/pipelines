/*
        // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
        // Setup variables for current run
        def RELEASE_VERSION
//      def SNAPSHOT_VERSION // Commented I think this value willl be deprecated as legacy
//      def STAGING_REPOSITORY // Commented I think this value willl be deprecated as legacy
//      def PRESETSDIR // Commented I think this value willl be deprecated as legacy
        def ACCOUNT
        def PROJECT
        def LIBPROJECT
        def PLATFORM_NAME
        def ARBITER_PROJECT
        def DEEPLEARNING4J_PROJECT
        def GYM_JAVA_CLIENT_PROJECT
        def ND4S_PROJECT
        def RL4J_PROJECT
        def SCALNET_PROJECT
        def DATAVEC_PROJECT
//      def OpenBLAS_HOME // Commented I think this value willl be deprecated as legacy
//      def ANDROID_NDK // Commented I think this value willl be deprecated as legacy
        def GITCREDID // Not sure is this is required in the end of

        // * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
        // This step required to predefine user input
        stage('SetVariables') {
            def userInput = input(
                    id: 'userInput', message: 'Let\'s promote?', parameters: [
                    [$class: 'TextParameterDefinition', defaultValue: '1.0.0-SNAPSHOT', description: 'Release Version', name: 'Release'],
//                  [$class: 'TextParameterDefinition', defaultValue: '1.0.0-SNAPSHOT', description: 'Snapshot Version', name: 'Snapshot'],
//                  [$class: 'TextParameterDefinition', defaultValue: '', description: 'Choose Repository', name: 'Repository'],
//                  [$class: 'TextParameterDefinition', defaultValue: 'presets', description: 'Set directory', name: 'dirSet'],
                    [$class: 'TextParameterDefinition', defaultValue: 'deeplearning4j', description: 'Owner of the project at github.com', name: 'accountName'],
                    [$class: 'TextParameterDefinition', defaultValue: 'nd4j', description: 'Git Project Name', name: 'ProjectName'],
                    [$class: 'TextParameterDefinition', defaultValue: 'libnd4j', description: 'Project Name for libraries', name: 'libProjectName'],
                    [$class: 'ChoiceParameterDefinition', choices: "linux-x86_64\nandroid-arm\nandroid-x86\nlinux-x86\nlinux-ppc64le", description: 'OpenBLAS home directory', name: 'Platform'],
                    [$class: 'TextParameterDefinition', defaultValue: 'arbiter', description: 'Git Project Name', name: 'arbiterProject'],
                    [$class: 'TextParameterDefinition', defaultValue: 'deeplearning4j', description: 'Git Project Name', name: 'deeplearning4jProject'],
                    [$class: 'TextParameterDefinition', defaultValue: 'gym-java-client', description: 'Git Project Name', name: 'gymjavaclientProject'],
                    [$class: 'TextParameterDefinition', defaultValue: 'nd4s', description: 'Git Project Name', name: 'nd4sProject'],
                    [$class: 'TextParameterDefinition', defaultValue: 'rl4j', description: 'Git Project Name', name: 'rl4jProject'],
                    [$class: 'TextParameterDefinition', defaultValue: 'scalnet', description: 'Git Project Name', name: 'scalnetProject'],
                    [$class: 'TextParameterDefinition', defaultValue: 'datavec', description: 'Git Project Name', name: 'datavecProject'],
//                  [$class: 'TextParameterDefinition', defaultValue: '$WORKSPACE/javacpp-presets/openblas/cppbuild/$PLATFORM_NAME/', description: 'OpenBLAS home directory"', name: 'BlasHome'],
//                  [$class: 'TextParameterDefinition', defaultValue: '/opt/android-ndk/', description: 'Android NDK home directory', name: 'NDK']
                    [$class: 'TextParameterDefinition', defaultValue: 'github-private-deeplearning4j-id-1', description: 'Git Project Name', name: 'gitCreditValue']

            ])

            RELEASE_VERSION = userInput['Release']
//          SNAPSHOT_VERSION = userInput['Snapshot']
//          STAGING_REPOSITORY = userInput['Repository']
//          PRESETSDIR = userInput['dirSet']
            ACCOUNT = userInput['accountName']
            PROJECT = userInput['ProjectName']
            LIBPROJECT = userInput['libProjectName']
            PLATFORM_NAME = userInput['Platform']
            ARBITER_PROJECT = userInput['arbiterProject']
            DEEPLEARNING4J_PROJECT = userInput['deeplearning4jProject']
            GYM_JAVA_CLIENT_PROJECT = userInput['gymjavaclientProject']
            ND4S_PROJECT = userInput['nd4sProject']
            RL4J_PROJECT = userInput['rl4jProject']
            SCALNET_PROJECT = userInput['scalnetProject']
            DATAVEC_PROJECT = userInput['']
//          OpenBLAS_HOME = userInput['BlasHome']
//          ANDROID_NDK = userInput['NDK']
            GITCREDID = userInput['gitCreditValue']
        }
 */
