timestamps {
  def scriptName = this.class.getName()
  // println "Script FQCN : " + scriptName
  println "Script Simple Name : " + this.class.getSimpleName()

  // Messages for debugging
  echo 'MARK: end of test.groovy (in timestamps)'
}
// Messages for debugging
echo 'MARK: end of test.groovy'
