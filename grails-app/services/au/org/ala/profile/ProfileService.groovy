package au.org.ala.profile
import grails.transaction.Transactional
import org.xml.sax.SAXException

@Transactional
class ProfileService {

    def serviceMethod() {}

    def nameService

    def importSponges(){
        def spongeOpus = Opus.findByDataResourceUid("dr824")
        if(!spongeOpus){
            spongeOpus = new Opus(
                    uuid : UUID.randomUUID().toString(),
                    dataResourceUid:  "dr824",
                    title: "Spongemaps",
                    imageSources: ["dr344"],
                    recordSources : ["dr344"],
                    logoUrl: "http://collections.ala.org.au/data/institution/QMN_logo.jpg",
                    bannerUrl: "http://images.ala.org.au/store/a/0/5/0/12c3a0cc-8a7a-4731-946a-6d481a60050a/thumbnail_large"
            )
            spongeOpus.save(flush:true)
        }
    }

    def cleanupText(str){
        if(str){
            str.replaceAll('<i>', '').replaceAll('</i>', '')
        } else {
            str
        }
    }

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
                    contributors << cleanupText(it.CONTRIBUTOR.text())
                }

                def distributions = []
                foaProfile.ROWSET.ROW.DISTRIBUTIONS?.DISTRIBUTIONS_ITEM?.each {
                    distributions << cleanupText(it.DIST_TEXT.text())
                }

                def parsed = [
                        scientificName: foaProfile.ROWSET.ROW.TAXON_NAME?.text(),
                        habitat       : cleanupText(foaProfile.ROWSET.ROW?.HABITAT?.text()),
                        source        : cleanupText(foaProfile.ROWSET.ROW.SOURCE.text()),
                        description   : cleanupText(foaProfile.ROWSET.ROW.DESCRIPTION?.text()),
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
                            contribs << new Contributor(uuid:UUID.randomUUID().toString(), name: it, dataResourceUid: foaOpus.dataResourceUid)
                        }
                    }

                    def oldFoaLink = new Link(
                            uuid:UUID.randomUUID().toString(),
                            title: parsed.scientificName,
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
}
