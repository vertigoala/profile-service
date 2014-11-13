
foaProfile = new XmlParser().parseText(new File("/data/foa/40160.xml").text)

contributors = []

foaProfile.ROWSET.ROW.CONTRIBUTORS?.CONTRIBUTORS_ITEM?.each {
    contributors << it.CONTRIBUTOR.text()
}

distributions = []
foaProfile.ROWSET.ROW.DISTRIBUTIONS?.DISTRIBUTIONS_ITEM?.each {
    distributions << it.DIST_TEXT.text()
}

parsed = [
    taxonName: foaProfile.ROWSET.ROW.TAXON_NAME?.text(),
    habitat: foaProfile.ROWSET.ROW?.HABITAT?.text(),
    source: foaProfile.ROWSET.ROW.SOURCE.text().replaceAll('<i>','').replaceAll('</i>',''),
    description: foaProfile.ROWSET.ROW.DESCRIPTION?.text(),
    distributions: distributions,
    contributor: contributors
]

println(parsed)