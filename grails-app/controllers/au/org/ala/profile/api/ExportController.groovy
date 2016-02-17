package au.org.ala.profile.api

import au.org.ala.profile.Attribute
import au.org.ala.profile.BaseController
import au.org.ala.profile.Opus
import au.org.ala.profile.Profile
import au.org.ala.profile.Vocab
import grails.converters.JSON
import groovyx.net.http.ContentType

class ExportController extends BaseController {

    static final int DEFAULT_MAXIMUM_PAGE_SIZE = 500

    def countProfiles() {
        if (!params.opusId) {
            badRequest "opusId is a required parameter"
        } else {
            Opus opus = getOpus()

            if (!opus) {
                notFound "No matching collection was found for opus id ${params.opusId}"
            } else {
                boolean includeArchived = params.includeArchived?.toBoolean()

                int count = getProfileCount(opus, includeArchived)

                render([profiles: count] as JSON)
            }
        }
    }

    private int getProfileCount(Opus opus, boolean includeArchived) {
        includeArchived ? Profile.countByOpus(opus) : Profile.countByOpusAndArchivedDateIsNull(opus)
    }

    def exportCollection() {
        if (!params.opusId) {
            badRequest "opusId is a required parameter"
        } else {
            Opus opus = getOpus()

            if (!opus) {
                notFound "No matching collection was found for opus id ${params.opusId}"
            } else {
                Map attributeTitles = getVocabTerms(opus.attributeVocabUuid)
                Map authorCategories = getVocabTerms(opus.authorshipVocabUuid)

                int max = params.max ? params.max as int : DEFAULT_MAXIMUM_PAGE_SIZE
                int offset = params.offset ? params.offset as int : 0
                boolean includeArchived = params.includeArchived?.toBoolean()

                Map query = [opus: opus.id]
                if (!includeArchived) {
                    query << [archivedDate: null]
                }

                def profiles = Profile.collection.find(query).sort([scientificName: 1]).skip(offset).limit(max)
                long start = System.currentTimeMillis()
                log.debug("Starting")

                int totalProfileCount = getProfileCount(opus, includeArchived)

                // result sets can be large, so we don't want to load them all into memory then transform them.
                // Therefore, write objects from the db cursor (stream) directly to the output stream, transforming as
                // we go.
                int count = 0
                response.contentType = ContentType.JSON
                response.outputStream.with { writer ->
                    writer << "{ \"profilesInCollection\": ${totalProfileCount}, "
                    writer << ' "profiles": ['
                    while (profiles.hasNext()) {
                        Map data = profiles.next()
                        Map profile = [
                                id               : data.uuid,
                                name             : data.scientificName,
                                fullName         : data.fullName,
                                rank             : data.rank,
                                taxonomy         : [ source: data.taxonomyTree, classification: data.classification],
                                matchedName      : data.matchedName,
                                nameAuthor       : data.nameAuthor,
                                nslNameId        : data.nslNameIdentifier,
                                nslNomenclatureId: data.nslNomenclatureIdentifier,
                                acknowledgements : data.authorship?.collectEntries {
                                    [(authorCategories[it.category]): it.text]
                                },
                                dateCreated      : data.dateCreated,
                                lastUpdate       : data.lastUpdated,
                                attributes       : []
                        ]

                        def attributeCursor = Attribute.collection.find([profile: data._id])
                        while (attributeCursor.hasNext()) {
                            Map attribute = attributeCursor.next()

                            profile.attributes << [
                                    id   : attribute.uuid,
                                    title: attributeTitles[attribute.title],
                                    text : attribute.text
                            ]
                        }

                        writer << com.mongodb.util.JSON.serialize(profile)
                        if (profiles.hasNext()) {
                            writer << ","
                        }
                        count++
                    }

                    writer << "]}"
                }
                log.debug("Finished exporting ${count} profiles in ${System.currentTimeMillis() - start}ms")
            }
        }
    }

    private Map<String, String> getVocabTerms(String vocabId) {
        Vocab.findByUuid(vocabId)?.terms?.collectEntries { [it.id, it.name] }
    }
}
