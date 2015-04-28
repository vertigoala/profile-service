package au.org.ala.profile

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicInteger

import static groovyx.gpars.GParsPool.*

import static org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4
import org.xml.sax.SAXException

class ImportService extends BaseDataAccessService {

    static final int IMPORT_THREAD_POOL_SIZE = 10

    ProfileService profileService
    NameService nameService

    def foaOpusId = "2f75e6c9-7034-409b-b27c-3864326bee41"
    def spongesOpusId = "e3e35631-d864-44ed-a0b1-2c707bbc6d61"

    def importSponges() {
        def spongeOpus = Opus.findByDataResourceUid("dr824")
        if (!spongeOpus) {
            spongeOpus = new Opus(
                    opusId: spongesOpusId,
                    dataResourceUid: "dr824",
                    title: "Spongemaps",
                    imageSources: ["dr344"],
                    recordSources: ["dr344"],
                    logoUrl: "http://collections.ala.org.au/data/institution/QMN_logo.jpg",
                    bannerUrl: "http://images.ala.org.au/store/a/0/5/0/12c3a0cc-8a7a-4731-946a-6d481a60050a/thumbnail_large",
                    enablePhyloUpload: true,
                    enableOccurrenceUpload: true,
                    enableTaxaUpload: true,
                    enableKeyUpload: true
            )
            spongeOpus.save(flush: true)

            spongeOpus.getErrors().getAllErrors().each { println it }
        }
    }

    def cleanupText(str) {
        if (str) {
            str = unescapeHtml4(str)
            // preserve line breaks as new lines, remove all other html
            str = str.replaceAll(/<p\/?>|<br\/?>/, "\n").replaceAll(/<.+?>/, "").trim()
        }
        return str
    }

    Term getOrCreateTerm(String vocabId, String name) {
        Vocab vocab = Vocab.findByUuid(vocabId)
        Term term = Term.findByNameAndVocab(name, vocab)
        if (!term) {
            term = new Term(name: name, vocab: vocab)
            term.save(flush: true)
        }
        term
    }

    def importFOA() {
        String FLORA_AUSTRALIA_VOCAB = "7dba0bab-65d2-4a22-a682-c13b4e301f70"

        def opusModel = [
                opusId                : foaOpusId,
                dataResourceUid       : "dr382",
                title                 : "Flora of Australia",
                imageSources          : ["dr382", "dr413", "dr689"],
                recordSources         : ["dr376"],
                logoUrl               : "https://fieldcapture.ala.org.au/static/RrjzrZ0Ci0GPLETIr8x8KUMjfJtZKvifrUtMCedwKRB.png",
                bannerUrl             : "http://www.anbg.gov.au/images/photo_cd/FLIND_RANGES/fr-3_3.jpg",
                attributeVocabUuid    : FLORA_AUSTRALIA_VOCAB,
                enablePhyloUpload     : false,
                enableOccurrenceUpload: false,
                enableTaxaUpload      : false,
                enableKeyUpload       : false,
                mapAttribution        : 'Australian Virtual Herbarium (CHAH)',
                biocacheUrl           : 'http://avh.ala.org.au',
                biocacheName          : 'Australian Virtual Herbarium',
                glossary              : new Glossary(uuid: UUID.randomUUID().toString())
        ]

        def foaOpus = Opus.findByDataResourceUid("dr382")
        if (!foaOpus) {
            foaOpus = new Opus(opusModel)
            save foaOpus
        }

        assert Opus.findByDataResourceUid("dr382") != null

        Vocab vocab = Vocab.findByUuid(FLORA_AUSTRALIA_VOCAB)
        if (!vocab) {
            vocab = new Vocab(uuid: FLORA_AUSTRALIA_VOCAB, name: "Flora of Australia Vocabulary")
            save vocab
        }

        Term habitatTerm = getOrCreateTerm(FLORA_AUSTRALIA_VOCAB, "Habitat")
        Term descriptionTerm = getOrCreateTerm(FLORA_AUSTRALIA_VOCAB, "Description")
        Term distributionTerm = getOrCreateTerm(FLORA_AUSTRALIA_VOCAB, "Distribution")

        new File("/data/foa").listFiles().each {
            println "Processing ${it.name}..."
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
                            profileId     : UUID.randomUUID().toString(),
                            guid          : guid,
                            scientificName: parsed.scientificName,
                            opus          : foaOpus
                    ])

                    if (profile.guid) {
                        profileService.populateTaxonHierarchy(profile)
                    }

                    profile.attributes = []

                    if (parsed.habitat) {
                        profile.attributes << new Attribute(uuid: UUID.randomUUID().toString(), title: habitatTerm, text: parsed.habitat, profile: profile)
                    }
                    if (parsed.description) {
                        profile.attributes << new Attribute(uuid: UUID.randomUUID().toString(), title: descriptionTerm, text: parsed.description, profile: profile)
                    }

                    parsed.distributions.each {
                        if (it) {
                            profile.attributes << new Attribute(uuid: UUID.randomUUID().toString(), title: distributionTerm, text: it, profile: profile)
                        }
                    }

                    //associate the contributors with all attributes
                    def contribs = []
                    contributors.each {
                        def retrieved = Contributor.findByName(it)
                        if (retrieved) {
                            contribs << retrieved
                        } else {
                            contribs << new Contributor(uuid: UUID.randomUUID().toString(), name: it, dataResourceUid: foaOpus.dataResourceUid)
                        }
                    }

                    def oldFoaLink = new Link(
                            uuid: UUID.randomUUID().toString(),
                            title: parsed.scientificName,
                            description: "Old Flora of Australia site page for ${parsed.scientificName}",
                            url: "http://www.anbg.gov.au/abrs/online-resources/flora/stddisplay.xsql?pnid=" + it.getName().replace(".xml", "")
                    )

                    profile.links = [oldFoaLink]

                    profile.attributes.each {
                        it.creators = contribs
                    }

                    profile.save(flush: true)

                    profile.errors.allErrors.each {
                        println(it)
                    }
                }
            } catch (SAXException se) {
                //se.printStackTrace()
            } catch (Exception e) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Profile import is a two-pass operation:
     * 1.) Use multiple threads to loop through the provided data set to find all values that should be unique (e.g. attribute terms, contributors, etc)
     * 1.1) Using a single thread, store them in the database and return a map of references.
     * 2.) Use multiple threads to loop through the provided data set again to process each individual record, retrieving references to unique values from the maps from pass 1.
     *
     * Querying data from the database, and writing records if they don't exist, is the slowest part of the import process.
     * Executing these 'get or create' actions concurrently results in race conditions and duplicated data.
     * Synchronising the methods resolves this issue, but dramatically increases the execution time of the import.
     *
     * The two-pass approach ensures no duplicate records are created, and does not significantly impact performance.
     *
     * @param opusId
     * @param profilesJson
     * @return
     */
    Map<String, String> importProfiles(String opusId, profilesJson) {
        Opus opus = Opus.findByUuid(opusId);

        Map<String, String> results = [:] as ConcurrentHashMap

        AtomicInteger success = new AtomicInteger(0)
        AtomicInteger index = new AtomicInteger(0)
        int reportInterval = 0.05 * profilesJson.size() // log at 5% intervals

        Map uniqueValues = findAndStoreUniqueValues(opus, profilesJson)
        Map<String, Term> vocab = uniqueValues.vocab
        Map<String, Contributor> contributors = uniqueValues.contributors

        log.info "Importing profiles ..."
        withPool(IMPORT_THREAD_POOL_SIZE) {
            profilesJson.eachParallel {
                index.incrementAndGet()
                Profile profile = Profile.findByScientificNameAndOpus(it.scientificName, opus);
                if (profile) {
                    log.info("Profile already exists in this opus for scientific name ${it.scientificName}")
                    results << [(it.scientificName): "Already exists"]
                } else {
                    if (!it.scientificName) {
                        results << [("Row${index}"): "Failed to import row ${index}, does not have a scientific name"]
                    } else {
                        List<String> guidList = nameService.getGuidForName(it.scientificName)
                        String guid = null
                        if (guidList && guidList.size() > 0) {
                            guid = guidList[0]
                        }

                        profile = new Profile(scientificName: it.scientificName, opus: opus, guid: guid, attributes: [], links: [], bhlLinks: []);

                        if (profile.guid) {
                            profileService.populateTaxonHierarchy(profile)
                        }

                        it.links.each {
                            if (it) {
                                profile.links << createLink(it, contributors)
                            }
                        }

                        it.bhl.each {
                            if (it) {
                                profile.bhlLinks << createLink(it, contributors)
                            }
                        }

                        Set<String> contributorNames = []
                        it.attributes.each {
                            if (it.title && it.text) {
                                Term term = vocab.get(it.title.trim())

                                String text = cleanupText(it.text)
                                if (text) {
                                    Attribute attribute = new Attribute(title: term, text: text)
                                    attribute.uuid = UUID.randomUUID().toString()

                                    if (it.creators) {
                                        attribute.creators = []
                                        it.creators.each {
                                            String name = cleanName(it)
                                            if (name) {
                                                Contributor contrib = contributors[name]
                                                if (contrib) {
                                                    attribute.creators << contrib
                                                    contributorNames << contrib.name
                                                } else {
                                                    log.warn("Missing contributor for name '${name}'")
                                                }
                                            }
                                        }
                                    }

                                    if (it.editors) {
                                        attribute.editors = []
                                        it.editors.each {
                                            String name = cleanName(it)
                                            if (name) {
                                                attribute.editors << contributors[name]
                                            }
                                        }
                                    }

                                    attribute.profile = profile
                                    profile.attributes << attribute
                                }
                            }
                        }

                        profile.authorship = [new Authorship(category: "Author", text: contributorNames.join(", "))]

                        profile.save(flush: true)

                        if (profile.errors.allErrors.size() > 0) {
                            log.error("Failed to save ${profile}")
                            profile.errors.each { log.error(it) }
                            results << [(it.scientificName): "Failed: ${profile.errors.allErrors.get(0)}"]
                        } else {
                            results << [(it.scientificName): "Success"]
                            success.incrementAndGet()
                            if (index % reportInterval == 0) {
                                log.debug("Saved ${success} of ${profilesJson.size()}")
                            }
                        }
                    }
                }
            }
        }
        log.debug "${success} of ${profilesJson.size()} records imported"

        results
    }

    Map findAndStoreUniqueValues(Opus opus, profilesJson) {
        Set<String> uniqueTerms = [] as ConcurrentSkipListSet
        Set<String> uniqueContributors = [] as ConcurrentSkipListSet

        log.info "Retrieving unique data..."
        withPool(IMPORT_THREAD_POOL_SIZE) {
            profilesJson.eachParallel {
                it.attributes.each { attr ->
                    if (attr.title) {
                        uniqueTerms << attr.title.trim()
                    }

                    attr.editors?.each { name ->
                        uniqueContributors << cleanName(name)
                    }
                    attr.creators?.each { name ->
                        uniqueContributors << cleanName(name)
                    }
                }
            }
        }

        log.info "Storing unique vocabulary (${uniqueTerms.size()} terms) ..."
        Map<String, Term> vocab = [:]
        uniqueTerms.each {
            vocab[it] = getOrCreateTerm(opus.attributeVocabUuid, it)
        }

        log.info "Storing unique contributors (${uniqueContributors.size()} names) ..."
        Map<String, Contributor> contributors = [:]
        uniqueContributors.each {
            contributors[it] = getOrCreateContributor(it, opus.dataResourceUid)
        }

        [vocab: vocab, contributors: contributors]
    }

    private static cleanName(String name) {
        name.replaceAll("\\(.*\\)", "").replaceAll(" +", " ").trim()
    }

    Contributor getOrCreateContributor(String name, String opusDataResourceId) {
        Contributor contributor = Contributor.findByName(name)
        if (!contributor) {
            contributor = new Contributor(name: name, uuid: UUID.randomUUID().toString(), dataResourceUid: opusDataResourceId)
        }
        contributor
    }

    Link createLink(data, contributors) {
        Link link = new Link(data)
        link.uuid = UUID.randomUUID().toString()

        if (data.creators) {
            link.creators = []
            data.creators.each {
                String name = cleanName(it)
                if (name) {
                    link.creators << contributors[name]
                }
            }
        }

        link
    }
}
