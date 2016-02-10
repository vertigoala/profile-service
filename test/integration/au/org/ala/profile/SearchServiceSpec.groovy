package au.org.ala.profile

import au.org.ala.web.AuthService


class SearchServiceSpec extends BaseIntegrationSpec {
    SearchService service = new SearchService()

    def "setup"() {
        service.authService = Mock(AuthService)
        service.userService = Mock(UserService)
    }

    def "findByScientificName should fail when no scientific name is provided"() {
        when:
        service.findByScientificName(null, ["opus"])

        then:
        thrown IllegalArgumentException
    }

    def "findByScientificName should match any opus when no opusId list is provided"() {
        given:
        Opus opus1 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1", title: "title1")
        Opus opus2 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr2", title: "title2")
        Opus opus3 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr3", title: "title3")

        Profile profile1 = save new Profile(scientificName: "name", opus: opus1, classification: [new Classification(rank: "kingdom", name: "Plantae")])
        Profile profile2 = save new Profile(scientificName: "name", opus: opus2, classification: [new Classification(rank: "kingdom", name: "Plantae")])
        Profile profile3 = save new Profile(scientificName: "name", opus: opus3, classification: [new Classification(rank: "kingdom", name: "Plantae")])

        when:
        List result = service.findByScientificName("name", null)

        then:
        result.size() == 3
        result.contains(profile1)
        result.contains(profile2)
        result.contains(profile3)
    }

    def "findByScientificName should exclude matches from private collections when there is no logged in user"() {
        given:
        Opus opus1 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1", title: "title1")
        Opus opus2 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr2", title: "title2", privateCollection: true, authorities: [[user: [userId: "9876"], role: "ROLE_USER"]])
        Opus opus3 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr3", title: "title3", privateCollection: true, authorities: [[user: [userId: "1234"], role: "ROLE_USER"]])

        Profile profile1 = save new Profile(scientificName: "name", opus: opus1, classification: [new Classification(rank: "kingdom", name: "Plantae")])
        Profile profile2 = save new Profile(scientificName: "name", opus: opus2, classification: [new Classification(rank: "kingdom", name: "Plantae")])
        Profile profile3 = save new Profile(scientificName: "name", opus: opus3, classification: [new Classification(rank: "kingdom", name: "Plantae")])

        service.authService.userInRole("ROLE_ADMIN") >> false
        service.userService.getCurrentUserDetails() >> [userId: null]

        when:
        List result = service.findByScientificName("name", null)

        then:
        result.size() == 1
        result.contains(profile1)
    }

    def "findByScientificName should include matches from private collections when the logged in user is registered with that collection"() {
        given:
        Opus opus1 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1", title: "title1")
        Opus opus2 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr2", title: "title2", privateCollection: true, authorities: [[user: [userId: "9876"], role: "ROLE_USER"]])
        Opus opus3 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr3", title: "title3", privateCollection: true, authorities: [[user: [userId: "1234"], role: "ROLE_USER"]])

        Profile profile1 = save new Profile(scientificName: "name", opus: opus1, classification: [new Classification(rank: "kingdom", name: "Plantae")])
        Profile profile2 = save new Profile(scientificName: "name", opus: opus2, classification: [new Classification(rank: "kingdom", name: "Plantae")])
        Profile profile3 = save new Profile(scientificName: "name", opus: opus3, classification: [new Classification(rank: "kingdom", name: "Plantae")])

        service.authService.userInRole("ROLE_ADMIN") >> false
        service.userService.getCurrentUserDetails() >> [userId: "1234"]

        when:
        List result = service.findByScientificName("name", null)

        then:
        result.size() == 2
        result.contains(profile1)
        result.contains(profile3)
    }

    def "findByScientificName should include matches from all private collections when the logged in user is an ALA admin"() {
        given:
        Opus opus1 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1", title: "title1")
        Opus opus2 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr2", title: "title2", privateCollection: true, authorities: [[user: [userId: "9876"], role: "ROLE_USER"]])
        Opus opus3 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr3", title: "title3", privateCollection: true, authorities: [[user: [userId: "1234"], role: "ROLE_USER"]])

        Profile profile1 = save new Profile(scientificName: "name", opus: opus1, classification: [new Classification(rank: "kingdom", name: "Plantae")])
        Profile profile2 = save new Profile(scientificName: "name", opus: opus2, classification: [new Classification(rank: "kingdom", name: "Plantae")])
        Profile profile3 = save new Profile(scientificName: "name", opus: opus3, classification: [new Classification(rank: "kingdom", name: "Plantae")])

        service.authService.userInRole("ROLE_ADMIN") >> true
        service.userService.getCurrentUserDetails() >> [userId: "1234"]

        when:
        List result = service.findByScientificName("name", null)

        then:
        result.size() == 3
        result.contains(profile1)
        result.contains(profile2)
        result.contains(profile3)
    }

    def "findByScientificName should match only the specified opus ids"() {
        given:
        Opus opus1 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1", title: "title1")
        Opus opus2 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr2", title: "title2")
        Opus opus3 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr3", title: "title3")

        Profile profile1 = save new Profile(scientificName: "name", opus: opus1, classification: [new Classification(rank: "kingdom", name: "Plantae")])
        Profile profile2 = save new Profile(scientificName: "name", opus: opus2, classification: [new Classification(rank: "kingdom", name: "Plantae")])
        Profile profile3 = save new Profile(scientificName: "name", opus: opus3, classification: [new Classification(rank: "kingdom", name: "Plantae")])

        when:
        List result = service.findByScientificName("name", [opus1.uuid, opus3.uuid])

        then:
        result.size() == 2
        result.contains(profile1)
        !result.contains(profile2)
        result.contains(profile3)
    }

    def "findByScientificName should ignore unknown opus ids"() {
        given:
        Opus opus1 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1", title: "title1")
        Opus opus2 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr2", title: "title2")
        Opus opus3 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr3", title: "title3")

        Profile profile1 = save new Profile(scientificName: "name", opus: opus1, classification: [new Classification(rank: "kingdom", name: "Plantae")])
        Profile profile2 = save new Profile(scientificName: "name", opus: opus2, classification: [new Classification(rank: "kingdom", name: "Plantae")])
        Profile profile3 = save new Profile(scientificName: "name", opus: opus3, classification: [new Classification(rank: "kingdom", name: "Plantae")])

        when:
        List result = service.findByScientificName("name", [opus1.uuid, "unknown"])

        then:
        result.size() == 1
        result.contains(profile1)
        !result.contains(profile2)
        !result.contains(profile3)
    }

    def "findByScientificName should default to using a wildcard"() {
        given:
        Opus opus1 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1", title: "title1")
        Profile profile1 = save new Profile(scientificName: "name", opus: opus1, classification: [new Classification(rank: "kingdom", name: "Plantae")])
        Profile profile2 = save new Profile(scientificName: "name two", opus: opus1, classification: [new Classification(rank: "kingdom", name: "Plantae")])
        Profile profile3 = save new Profile(scientificName: "nameThree", opus: opus1, classification: [new Classification(rank: "kingdom", name: "Plantae")])

        when:
        List result = service.findByScientificName("name", [opus1.uuid])

        then:
        result.size() == 3
        result.contains(profile1)
        result.contains(profile2)
        result.contains(profile3)
    }

    def "findByScientificName should perform exact match (case-insensitive) when useWildcard is false"() {
        given:
        Opus opus1 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1", title: "title1")
        Profile profile1 = save new Profile(scientificName: "name", opus: opus1, classification: [new Classification(rank: "kingdom", name: "Plantae")])
        Profile profile2 = save new Profile(scientificName: "name two", opus: opus1, classification: [new Classification(rank: "kingdom", name: "Plantae")])
        Profile profile3 = save new Profile(scientificName: "nameThree", opus: opus1, classification: [new Classification(rank: "kingdom", name: "Plantae")])

        when:
        List result = service.findByScientificName("NAME", [opus1.uuid], false)

        then:
        result.size() == 1
        result.contains(profile1)
        !result.contains(profile2)
        !result.contains(profile3)
    }

    def "findByScientificName should limit the results to the specified maximum (names sorted alphabetically"() {
        given:
        Opus opus = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1234", title: "title")

        Profile profile1 = save new Profile(scientificName: "ab", opus: opus, classification: [new Classification(rank: "kingdom", name: "plantae")])
        save new Profile(scientificName: "ac", opus: opus, classification: [new Classification(rank: "kingdom", name: "plantae")])
        Profile profile3 = save new Profile(scientificName: "aa", opus: opus, classification: [new Classification(rank: "kingdom", name: "plantae")])
        save new Profile(scientificName: "ad", opus: opus, classification: [new Classification(rank: "kingdom", name: "plantae")])

        when:
        List result = service.findByScientificName("a", [opus.uuid], true, 1)

        then:
        result == [profile3]

        when:
        result = service.findByScientificName("a", [opus.uuid], true, 2)

        then:
        result == [profile3, profile1]
    }

    def "findByScientificName should start the result set at the specified offset"() {
        given:
        Opus opus = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1234", title: "title")

        Profile profile1 = save new Profile(scientificName: "ab", opus: opus, classification: [new Classification(rank: "kingdom", name: "plantae")])
        Profile profile2 = save new Profile(scientificName: "ac", opus: opus, classification: [new Classification(rank: "kingdom", name: "plantae")])
        save new Profile(scientificName: "aa", opus: opus, classification: [new Classification(rank: "kingdom", name: "plantae")])
        Profile profile4 = save new Profile(scientificName: "ad", opus: opus, classification: [new Classification(rank: "kingdom", name: "plantae")])

        when:
        List result = service.findByScientificName("a", [opus.uuid], true, 1, 1)

        then:
        result == [profile1]

        when:
        result = service.findByScientificName("a", [opus.uuid], true, 2, 2)

        then:
        result == [profile2, profile4]
    }

    def "findByScientificName should perform exclude archived profiles"() {
        given:
        Opus opus1 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1", title: "title1")
        Profile profile1 = save new Profile(scientificName: "name", opus: opus1, classification: [new Classification(rank: "kingdom", name: "Plantae")])
        Profile profile2 = save new Profile(scientificName: "name two", opus: opus1, archivedDate: new Date(), classification: [new Classification(rank: "kingdom", name: "Plantae")])
        Profile profile3 = save new Profile(scientificName: "nameThree", opus: opus1, classification: [new Classification(rank: "kingdom", name: "Plantae")])

        when:
        List result = service.findByScientificName("NAME", [opus1.uuid], true)

        then:
        result.size() == 2
        result.contains(profile1)
        !result.contains(profile2)
        result.contains(profile3)
    }

    def "findByTaxonNameAndLevel should fail when no scientific name is provided"() {
        when:
        service.findByTaxonNameAndLevel(null, "name", ["opus"])

        then:
        thrown IllegalArgumentException

        when:
        service.findByTaxonNameAndLevel("taxon", null, ["opus"])

        then:
        thrown IllegalArgumentException
    }

    def "findByTaxonNameAndLevel should match any opus when no opusId list is provided"() {
        given:
        Opus opus1 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1", title: "title1")
        Opus opus2 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr2", title: "title2")
        Opus opus3 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr3", title: "title3")

        Profile profile1 = save new Profile(scientificName: "name1", opus: opus1, classification: [new Classification(rank: "kingdom", name: "plantae")])
        Profile profile2 = save new Profile(scientificName: "name2", opus: opus2, classification: [new Classification(rank: "kingdom", name: "plantae")])
        Profile profile3 = save new Profile(scientificName: "name3", opus: opus3, classification: [new Classification(rank: "kingdom", name: "plantae")])

        when:
        List result = service.findByTaxonNameAndLevel("kingdom", "plantae", null)

        then:
        result.size() == 3
        result.find { it.scientificName == profile1.scientificName } != null
        result.find { it.scientificName == profile2.scientificName } != null
        result.find { it.scientificName == profile3.scientificName } != null
    }

    def "findByTaxonNameAndLevel should exclude archived profiles"() {
        given:
        Opus opus1 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1", title: "title1")
        Opus opus2 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr2", title: "title2")
        Opus opus3 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr3", title: "title3")

        Profile profile1 = save new Profile(scientificName: "name1", opus: opus1, classification: [new Classification(rank: "kingdom", name: "plantae")])
        Profile profile2 = save new Profile(scientificName: "name2", opus: opus2, archivedDate: new Date(), classification: [new Classification(rank: "kingdom", name: "plantae")])
        Profile profile3 = save new Profile(scientificName: "name3", opus: opus3, classification: [new Classification(rank: "kingdom", name: "plantae")])

        when:
        List result = service.findByTaxonNameAndLevel("kingdom", "plantae", null)

        then:
        result.size() == 2
        result.find { it.scientificName == profile1.scientificName } != null
        result.find { it.scientificName == profile2.scientificName } == null
        result.find { it.scientificName == profile3.scientificName } != null
    }

    def "findByTaxonNameAndLevel should match only the specified opus ids"() {
        given:
        Opus opus1 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1", title: "title1")
        Opus opus2 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr2", title: "title2")
        Opus opus3 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr3", title: "title3")

        Profile profile1 = save new Profile(scientificName: "name1", opus: opus1, classification: [new Classification(rank: "kingdom", name: "plantae")])
        Profile profile2 = save new Profile(scientificName: "name2", opus: opus2, classification: [new Classification(rank: "kingdom", name: "plantae")])
        Profile profile3 = save new Profile(scientificName: "name3", opus: opus3, classification: [new Classification(rank: "kingdom", name: "plantae")])

        when:
        List result = service.findByTaxonNameAndLevel("kingdom", "plantae", [opus1.uuid, opus3.uuid])

        then:
        result.size() == 2
        result.find { it.scientificName == profile1.scientificName } != null
        result.find { it.scientificName == profile2.scientificName } == null
        result.find { it.scientificName == profile3.scientificName } != null
    }

    def "findByTaxonNameAndLevel should recognise 'unknown' classifications"() {
        given:
        Opus opus1 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1", title: "title1")
        Opus opus2 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr2", title: "title2")
        Opus opus3 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr3", title: "title3")

        Profile profile1 = save new Profile(scientificName: "name1", opus: opus1, classification: [new Classification(rank: "kingdom", name: "plantae")])
        Profile profile2 = save new Profile(scientificName: "name2", opus: opus2)
        Profile profile3 = save new Profile(scientificName: "name3", opus: opus3)

        when:
        List result = service.findByTaxonNameAndLevel(SearchService.UNKNOWN_RANK, "plantae", [opus1.uuid, opus2.uuid, opus3.uuid])

        then:
        result.size() == 2
        result.find { it.scientificName == profile1.scientificName } == null
        result.find { it.scientificName == profile2.scientificName } != null
        result.find { it.scientificName == profile3.scientificName } != null
    }

    def "findByTaxonNameAndLevel should ignore unknown opus ids"() {
        given:
        Opus opus1 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1", title: "title1")
        Opus opus2 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr2", title: "title2")
        Opus opus3 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr3", title: "title3")

        Profile profile1 = save new Profile(scientificName: "name1", opus: opus1, classification: [new Classification(rank: "kingdom", name: "plantae")])
        Profile profile2 = save new Profile(scientificName: "name2", opus: opus2, classification: [new Classification(rank: "kingdom", name: "plantae")])
        Profile profile3 = save new Profile(scientificName: "name3", opus: opus3, classification: [new Classification(rank: "kingdom", name: "plantae")])

        when:
        List result = service.findByTaxonNameAndLevel("kingdom", "plantae", [opus1.uuid, "unknown"])

        then:
        result.size() == 1
        result.find { it.scientificName == profile1.scientificName } != null
        result.find { it.scientificName == profile2.scientificName } == null
        result.find { it.scientificName == profile3.scientificName } == null
    }

    def "findByTaxonNameAndLevel should perform exact match (case-insensitive)"() {
        given:
        Opus opus1 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1", title: "title1")
        Profile profile1 = save new Profile(scientificName: "name1", opus: opus1, classification: [new Classification(rank: "kingdom", name: "plANtae")])
        Profile profile2 = save new Profile(scientificName: "name2", opus: opus1, classification: [new Classification(rank: "kingdom", name: "fungi")])
        Profile profile3 = save new Profile(scientificName: "name3", opus: opus1, classification: [new Classification(rank: "kingdom", name: "Animalae")])

        when:
        List result = service.findByTaxonNameAndLevel("kingdom", "PLANTAE", [opus1.uuid])

        then:
        result.size() == 1
        result.find { it.scientificName == profile1.scientificName } != null
        result.find { it.scientificName == profile2.scientificName } == null
        result.find { it.scientificName == profile3.scientificName } == null
    }

    def "findByTaxonNameAndLevel should limit the results to the specified maximum (names sorted alphabetically"() {
        given:
        Opus opus = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1234", title: "title")

        Profile profile1 = save new Profile(scientificName: "ab", opus: opus, classification: [new Classification(rank: "kingdom", name: "aa")])
        save new Profile(scientificName: "ac", opus: opus, classification: [new Classification(rank: "kingdom", name: "ac")])
        Profile profile3 = save new Profile(scientificName: "aa", opus: opus, classification: [new Classification(rank: "kingdom", name: "aa")])
        save new Profile(scientificName: "ad", opus: opus, classification: [new Classification(rank: "kingdom", name: "ad")])

        when:
        List result = service.findByTaxonNameAndLevel("kingdom", "aa", [opus.uuid], 1)

        then:
        result[0].scientificName == profile3.scientificName

        when:
        result = service.findByTaxonNameAndLevel("kingdom", "aa", [opus.uuid], 2)

        then:
        result.size() == 2
        result.find { it.scientificName == profile1.scientificName } != null
        result.find { it.scientificName == profile3.scientificName } != null
    }

    def "findByTaxonNameAndLevel should start the result set at the specified offset, sorted taxonomically"() {
        given:
        Opus opus = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1234", title: "title")

        Profile profile1 = save new Profile(scientificName: "ab", opus: opus, classification: [new Classification(rank: "kingdom", name: "a")])
        Profile profile2 = save new Profile(scientificName: "ac", opus: opus, classification: [new Classification(rank: "kingdom", name: "a")])
        Profile profile3 = save new Profile(scientificName: "aa", opus: opus, classification: [new Classification(rank: "kingdom", name: "a")])
        Profile profile4 = save new Profile(scientificName: "ad", opus: opus, classification: [new Classification(rank: "kingdom", name: "a")])

        when:
        List result = service.findByTaxonNameAndLevel("kingdom", "a", [opus.uuid], 1, 1)

        then:
        result[0].scientificName == profile1.scientificName

        when:
        result = service.findByTaxonNameAndLevel("kingdom", "a", [opus.uuid], 2, 2)

        then: "the results should be [aa, ab] (i.e. [profile3, profile1]"
        result.size() == 2
        result.find { it.scientificName == profile1.scientificName } == null
        result.find { it.scientificName == profile2.scientificName } != null
        result.find { it.scientificName == profile3.scientificName } == null
        result.find { it.scientificName == profile4.scientificName } != null
    }

    def "findByTaxonNameAndLevel should sort the results by taxonomic hierarchy"() {
        Opus opus = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1234", title: "title")

        save new Profile(scientificName: "Trimeniaceae", opus: opus, classification: [new Classification(rank: "kingdom", name: "plantae"),
                                                                                      new Classification(rank: "phylum", name: "Charophyta"),
                                                                                      new Classification(rank: "class", name: "Equisetopsida"),
                                                                                      new Classification(rank: "subclass", name: "Magnoliidae"),
                                                                                      new Classification(rank: "superorder", name: "Austrobaileyanae"),
                                                                                      new Classification(rank: "order", name: "Austrobaileyales"),
                                                                                      new Classification(rank: "family", name: "Trimeniaceae")])

        save new Profile(scientificName: "Trimenia", opus: opus, classification: [new Classification(rank: "kingdom", name: "plantae"),
                                                                                  new Classification(rank: "phylum", name: "Charophyta"),
                                                                                  new Classification(rank: "class", name: "Equisetopsida"),
                                                                                  new Classification(rank: "subclass", name: "Magnoliidae"),
                                                                                  new Classification(rank: "superorder", name: "Austrobaileyanae"),
                                                                                  new Classification(rank: "order", name: "Austrobaileyales"),
                                                                                  new Classification(rank: "family", name: "Trimeniaceae"),
                                                                                  new Classification(rank: "genus", name: "Trimenia")])

        save new Profile(scientificName: "Trimenia moorei", opus: opus, classification: [new Classification(rank: "kingdom", name: "plantae"),
                                                                                         new Classification(rank: "phylum", name: "Charophyta"),
                                                                                         new Classification(rank: "class", name: "Equisetopsida"),
                                                                                         new Classification(rank: "subclass", name: "Magnoliidae"),
                                                                                         new Classification(rank: "superorder", name: "Austrobaileyanae"),
                                                                                         new Classification(rank: "order", name: "Austrobaileyales"),
                                                                                         new Classification(rank: "family", name: "Trimeniaceae"),
                                                                                         new Classification(rank: "genus", name: "Trimenia"),
                                                                                         new Classification(rank: "species", name: "Trimenia moorei")])

        save new Profile(scientificName: "Austrobaileyaceae", opus: opus, classification: [new Classification(rank: "kingdom", name: "plantae"),
                                                                                           new Classification(rank: "phylum", name: "Charophyta"),
                                                                                           new Classification(rank: "class", name: "Equisetopsida"),
                                                                                           new Classification(rank: "subclass", name: "Magnoliidae"),
                                                                                           new Classification(rank: "superorder", name: "Austrobaileyanae"),
                                                                                           new Classification(rank: "order", name: "Austrobaileyales"),
                                                                                           new Classification(rank: "family", name: "Austrobaileyaceae")])

        save new Profile(scientificName: "Austrobaileya", opus: opus, classification: [new Classification(rank: "kingdom", name: "plantae"),
                                                                                       new Classification(rank: "phylum", name: "Charophyta"),
                                                                                       new Classification(rank: "class", name: "Equisetopsida"),
                                                                                       new Classification(rank: "subclass", name: "Magnoliidae"),
                                                                                       new Classification(rank: "superorder", name: "Austrobaileyanae"),
                                                                                       new Classification(rank: "order", name: "Austrobaileyales"),
                                                                                       new Classification(rank: "family", name: "Austrobaileyaceae"),
                                                                                       new Classification(rank: "genus", name: "Austrobaileya")])

        save new Profile(scientificName: "Austrobaileya scandens", opus: opus, classification: [new Classification(rank: "kingdom", name: "plantae"),
                                                                                                new Classification(rank: "phylum", name: "Charophyta"),
                                                                                                new Classification(rank: "class", name: "Equisetopsida"),
                                                                                                new Classification(rank: "subclass", name: "Magnoliidae"),
                                                                                                new Classification(rank: "superorder", name: "Austrobaileyanae"),
                                                                                                new Classification(rank: "order", name: "Austrobaileyales"),
                                                                                                new Classification(rank: "family", name: "Austrobaileyaceae"),
                                                                                                new Classification(rank: "genus", name: "Austrobaileya"),
                                                                                                new Classification(rank: "species", name: "Austrobaileya scandens")])

        when:
        def result = service.findByTaxonNameAndLevel("order", "Austrobaileyales", [opus.uuid])

        then:
        result.size() == 6
        result[0].scientificName == "Austrobaileyaceae"
        result[1].scientificName == "Austrobaileya"
        result[2].scientificName == "Austrobaileya scandens"
        result[3].scientificName == "Trimeniaceae"
        result[4].scientificName == "Trimenia"
        result[5].scientificName == "Trimenia moorei"
    }

    def "findByTaxonNameAndLevel should sort the results alphabetically within the same rank"() {
        Opus opus = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1234", title: "title")

        save new Profile(scientificName: "b", opus: opus, classification: [new Classification(rank: "kingdom", name: "plantae"),
                                                                           new Classification(rank: "phylum", name: "Charophyta"),
                                                                           new Classification(rank: "class", name: "Equisetopsida"),
                                                                           new Classification(rank: "subclass", name: "Magnoliidae"),
                                                                           new Classification(rank: "superorder", name: "Austrobaileyanae"),
                                                                           new Classification(rank: "order", name: "Austrobaileyales"),
                                                                           new Classification(rank: "family", name: "Trimeniaceae")])

        save new Profile(scientificName: "a", opus: opus, classification: [new Classification(rank: "kingdom", name: "plantae"),
                                                                           new Classification(rank: "phylum", name: "Charophyta"),
                                                                           new Classification(rank: "class", name: "Equisetopsida"),
                                                                           new Classification(rank: "subclass", name: "Magnoliidae"),
                                                                           new Classification(rank: "superorder", name: "Austrobaileyanae"),
                                                                           new Classification(rank: "order", name: "Austrobaileyales"),
                                                                           new Classification(rank: "family", name: "Trimeniaceae")])

        save new Profile(scientificName: "c", opus: opus, classification: [new Classification(rank: "kingdom", name: "plantae"),
                                                                           new Classification(rank: "phylum", name: "Charophyta"),
                                                                           new Classification(rank: "class", name: "Equisetopsida"),
                                                                           new Classification(rank: "subclass", name: "Magnoliidae"),
                                                                           new Classification(rank: "superorder", name: "Austrobaileyanae"),
                                                                           new Classification(rank: "order", name: "Austrobaileyales"),
                                                                           new Classification(rank: "family", name: "Trimeniaceae")])

        when:
        def result = service.findByTaxonNameAndLevel("family", "Trimeniaceae", [opus.uuid])

        then:
        result.size() == 3
        result[0].scientificName == "a"
        result[1].scientificName == "b"
        result[2].scientificName == "c"
    }

    def "getTaxonLevels should fail if no opus id is provided"() {
        when:
        service.getTaxonLevels(null)

        then:
        thrown IllegalArgumentException
    }

    def "getTaxonLevels should return an empty Map the opus does not exist"() {
        when:
        Map result = service.getTaxonLevels("unknown")

        then:
        result == [:]
    }

    def "getTaxonLevels should group and count all unique classification levels, including unknown classifications"() {
        given:
        Opus opus = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1234", title: "title")

        save new Profile(scientificName: "unknown1", opus: opus)
        save new Profile(scientificName: "unknown2", opus: opus)

        save new Profile(scientificName: "kingdom1", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae")])
        save new Profile(scientificName: "kingdom2", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae")])
        save new Profile(scientificName: "kingdom3", opus: opus, classification: [new Classification(rank: "kingdom", name: "Animalia")])

        save new Profile(scientificName: "phylum1", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                 new Classification(rank: "phylum", name: "Charophyta")])
        save new Profile(scientificName: "phylum2", opus: opus, classification: [new Classification(rank: "kingdom", name: "Animalia"),
                                                                                 new Classification(rank: "phylum", name: "Hygrophila")])
        save new Profile(scientificName: "phylum3", opus: opus, classification: [new Classification(rank: "kingdom", name: "Animalia"),
                                                                                 new Classification(rank: "phylum", name: "Arthropoda")])

        save new Profile(scientificName: "class1", opus: opus, classification: [new Classification(kingdom: "Plantae"),
                                                                                new Classification(rank: "phylum", name: "Charophyta"),
                                                                                new Classification(rank: "class", name: "Equisetopsida")])

        save new Profile(scientificName: "subclass1", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                   new Classification(rank: "phylum", name: "Charophyta"),
                                                                                   new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                   new Classification(rank: "subclazz", name: "Cycadidae")])
        save new Profile(scientificName: "subclass2", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                   new Classification(rank: "phylum", name: "Charophyta"),
                                                                                   new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                   new Classification(rank: "subclazz", name: "Magnoliidae")])
        save new Profile(scientificName: "subclass3", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                   new Classification(rank: "phylum", name: "Charophyta"),
                                                                                   new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                   new Classification(rank: "subclazz", name: "Marattiidae")])
        save new Profile(scientificName: "subclass4", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                   new Classification(rank: "phylum", name: "Charophyta"),
                                                                                   new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                   new Classification(rank: "subclazz", name: "Ophioglossidae")])

        save new Profile(scientificName: "order1", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                new Classification(rank: "phylum", name: "Charophyta"),
                                                                                new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                new Classification(rank: "subclazz", name: "Ophioglossidae"),
                                                                                new Classification(rank: "order", name: "Apiales")])
        save new Profile(scientificName: "order2", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                new Classification(rank: "phylum", name: "Charophyta"),
                                                                                new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                new Classification(rank: "subclazz", name: "Ophioglossidae"),
                                                                                new Classification(rank: "order", name: "Arecales")])

        save new Profile(scientificName: "family1", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                 new Classification(rank: "phylum", name: "Charophyta"),
                                                                                 new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                 new Classification(rank: "subclazz", name: "Ophioglossidae"),
                                                                                 new Classification(rank: "order", name: "Apiales"),
                                                                                 new Classification(rank: "family", name: "Aphididae")])
        save new Profile(scientificName: "family2", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                 new Classification(rank: "phylum", name: "Charophyta"),
                                                                                 new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                 new Classification(rank: "subclazz", name: "Ophioglossidae"),
                                                                                 new Classification(rank: "order", name: "Apiales"),
                                                                                 new Classification(rank: "family", name: "Acanthaceae")])
        save new Profile(scientificName: "family3", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                 new Classification(rank: "phylum", name: "Charophyta"),
                                                                                 new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                 new Classification(rank: "subclazz", name: "Ophioglossidae"),
                                                                                 new Classification(rank: "order", name: "Apiales"),
                                                                                 new Classification(rank: "family", name: "Adoxaceae")])
        save new Profile(scientificName: "family4", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                 new Classification(rank: "phylum", name: "Charophyta"),
                                                                                 new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                 new Classification(rank: "subclazz", name: "Ophioglossidae"),
                                                                                 new Classification(rank: "order", name: "Apiales"),
                                                                                 new Classification(rank: "family", name: "Agapanthaceae")])

        save new Profile(scientificName: "genus1", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                new Classification(rank: "phylum", name: "Charophyta"),
                                                                                new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                new Classification(rank: "subclazz", name: "Ophioglossidae"),
                                                                                new Classification(rank: "order", name: "Apiales"),
                                                                                new Classification(rank: "family", name: "Agapanthaceae"),
                                                                                new Classification(rank: "genus", name: "Abildgaardia")])
        save new Profile(scientificName: "genus2", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                new Classification(rank: "phylum", name: "Charophyta"),
                                                                                new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                new Classification(rank: "subclazz", name: "Ophioglossidae"),
                                                                                new Classification(rank: "order", name: "Asterales"),
                                                                                new Classification(rank: "family", name: "Agapanthaceae"),
                                                                                new Classification(rank: "genus", name: "Abrophyllum")])

        save new Profile(scientificName: "species1", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                  new Classification(rank: "phylum", name: "Charophyta"),
                                                                                  new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                  new Classification(rank: "subclazz", name: "Ophioglossidae"),
                                                                                  new Classification(rank: "order", name: "Agapanthaceae"),
                                                                                  new Classification(rank: "genus", name: "Abrophyllum"),
                                                                                  new Classification(rank: "species", name: "bla")])


        when:
        Map levels = service.getTaxonLevels(opus.uuid)

        then:
        levels.kingdom == 2
        levels.phylum == 3
        levels.clazz == 1
        levels.subclazz == 4
        levels.order == 4
        levels.family == 4
        levels.genus == 2
        levels[SearchService.UNKNOWN_RANK] == 2
    }

    def "groupByTaxonLevel should fail if no opusId or taxon are provided"() {
        when:
        service.groupByTaxonLevel(null, "taxon")

        then:
        thrown IllegalArgumentException

        when:
        service.groupByTaxonLevel("opus", null)

        then:
        thrown IllegalArgumentException
    }

    def "groupByTaxonLevel should return an empty map when the opus does not exist"() {
        when:
        Map result = service.groupByTaxonLevel("unknown", "kingdom")

        then:
        result == [:]
    }

    def "groupByTaxonLevel should return a count of profiles with no rank/classification when the requested rank is 'unknown'"() {
        given:
        Opus opus = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1234", title: "title")

        save new Profile(scientificName: "kingdom1", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae")])
        save new Profile(scientificName: "kingdom2", opus: opus)
        save new Profile(scientificName: "kingdom3", opus: opus)

        when:
        Map result = service.groupByTaxonLevel(opus.uuid, SearchService.UNKNOWN_RANK)

        then:
        result == [(SearchService.UNKNOWN_RANK): 2]

    }

    def "groupByTaxonLevel should return a map of names of the requested level, with their counts"() {
        given:
        Opus opus = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1234", title: "title")

        save new Profile(scientificName: "kingdom1", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae")])
        save new Profile(scientificName: "kingdom2", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae")])
        save new Profile(scientificName: "kingdom3", opus: opus, classification: [new Classification(rank: "kingdom", name: "Animalia")])

        save new Profile(scientificName: "phylum1", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                 new Classification(rank: "phylum", name: "Charophyta")])
        save new Profile(scientificName: "phylum2", opus: opus, classification: [new Classification(rank: "kingdom", name: "Animalia"),
                                                                                 new Classification(rank: "phylum", name: "Hygrophila")])
        save new Profile(scientificName: "phylum3", opus: opus, classification: [new Classification(rank: "kingdom", name: "Animalia"),
                                                                                 new Classification(rank: "phylum", name: "Arthropoda")])

        save new Profile(scientificName: "class1", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                new Classification(rank: "phylum", name: "Charophyta"),
                                                                                new Classification(rank: "class", name: "Equisetopsida")])

        save new Profile(scientificName: "subclass1", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                   new Classification(rank: "phylum", name: "Charophyta"),
                                                                                   new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                   new Classification(rank: "subclazz", name: "Cycadidae")])
        save new Profile(scientificName: "subclass2", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                   new Classification(rank: "phylum", name: "Charophyta"),
                                                                                   new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                   new Classification(rank: "subclazz", name: "Magnoliidae")])
        save new Profile(scientificName: "subclass3", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                   new Classification(rank: "phylum", name: "Charophyta"),
                                                                                   new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                   new Classification(rank: "subclazz", name: "Marattiidae")])
        save new Profile(scientificName: "subclass4", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                   new Classification(rank: "phylum", name: "Charophyta"),
                                                                                   new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                   new Classification(rank: "subclazz", name: "Ophioglossidae")])

        save new Profile(scientificName: "order1", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                new Classification(rank: "phylum", name: "Charophyta"),
                                                                                new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                new Classification(rank: "subclazz", name: "Ophioglossidae"),
                                                                                new Classification(rank: "order", name: "Apiales")])
        save new Profile(scientificName: "order2", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                new Classification(rank: "phylum", name: "Charophyta"),
                                                                                new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                new Classification(rank: "subclazz", name: "Ophioglossidae"),
                                                                                new Classification(rank: "order", name: "Arecales")])

        save new Profile(scientificName: "family1", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                 new Classification(rank: "phylum", name: "Charophyta"),
                                                                                 new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                 new Classification(rank: "subclazz", name: "Ophioglossidae"),
                                                                                 new Classification(rank: "order", name: "Apiales"),
                                                                                 new Classification(rank: "family", name: "Aphididae")])
        save new Profile(scientificName: "family2", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                 new Classification(rank: "phylum", name: "Charophyta"),
                                                                                 new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                 new Classification(rank: "subclazz", name: "Ophioglossidae"),
                                                                                 new Classification(rank: "order", name: "Apiales"),
                                                                                 new Classification(rank: "family", name: "Acanthaceae")])
        save new Profile(scientificName: "family3", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                 new Classification(rank: "phylum", name: "Charophyta"),
                                                                                 new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                 new Classification(rank: "subclazz", name: "Ophioglossidae"),
                                                                                 new Classification(rank: "order", name: "Apiales"),
                                                                                 new Classification(rank: "family", name: "Adoxaceae")])
        save new Profile(scientificName: "family4", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                 new Classification(rank: "phylum", name: "Charophyta"),
                                                                                 new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                 new Classification(rank: "subclazz", name: "Ophioglossidae"),
                                                                                 new Classification(rank: "order", name: "Apiales"),
                                                                                 new Classification(rank: "family", name: "Agapanthaceae")])

        save new Profile(scientificName: "genus1", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                new Classification(rank: "phylum", name: "Charophyta"),
                                                                                new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                new Classification(rank: "subclazz", name: "Ophioglossidae"),
                                                                                new Classification(rank: "order", name: "Apiales"),
                                                                                new Classification(rank: "family", name: "Agapanthaceae"),
                                                                                new Classification(rank: "genus", name: "Abildgaardia")])
        save new Profile(scientificName: "genus2", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                new Classification(rank: "phylum", name: "Charophyta"),
                                                                                new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                new Classification(rank: "subclazz", name: "Ophioglossidae"),
                                                                                new Classification(rank: "order", name: "Asterales"),
                                                                                new Classification(rank: "family", name: "Agapanthaceae"),
                                                                                new Classification(rank: "genus", name: "Abrophyllum")])

        save new Profile(scientificName: "species1", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                  new Classification(rank: "phylum", name: "Charophyta"),
                                                                                  new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                  new Classification(rank: "subclazz", name: "Ophioglossidae"),
                                                                                  new Classification(rank: "order", name: "Agapanthaceae"),
                                                                                  new Classification(rank: "genus", name: "Abrophyllum"),
                                                                                  new Classification(rank: "species", name: "bla")])


        when:
        Map result = service.groupByTaxonLevel(opus.uuid, "kingdom")

        then:
        result == [Animalia: 3, Plantae: 17]

        when:
        result = service.groupByTaxonLevel(opus.uuid, "phylum")

        then:
        result == [Charophyta: 15, Hygrophila: 1, Arthropoda: 1]

        when:
        result = service.groupByTaxonLevel(opus.uuid, "genus")

        then:
        result == [Abildgaardia: 1, Abrophyllum: 2]
    }

    def "groupByTaxonLevel should limit the results to the specified maximum (names sorted alphabetically"() {
        given:
        Opus opus = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1234", title: "title")

        save new Profile(scientificName: "subclass1", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                   new Classification(rank: "phylum", name: "Charophyta"),
                                                                                   new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                   new Classification(rank: "subclazz", name: "Cycadidae")])
        save new Profile(scientificName: "subclass2", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                   new Classification(rank: "phylum", name: "Charophyta"),
                                                                                   new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                   new Classification(rank: "subclazz", name: "Magnoliidae")])
        save new Profile(scientificName: "subclass3", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                   new Classification(rank: "phylum", name: "Arthropoda"),
                                                                                   new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                   new Classification(rank: "subclazz", name: "Marattiidae")])
        save new Profile(scientificName: "subclass4", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                   new Classification(rank: "phylum", name: "Hygrophila"),
                                                                                   new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                   new Classification(rank: "subclazz", name: "Ophioglossidae")])

        when:
        Map result = service.groupByTaxonLevel(opus.uuid, "phylum", 1)

        then:
        result == [Arthropoda: 1]

        when:
        result = service.groupByTaxonLevel(opus.uuid, "phylum", 2)

        then:
        result == [Arthropoda: 1, Charophyta: 2]
    }

    def "groupByTaxonLevel should start the result set at the specified offset"() {
        given:
        Opus opus = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1234", title: "title")

        save new Profile(scientificName: "subclass1", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                   new Classification(rank: "phylum", name: "Charophyta"),
                                                                                   new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                   new Classification(rank: "subclazz", name: "Cycadidae")])
        save new Profile(scientificName: "subclass2", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                   new Classification(rank: "phylum", name: "Charophyta"),
                                                                                   new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                   new Classification(rank: "subclazz", name: "Magnoliidae")])
        save new Profile(scientificName: "subclass3", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                   new Classification(rank: "phylum", name: "Arthropoda"),
                                                                                   new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                   new Classification(rank: "subclazz", name: "Marattiidae")])
        save new Profile(scientificName: "subclass4", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                   new Classification(rank: "phylum", name: "Hygrophila"),
                                                                                   new Classification(rank: "clazz", name: "Equisetopsida"),
                                                                                   new Classification(rank: "subclazz", name: "Ophioglossidae")])

        when:
        Map result = service.groupByTaxonLevel(opus.uuid, "phylum", 1, 1)

        then:
        result == [Charophyta: 2]

        when:
        result = service.groupByTaxonLevel(opus.uuid, "phylum", 2, 2)

        then:
        result == [Hygrophila: 1]
    }

    def "getImmediateChildren should list the next rank after the specified for all profiles"() {
        given:
        Opus opus = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1234", title: "title")

        save new Profile(scientificName: "profile1", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                  new Classification(rank: "phylum", name: "Charophyta"),
                                                                                  new Classification(rank: "class", name: "Equisetopsida"),
                                                                                  new Classification(rank: "subclass", name: "Cycadidae"),
                                                                                  new Classification(rank: "order", name: "Cycadales")])

        save new Profile(scientificName: "profile2", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                  new Classification(rank: "phylum", name: "Charophyta"),
                                                                                  new Classification(rank: "class", name: "Equisetopsida"),
                                                                                  new Classification(rank: "subclass", name: "Lycopodiidae"),
                                                                                  new Classification(rank: "order", name: "Isoetales"),
                                                                                  new Classification(rank: "family", name: "Isoetaceae")])

        save new Profile(scientificName: "profile3", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                  new Classification(rank: "phylum", name: "Arthropoda"),
                                                                                  new Classification(rank: "class", name: "Equisetopsida"),
                                                                                  new Classification(rank: "subclass", name: "Lycopodiidae"),
                                                                                  new Classification(rank: "order", name: "Lycopodiales")])

        save new Profile(scientificName: "profile4", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                  new Classification(rank: "phylum", name: "Hygrophila"),
                                                                                  new Classification(rank: "class", name: "Equisetopsida"),
                                                                                  new Classification(rank: "subclass", name: "Magnoliidae"),
                                                                                  new Classification(rank: "order", name: "Fabales")])

        when: "asked for the children of subclass Lycopodiidae"
        List result = service.getImmediateChildren(opus, "subCLass", "LycoPODiidAE") // matches should be case insensitive

        then: "it should return the 2 orders directly below Lycopodiidae (in alphabetic order), but not the family below Lycopodiidae"
        result.size() == 2
        result[0].name == "Isoetales"
        result[0].rank == "order"
        result[1].name == "Lycopodiales"
        result[1].rank == "order"
    }

    def "getImmediateChildren should page the results"() {
        given:
        Opus opus = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1234", title: "title")

        save new Profile(scientificName: "profile1", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                  new Classification(rank: "phylum", name: "Charophyta"),
                                                                                  new Classification(rank: "class", name: "Equisetopsida"),
                                                                                  new Classification(rank: "subclass", name: "Cycadidae"),
                                                                                  new Classification(rank: "order", name: "Cycadales")])

        save new Profile(scientificName: "profile2", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                  new Classification(rank: "phylum", name: "Charophyta"),
                                                                                  new Classification(rank: "class", name: "Equisetopsida"),
                                                                                  new Classification(rank: "subclass", name: "Lycopodiidae"),
                                                                                  new Classification(rank: "order", name: "Isoetales"),
                                                                                  new Classification(rank: "family", name: "Isoetaceae")])

        save new Profile(scientificName: "profile3", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                  new Classification(rank: "phylum", name: "Arthropoda"),
                                                                                  new Classification(rank: "class", name: "Equisetopsida"),
                                                                                  new Classification(rank: "subclass", name: "Lycopodiidae"),
                                                                                  new Classification(rank: "order", name: "Lycopodiales")])

        save new Profile(scientificName: "profile4", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                  new Classification(rank: "phylum", name: "Hygrophila"),
                                                                                  new Classification(rank: "class", name: "Equisetopsida"),
                                                                                  new Classification(rank: "subclass", name: "Magnoliidae"),
                                                                                  new Classification(rank: "order", name: "Fabales")])

        when: "asked for the children of subclass Lycopodiidae, with offset 1 and page size 1"
        List result = service.getImmediateChildren(opus, "subclass", "Lycopodiidae", 1, 1)

        then: "it should return the 2nd of the 2 orders directly below Lycopodiida"
        result.size() == 1
        result[0].name == "Lycopodiales"
        result[0].rank == "order"
    }

    def "getImmediateChildren should handle mixed ranks below the specified rank"() {
        given:
        Opus opus = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1234", title: "title")

        save new Profile(scientificName: "profile1", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                  new Classification(rank: "phylum", name: "Charophyta"),
                                                                                  new Classification(rank: "class", name: "Equisetopsida"),
                                                                                  new Classification(rank: "subclass", name: "Cycadidae"),
                                                                                  new Classification(rank: "order", name: "Cycadales")])

        save new Profile(scientificName: "profile2", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                  new Classification(rank: "phylum", name: "Charophyta"),
                                                                                  new Classification(rank: "class", name: "Equisetopsida"),
                                                                                  new Classification(rank: "subclass", name: "Magnoliidae"),
                                                                                  new Classification(rank: "order", name: "Ranunculales")])

        save new Profile(scientificName: "profile3", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                  new Classification(rank: "phylum", name: "Charophyta"),
                                                                                  new Classification(rank: "class", name: "Equisetopsida"),
                                                                                  new Classification(rank: "subclass", name: "Magnoliidae"),
                                                                                  new Classification(rank: "superorder", name: "Asteranae")])

        save new Profile(scientificName: "profile4", opus: opus, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                  new Classification(rank: "phylum", name: "Charophyta"),
                                                                                  new Classification(rank: "class", name: "Equisetopsida"),
                                                                                  new Classification(rank: "subclass", name: "Magnoliidae"),
                                                                                  new Classification(rank: "order", name: "Fabales")])

        when: "asked for the children of subclass Magnoliidae"
        List result = service.getImmediateChildren(opus, "subclass", "Magnoliidae")

        then: "it should return the superorder Asteranae, and the two orders Fabales and Ranunculales, sorted by rank order then name alphabetically"
        result.size() == 3
        result[0].name == "Asteranae"
        result[0].rank == "superorder" // superorder is higher up the taxonomy than order, so must come first
        result[1].name == "Fabales"
        result[1].rank == "order"
        result[2].name == "Ranunculales"
        result[2].rank == "order"
    }

    def "getImmediateChildren should only return results from the specified Opus"() {
        given:
        Opus opus1 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1234", title: "title1")
        Opus opus2 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1234", title: "title2")

        save new Profile(scientificName: "profile1", opus: opus1, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                   new Classification(rank: "phylum", name: "Charophyta"),
                                                                                   new Classification(rank: "class", name: "Equisetopsida"),
                                                                                   new Classification(rank: "subclass", name: "Cycadidae"),
                                                                                   new Classification(rank: "order", name: "Cycadales")])

        save new Profile(scientificName: "profile1", opus: opus2, classification: [new Classification(rank: "kingdom", name: "Plantae"),
                                                                                   new Classification(rank: "phylum", name: "Charophyta"),
                                                                                   new Classification(rank: "class", name: "Equisetopsida"),
                                                                                   new Classification(rank: "subclass", name: "Magnoliidae"),
                                                                                   new Classification(rank: "order", name: "Ranunculales")])

        when: "asked for the children of class Equisetopsida in Opus1"
        List result = service.getImmediateChildren(opus1, "class", "Equisetopsida")

        then: "it should return not return Magnoliidae from Opus2"
        result.size() == 1
        result[0].name == "Cycadidae"
        result[0].rank == "subclass"
    }

}
