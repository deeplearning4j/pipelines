timestamps {
      step([$class: 'WsCleanup'])
  def scriptName = this.class.getName()
  // println "Script FQCN : " + scriptName
  println "Script Simple Name : " + this.class.getSimpleName()

  // Messages for debugging
  echo 'MARK: end of test.groovy (in timestamps)'
  step([$class: 'WsCleanup'])
}
// Messages for debugging
echo 'MARK: end of test.groovy'
