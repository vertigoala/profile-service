package au.org.ala.profile
import grails.transaction.Transactional
import org.xml.sax.SAXException

@Transactional
class ProfileService {

    def serviceMethod() {}

    def nameService

    def importFOA(){

        def foaOpus = Opus.findByDataResourceUid("dr382")
        if(!foaOpus){
            foaOpus = new Opus(
                uuid : UUID.randomUUID().toString(),
                dataResourceUid:  "dr382",
                title: "Flora of Australia",
                imageSources: ["dr382", "dr413", "dr689"],
                recordSources : ["dr376"],
                logoUrl: "https://fieldcapture.ala.org.au/static/RrjzrZ0Ci0GPLETIr8x8KUMjfJtZKvifrUtMCedwKRB.png",
                bannerUrl: "http://www.anbg.gov.au/images/photo_cd/FLIND_RANGES/fr-3_3.jpg"
            )
            foaOpus.save(flush:true)
        }

        new File("/data/foa").listFiles().each {
            try {
                def foaProfile = new XmlParser().parseText(it.text)

                def contributors = []

                foaProfile.ROWSET.ROW.CONTRIBUTORS?.CONTRIBUTORS_ITEM?.each {
                    contributors << it.CONTRIBUTOR.text()
                }

                def distributions = []
                foaProfile.ROWSET.ROW.DISTRIBUTIONS?.DISTRIBUTIONS_ITEM?.each {
                    distributions << it.DIST_TEXT.text()
                }

                def parsed = [
                        scientificName: foaProfile.ROWSET.ROW.TAXON_NAME?.text(),
                        habitat       : foaProfile.ROWSET.ROW?.HABITAT?.text(),
                        source        : foaProfile.ROWSET.ROW.SOURCE.text().replaceAll('<i>', '').replaceAll('</i>', ''),
                        description   : foaProfile.ROWSET.ROW.DESCRIPTION?.text(),
                        distributions : distributions,
                        contributor   : contributors
                ]

                if (parsed.scientificName) {

                    //lookup GUID
                    def guid = nameService.getGuidForName(parsed.scientificName)

                    //add a match to APC / APNI
                    def profile = new Profile([
                        uuid            : UUID.randomUUID().toString(),
                        guid            : guid,
                        scientificName  : parsed.scientificName,
                        opus            : foaOpus
                    ])

                     profile.attributes = []

                    if(parsed.habitat) {
                         profile.attributes << new Attribute(uuid: UUID.randomUUID().toString(), title: "Habitat", text: parsed.habitat)
                    }
                    if(parsed.description) {
                         profile.attributes << new Attribute(uuid: UUID.randomUUID().toString(), title: "Description", text: parsed.description)
                    }

                    parsed.distributions.each {
                        if(it) {
                             profile.attributes << new Attribute(uuid: UUID.randomUUID().toString(), title: "Distribution", text: it)
                        }
                    }

                    //associate the contributors with all attributes
                    def contribs = []
                    contributors.each {
                        def retrieved = Contributor.findByName(it)
                        if(retrieved){
                            contribs << retrieved
                        } else {
                            contribs << new Contributor(name: it, dataResourceUid: foaOpus.dataResourceUid)
                        }
                    }

                    def oldFoaLink = new Link(title: parsed.scientificName,
                            description: "Old Flora of Australia site page for " + parsed.scientificName,
                            url: "http://www.anbg.gov.au/abrs/online-resources/flora/stddisplay.xsql?pnid="+it.getName().replace(".xml", "")
                    )

                    profile.links = [oldFoaLink]

                    profile.attributes.each {
                        it.contributors = contribs
                    }

                     profile.save(flush: true)

                     profile.errors.allErrors.each {
                        println(it)
                    }
                }
            } catch (SAXException se){
                //se.printStackTrace()
            } catch (Exception e){
                e.printStackTrace()
            }
        }
    }
//
//    def createTaxonProfile(String guid){
//
//        def taxonProfile = new TaxonProfile([
//            uuid: UUID.randomUUID().toString(),
//            descriptions: [
//                new Attribute(uuid:UUID.randomUUID().toString(), title:"Habitat", text:"Forests and swamplands", authorName: "Dave Martin", authorId: "123"),
//                new Attribute(uuid:UUID.randomUUID().toString(), title:"Taxonomy", text:"Belongs in the family...", authorName: "Linnaeus", authorId: "132"),
//                new Attribute(uuid:UUID.randomUUID().toString(), title:"Geography", text:"Belongs in the family...", authorName: "Linnaeus", authorId: "132",
//                    subAttributes: [
//                        new Attribute(uuid:UUID.randomUUID().toString(), title:"South Australia", text:"Belongs in the family...", authorName: "Linnaeus", authorId: "132"),
//                        new Attribute(uuid:UUID.randomUUID().toString(), title:"New South Wales", text:"Belongs in the family...", authorName: "Linnaeus", authorId: "132")
//                    ]
//                ),
//            ]
//        ])
//
//        taxonProfile.save(flush:true)
//    }
}
