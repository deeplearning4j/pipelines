 configFileProvider([configFile(fileId: settings_xml, variable: 'MAVEN_SETTINGS')]) {
    bat "cp ${MAVEN_SETTINGS} ~/.m2/"

 }