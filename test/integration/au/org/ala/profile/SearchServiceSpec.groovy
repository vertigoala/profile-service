package au.org.ala.profile

import au.org.ala.web.AuthService


class SearchServiceSpec extends BaseIntegrationSpec {
    SearchService service = new SearchService()

    def "setup"() {
        service.authService = Mock(AuthService)
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
        service.authService.getUserId() >> null

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
        service.authService.getUserId() >> "1234"

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
        service.authService.getUserId() >> "1234"

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

        Profile profile1 = save new Profile(scientificName: "ab", opus: opus, classification: [new Classification(rank: "kingdom", name: "kingdom")])
        save new Profile(scientificName: "ac", opus: opus, classification: [new Classification(rank: "kingdom", name: "kingdom")])
        Profile profile3 = save new Profile(scientificName: "aa", opus: opus, classification: [new Classification(rank: "kingdom", name: "kingdom")])
        save new Profile(scientificName: "ad", opus: opus, classification: [new Classification(rank: "kingdom", name: "kingdom")])

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

        Profile profile1 = save new Profile(scientificName: "ab", opus: opus, classification: [new Classification(rank: "kingdom", name: "kingdom")])
        Profile profile2 = save new Profile(scientificName: "ac", opus: opus, classification: [new Classification(rank: "kingdom", name: "kingdom")])
        save new Profile(scientificName: "aa", opus: opus, classification: [new Classification(rank: "kingdom", name: "kingdom")])
        Profile profile4 = save new Profile(scientificName: "ad", opus: opus, classification: [new Classification(rank: "kingdom", name: "kingdom")])

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

        Profile profile1 = save new Profile(scientificName: "name", opus: opus1, classification: [new Classification(rank: "kingdom", name: "kingdom1")])
        Profile profile2 = save new Profile(scientificName: "name", opus: opus2, classification: [new Classification(rank: "kingdom", name: "kingdom2")])
        Profile profile3 = save new Profile(scientificName: "name", opus: opus3, classification: [new Classification(rank: "kingdom", name: "kingdom3")])

        when:
        List result = service.findByTaxonNameAndLevel("kingdom", "kingdom", null)

        then:
        result.size() == 3
        result.contains(profile1)
        result.contains(profile2)
        result.contains(profile3)
    }

    def "findByTaxonNameAndLevel should exclude archived profiles"() {
        given:
        Opus opus1 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1", title: "title1")
        Opus opus2 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr2", title: "title2")
        Opus opus3 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr3", title: "title3")

        Profile profile1 = save new Profile(scientificName: "name", opus: opus1, classification: [new Classification(rank: "kingdom", name: "kingdom1")])
        Profile profile2 = save new Profile(scientificName: "name", opus: opus2, archivedDate: new Date(), classification: [new Classification(rank: "kingdom", name: "kingdom2")])
        Profile profile3 = save new Profile(scientificName: "name", opus: opus3, classification: [new Classification(rank: "kingdom", name: "kingdom3")])

        when:
        List result = service.findByTaxonNameAndLevel("kingdom", "kingdom", null)

        then:
        result.size() == 2
        result.contains(profile1)
        !result.contains(profile2)
        result.contains(profile3)
    }

    def "findByTaxonNameAndLevel should match only the specified opus ids"() {
        given:
        Opus opus1 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1", title: "title1")
        Opus opus2 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr2", title: "title2")
        Opus opus3 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr3", title: "title3")

        Profile profile1 = save new Profile(scientificName: "name", opus: opus1, classification: [new Classification(rank: "kingdom", name: "kingdom1")])
        Profile profile2 = save new Profile(scientificName: "name", opus: opus2, classification: [new Classification(rank: "kingdom", name: "kingdom2")])
        Profile profile3 = save new Profile(scientificName: "name", opus: opus3, classification: [new Classification(rank: "kingdom", name: "kingdom3")])

        when:
        List result = service.findByTaxonNameAndLevel("kingdom", "kingdom", [opus1.uuid, opus3.uuid])

        then:
        result.size() == 2
        result.contains(profile1)
        !result.contains(profile2)
        result.contains(profile3)
    }

    def "findByTaxonNameAndLevel should ignore unknown opus ids"() {
        given:
        Opus opus1 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1", title: "title1")
        Opus opus2 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr2", title: "title2")
        Opus opus3 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr3", title: "title3")

        Profile profile1 = save new Profile(scientificName: "name", opus: opus1, classification: [new Classification(rank: "kingdom", name: "kingdom1")])
        Profile profile2 = save new Profile(scientificName: "name", opus: opus2, classification: [new Classification(rank: "kingdom", name: "kingdom2")])
        Profile profile3 = save new Profile(scientificName: "name", opus: opus3, classification: [new Classification(rank: "kingdom", name: "kingdom3")])

        when:
        List result = service.findByTaxonNameAndLevel("kingdom", "king", [opus1.uuid, "unknown"])

        then:
        result.size() == 1
        result.contains(profile1)
        !result.contains(profile2)
        !result.contains(profile3)
    }

    def "findByTaxonNameAndLevel should default to using a wildcard"() {
        given:
        Opus opus1 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1", title: "title1")
        Profile profile1 = save new Profile(scientificName: "name", opus: opus1, classification: [new Classification(rank: "kingdom", name: "kingdom1")])
        Profile profile2 = save new Profile(scientificName: "name", opus: opus1, classification: [new Classification(rank: "kingdom", name: "kingdom2")])
        Profile profile3 = save new Profile(scientificName: "name", opus: opus1, classification: [new Classification(rank: "kingdom", name: "kingdom3")])

        when:
        List result = service.findByTaxonNameAndLevel("kingdom", "kin", [opus1.uuid])

        then:
        result.size() == 3
        result.contains(profile1)
        result.contains(profile2)
        result.contains(profile3)
    }

    def "findByTaxonNameAndLevel should perform exact match (case-insensitive) when useWildcard is false"() {
        given:
        Opus opus1 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1", title: "title1")
        Profile profile1 = save new Profile(scientificName: "name", opus: opus1, classification: [new Classification(rank: "kingdom", name: "kingdom")])
        Profile profile2 = save new Profile(scientificName: "name", opus: opus1, classification: [new Classification(rank: "kingdom", name: "kingdom2")])
        Profile profile3 = save new Profile(scientificName: "name", opus: opus1, classification: [new Classification(rank: "kingdom", name: "kingdom3")])

        when:
        List result = service.findByTaxonNameAndLevel("kingdom", "KINGDOM", [opus1.uuid], false)

        then:
        result.size() == 1
        result.contains(profile1)
        !result.contains(profile2)
        !result.contains(profile3)
    }

    def "findByTaxonNameAndLevel should limit the results to the specified maximum (names sorted alphabetically"() {
        given:
        Opus opus = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1234", title: "title")

        Profile profile1 = save new Profile(scientificName: "ab", opus: opus, classification: [new Classification(rank: "kingdom", name: "ab")])
        save new Profile(scientificName: "ac", opus: opus, classification: [new Classification(rank: "kingdom", name: "ac")])
        Profile profile3 = save new Profile(scientificName: "aa", opus: opus, classification: [new Classification(rank: "kingdom", name: "aa")])
        save new Profile(scientificName: "ad", opus: opus, classification: [new Classification(rank: "kingdom", name: "ad")])

        when:
        List result = service.findByTaxonNameAndLevel("kingdom", "a", [opus.uuid], true, 1)

        then:
        result == [profile3]

        when:
        result = service.findByTaxonNameAndLevel("kingdom", "a", [opus.uuid], true, 2)

        then:
        result == [profile3, profile1]
    }

    def "findByTaxonNameAndLevel should start the result set at the specified offset"() {
        given:
        Opus opus = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1234", title: "title")

        Profile profile1 = save new Profile(scientificName: "ab", opus: opus, classification: [new Classification(rank: "kingdom", name: "ab")])
        Profile profile2 = save new Profile(scientificName: "ac", opus: opus, classification: [new Classification(rank: "kingdom", name: "ac")])
        save new Profile(scientificName: "aa", opus: opus, classification: [new Classification(rank: "kingdom", name: "aa")])
        Profile profile4 = save new Profile(scientificName: "ad", opus: opus, classification: [new Classification(rank: "kingdom", name: "ad")])

        when:
        List result = service.findByTaxonNameAndLevel("kingdom", "a", [opus.uuid], true, 1, 1)

        then:
        result == [profile1]

        when:
        result = service.findByTaxonNameAndLevel("kingdom", "a", [opus.uuid], true, 2, 2)

        then:
        result == [profile2, profile4]
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

    def "getTaxonLevels should group and count all unique classification levels"() {
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

    def "groupByTaxLevel should start the result set at the specified offset"() {
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

}
