 
 node ('windows-slave') {

    checkout scm

    load "jobs/dl4j/vars.groovy"
    functions = load "jobs/dl4j/functions.groovy"
    configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
        bat '''
        start /i /b /wait "C:\\Program Files (x86)\\Microsoft Visual Studio 12.0\\VC\\bin\\amd64\\vcvars64.bat"
        git clone -b intropro072-01 https://github.com/deeplearning4j/nd4j.git
        cd nd4j
        mvn -s %MAVEN_SETTINGS% clean install -Dmaven.test.skip=true
        '''
    }
}