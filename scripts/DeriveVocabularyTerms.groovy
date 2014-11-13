
terms = [].toSet()

new File("/data/foa").listFiles().each {
    try {
        foaProfile = new XmlParser().parseText(it.text)
        foaProfile.depthFirst().each { node -> terms << node.name() }
    } catch (Exception e){}
}

terms.sort().eachWithIndex { term, idx -> println (idx + ": " + term) }





