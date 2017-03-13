node ('windows-x86_64') {
    checkout scm
    load "job/dl4j/vars.groovy"
    functions = load "job/dl4j/functions.groovy"
    configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
        bat (
            'vcvars64.bat' +
            '&&' +
            'git clone https://github.com/deeplearning4j/nd4j.git' +
            '&&' +
            'cd nd4j' +
            '&&' +
            'mvn -s %MAVEN_SETTINGS% clean install -Dmaven.test.skip=true'
        )
    }
}