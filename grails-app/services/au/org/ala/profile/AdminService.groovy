package au.org.ala.profile

import org.springframework.scheduling.annotation.Async

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

import static groovyx.gpars.GParsPool.withPool

class AdminService extends BaseDataAccessService {
    static final int THREAD_POOL_SIZE = 10

    NameService nameService

    @Async
    void rematchAllNames(List<String> opusIds) {
        long start = System.currentTimeMillis()

        List<Opus> collections
        if (opusIds) {
            collections = opusIds.collect { Opus.findByUuid(it) }
        } else {
            collections = Opus.list()
        }

        Map<String, Map> results = [:] as ConcurrentHashMap

        NameRematch rematch = new NameRematch(uuid: UUID.randomUUID().toString(), startDate: new Date(), opusIds: opusIds)
        save rematch
        AtomicInteger changed = new AtomicInteger(0)
        AtomicInteger total = new AtomicInteger(0)

        collections.each { Opus opus ->
            List<Profile> profiles = Profile.findAllByOpus(opus)

            log.debug("Processing Opus ${opus.title} (${opus.uuid}) with ${profiles.size()} profiles...")
            results[opus.uuid] = [:] as ConcurrentHashMap
            results[opus.uuid].totalProfiles = profiles.size()
            results[opus.uuid].opusTitle = opus.title
            results[opus.uuid].profilesUpdated = [:] as ConcurrentHashMap

            withPool(THREAD_POOL_SIZE) {
                profiles.each { Profile profile ->
                    try {
                        Map<String, String> classification = profile.classification?.collectEntries {
                            [(it.rank): it.name]
                        } ?: [:]

                        Map newMatchedName
                        if (profile.manuallyMatchedName) {
                            newMatchedName = nameService.matchName(profile.matchedName.scientificName, classification, profile.matchedName.guid)
                        } else {
                            newMatchedName = nameService.matchName(profile.scientificName, classification)
                        }

                        if (profile.matchedName?.guid != newMatchedName?.guid) {
                            results[opus.uuid].profilesUpdated << [
                                    (profile.uuid): [
                                            profileName: profile.scientificName,
                                            old: [guid: profile.guid, name: profile.matchedName?.fullName],
                                            new: [guid: newMatchedName?.guid, name: newMatchedName?.fullName]
                                    ]
                            ]
                            profile.matchedName = new Name(newMatchedName)
                            profile.guid = newMatchedName?.guid

                            save profile

                            changed.incrementAndGet()
                        }
                    } catch (Exception e) {
                        log.error("Failed to match ${profile.scientificName}", e)
                        results[opus.uuid] << [(profile.uuid): [error: "Failed to match ${profile.scientificName}: ${e.message}. See log for stacktrace."]]
                    }
                    total.incrementAndGet()
                }
            }
        }

        log.debug("Name rematched finished in ${System.currentTimeMillis() - start}ms")
        rematch.numberOfProfilesChanged = changed.get()
        rematch.numberOfProfilesChecked = total.get()
        rematch.results = results
        rematch.endDate = new Date()
        save rematch
    }

    Tag createTag(Map properties) {
        properties.abbrev = properties.abbrev.toUpperCase()
        Tag tag = new Tag(properties)
        tag.uuid = UUID.randomUUID().toString()

        boolean success = save tag

        if (!success) {
            tag = null
        }

        tag
    }

    Tag updateTag(String tagId, Map properties) {
        Tag tag = Tag.findByUuid(tagId)

        if (tag) {
            tag.colour = properties.colour
            tag.name = properties.name
            tag.abbrev = properties.abbrev.toUpperCase()

            save tag
        }

        tag
    }

    void deleteTag(String tagId) {
        Tag tag = Tag.findByUuid(tagId)

        if (tag) {
            tag.delete()

            Opus.list().each { opus ->
                Tag t = opus.tags?.find {
                    it.uuid == tagId
                }
                if (t) {
                    opus.tags.remove(t)
                    save opus
                }
            }
        }
    }
}
