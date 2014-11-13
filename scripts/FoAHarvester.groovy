println "Harvesting FoA content..."
currentId = 40155
endId = 48965
while(currentId <= endId){
  xml = new URL("""http://www.anbg.gov.au/abrs/online-resources/flora/stddisplay.xsql?xml-stylesheet=none&pnid=${currentId}""").text
  new File("/data/foa/${currentId}.xml").write(xml)
  currentId++
}



