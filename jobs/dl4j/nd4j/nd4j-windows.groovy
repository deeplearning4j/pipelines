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