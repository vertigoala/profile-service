package au.org.ala.profile


class SearchServiceSpec extends BaseIntegrationSpec {
    SearchService service = new SearchService()

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

        Profile profile1 = save new Profile(scientificName: "name", opus: opus1, classification: new Classification(kingdom: "Plantae"))
        Profile profile2 = save new Profile(scientificName: "name", opus: opus2, classification: new Classification(kingdom: "Plantae"))
        Profile profile3 = save new Profile(scientificName: "name", opus: opus3, classification: new Classification(kingdom: "Plantae"))

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

        Profile profile1 = save new Profile(scientificName: "name", opus: opus1, classification: new Classification(kingdom: "Plantae"))
        Profile profile2 = save new Profile(scientificName: "name", opus: opus2, classification: new Classification(kingdom: "Plantae"))
        Profile profile3 = save new Profile(scientificName: "name", opus: opus3, classification: new Classification(kingdom: "Plantae"))

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

        Profile profile1 = save new Profile(scientificName: "name", opus: opus1, classification: new Classification(kingdom: "Plantae"))
        Profile profile2 = save new Profile(scientificName: "name", opus: opus2, classification: new Classification(kingdom: "Plantae"))
        Profile profile3 = save new Profile(scientificName: "name", opus: opus3, classification: new Classification(kingdom: "Plantae"))

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
        Profile profile1 = save new Profile(scientificName: "name", opus: opus1, classification: new Classification(kingdom: "Plantae"))
        Profile profile2 = save new Profile(scientificName: "name two", opus: opus1, classification: new Classification(kingdom: "Plantae"))
        Profile profile3 = save new Profile(scientificName: "nameThree", opus: opus1, classification: new Classification(kingdom: "Plantae"))

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
        Profile profile1 = save new Profile(scientificName: "name", opus: opus1, classification: new Classification(kingdom: "Plantae"))
        Profile profile2 = save new Profile(scientificName: "name two", opus: opus1, classification: new Classification(kingdom: "Plantae"))
        Profile profile3 = save new Profile(scientificName: "nameThree", opus: opus1, classification: new Classification(kingdom: "Plantae"))

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

        Profile profile1 = save new Profile(scientificName: "ab", opus: opus, classification: new Classification(kingdom: "kingdom"))
        save new Profile(scientificName: "ac", opus: opus, classification: new Classification(kingdom: "kingdom"))
        Profile profile3 = save new Profile(scientificName: "aa", opus: opus, classification: new Classification(kingdom: "kingdom"))
        save new Profile(scientificName: "ad", opus: opus, classification: new Classification(kingdom: "kingdom"))

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

        Profile profile1 = save new Profile(scientificName: "ab", opus: opus, classification: new Classification(kingdom: "kingdom"))
        Profile profile2 = save new Profile(scientificName: "ac", opus: opus, classification: new Classification(kingdom: "kingdom"))
        save new Profile(scientificName: "aa", opus: opus, classification: new Classification(kingdom: "kingdom"))
        Profile profile4 = save new Profile(scientificName: "ad", opus: opus, classification: new Classification(kingdom: "kingdom"))

        when:
        List result = service.findByScientificName("a", [opus.uuid], true, 1, 1)

        then:
        result == [profile1]

        when:
        result = service.findByScientificName("a", [opus.uuid], true, 2, 2)

        then:
        result == [profile2, profile4]
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

        Profile profile1 = save new Profile(scientificName: "name", opus: opus1, classification: new Classification(kingdom: "kingdom1"))
        Profile profile2 = save new Profile(scientificName: "name", opus: opus2, classification: new Classification(kingdom: "kingdom2"))
        Profile profile3 = save new Profile(scientificName: "name", opus: opus3, classification: new Classification(kingdom: "kingdom3"))

        when:
        List result = service.findByTaxonNameAndLevel("kingdom", "kingdom", null)

        then:
        result.size() == 3
        result.contains(profile1)
        result.contains(profile2)
        result.contains(profile3)
    }

    def "findByTaxonNameAndLevel should match only the specified opus ids"() {
        given:
        Opus opus1 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr1", title: "title1")
        Opus opus2 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr2", title: "title2")
        Opus opus3 = save new Opus(glossary: new Glossary(), dataResourceUid: "dr3", title: "title3")

        Profile profile1 = save new Profile(scientificName: "name", opus: opus1, classification: new Classification(kingdom: "kingdom1"))
        Profile profile2 = save new Profile(scientificName: "name", opus: opus2, classification: new Classification(kingdom: "kingdom2"))
        Profile profile3 = save new Profile(scientificName: "name", opus: opus3, classification: new Classification(kingdom: "kingdom3"))

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

        Profile profile1 = save new Profile(scientificName: "name", opus: opus1, classification: new Classification(kingdom: "kingdom1"))
        Profile profile2 = save new Profile(scientificName: "name", opus: opus2, classification: new Classification(kingdom: "kingdom2"))
        Profile profile3 = save new Profile(scientificName: "name", opus: opus3, classification: new Classification(kingdom: "kingdom3"))

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
        Profile profile1 = save new Profile(scientificName: "name", opus: opus1, classification: new Classification(kingdom: "kingdom1"))
        Profile profile2 = save new Profile(scientificName: "name", opus: opus1, classification: new Classification(kingdom: "kingdom2"))
        Profile profile3 = save new Profile(scientificName: "name", opus: opus1, classification: new Classification(kingdom: "kingdom3"))

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
        Profile profile1 = save new Profile(scientificName: "name", opus: opus1, classification: new Classification(kingdom: "kingdom"))
        Profile profile2 = save new Profile(scientificName: "name", opus: opus1, classification: new Classification(kingdom: "kingdom2"))
        Profile profile3 = save new Profile(scientificName: "name", opus: opus1, classification: new Classification(kingdom: "kingdom3"))

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

        Profile profile1 = save new Profile(scientificName: "ab", opus: opus, classification: new Classification(kingdom: "ab"))
        save new Profile(scientificName: "ac", opus: opus, classification: new Classification(kingdom: "ac"))
        Profile profile3 = save new Profile(scientificName: "aa", opus: opus, classification: new Classification(kingdom: "aa"))
        save new Profile(scientificName: "ad", opus: opus, classification: new Classification(kingdom: "ad"))

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

        Profile profile1 = save new Profile(scientificName: "ab", opus: opus, classification: new Classification(kingdom: "ab"))
        Profile profile2 = save new Profile(scientificName: "ac", opus: opus, classification: new Classification(kingdom: "ac"))
        save new Profile(scientificName: "aa", opus: opus, classification: new Classification(kingdom: "aa"))
        Profile profile4 = save new Profile(scientificName: "ad", opus: opus, classification: new Classification(kingdom: "ad"))

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

        save new Profile(scientificName: "kingdom1", opus: opus, classification: new Classification(kingdom: "Plantae"))
        save new Profile(scientificName: "kingdom2", opus: opus, classification: new Classification(kingdom: "Plantae"))
        save new Profile(scientificName: "kingdom3", opus: opus, classification: new Classification(kingdom: "Animalia"))

        save new Profile(scientificName: "phylum1", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta"))
        save new Profile(scientificName: "phylum2", opus: opus, classification: new Classification(kingdom: "Animalia", phylum: "Hygrophila"))
        save new Profile(scientificName: "phylum3", opus: opus, classification: new Classification(kingdom: "Animalia", phylum: "Arthropoda"))

        save new Profile(scientificName: "class1", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta", clazz: "Equisetopsida"))

        save new Profile(scientificName: "subclass1", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta", clazz: "Equisetopsida", subclazz: "Cycadidae"))
        save new Profile(scientificName: "subclass2", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta", clazz: "Equisetopsida", subclazz: "Magnoliidae"))
        save new Profile(scientificName: "subclass3", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta", clazz: "Equisetopsida", subclazz: "Marattiidae"))
        save new Profile(scientificName: "subclass4", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta", clazz: "Equisetopsida", subclazz: "Ophioglossidae"))

        save new Profile(scientificName: "order1", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta", clazz: "Equisetopsida", subclazz: "Ophioglossidae", order: "Apiales"))
        save new Profile(scientificName: "order2", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta", clazz: "Equisetopsida", subclazz: "Ophioglossidae", order: "Arecales"))

        save new Profile(scientificName: "family1", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta", clazz: "Equisetopsida", subclazz: "Ophioglossidae", order: "Apiales", family: "Aphididae"))
        save new Profile(scientificName: "family2", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta", clazz: "Equisetopsida", subclazz: "Ophioglossidae", order: "Apiales", family: "Acanthaceae"))
        save new Profile(scientificName: "family3", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta", clazz: "Equisetopsida", subclazz: "Ophioglossidae", order: "Apiales", family: "Adoxaceae"))
        save new Profile(scientificName: "family4", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta", clazz: "Equisetopsida", subclazz: "Ophioglossidae", order: "Apiales", family: "Agapanthaceae"))

        save new Profile(scientificName: "genus1", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta", clazz: "Equisetopsida", subclazz: "Ophioglossidae", order: "Apiales", family: "Agapanthaceae", genus: "Abildgaardia"))
        save new Profile(scientificName: "genus2", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta", clazz: "Equisetopsida", subclazz: "Ophioglossidae", order: "Asterales", family: "Agapanthaceae", genus: "Abrophyllum"))

        save new Profile(scientificName: "species1", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta", clazz: "Equisetopsida", subclazz: "Ophioglossidae", order: "Agapanthaceae", genus: "Abrophyllum", species: "bla"))


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

        save new Profile(scientificName: "kingdom1", opus: opus, classification: new Classification(kingdom: "Plantae"))
        save new Profile(scientificName: "kingdom2", opus: opus, classification: new Classification(kingdom: "Plantae"))
        save new Profile(scientificName: "kingdom3", opus: opus, classification: new Classification(kingdom: "Animalia"))

        save new Profile(scientificName: "phylum1", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta"))
        save new Profile(scientificName: "phylum2", opus: opus, classification: new Classification(kingdom: "Animalia", phylum: "Hygrophila"))
        save new Profile(scientificName: "phylum3", opus: opus, classification: new Classification(kingdom: "Animalia", phylum: "Arthropoda"))

        save new Profile(scientificName: "class1", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta", clazz: "Equisetopsida"))

        save new Profile(scientificName: "subclass1", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta", clazz: "Equisetopsida", subclazz: "Cycadidae"))
        save new Profile(scientificName: "subclass2", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta", clazz: "Equisetopsida", subclazz: "Magnoliidae"))
        save new Profile(scientificName: "subclass3", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta", clazz: "Equisetopsida", subclazz: "Marattiidae"))
        save new Profile(scientificName: "subclass4", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta", clazz: "Equisetopsida", subclazz: "Ophioglossidae"))

        save new Profile(scientificName: "order1", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta", clazz: "Equisetopsida", subclazz: "Ophioglossidae", order: "Apiales"))
        save new Profile(scientificName: "order2", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta", clazz: "Equisetopsida", subclazz: "Ophioglossidae", order: "Arecales"))

        save new Profile(scientificName: "family1", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta", clazz: "Equisetopsida", subclazz: "Ophioglossidae", order: "Apiales", family: "Aphididae"))
        save new Profile(scientificName: "family2", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta", clazz: "Equisetopsida", subclazz: "Ophioglossidae", order: "Apiales", family: "Acanthaceae"))
        save new Profile(scientificName: "family3", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta", clazz: "Equisetopsida", subclazz: "Ophioglossidae", order: "Apiales", family: "Adoxaceae"))
        save new Profile(scientificName: "family4", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta", clazz: "Equisetopsida", subclazz: "Ophioglossidae", order: "Apiales", family: "Agapanthaceae"))

        save new Profile(scientificName: "genus1", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta", clazz: "Equisetopsida", subclazz: "Ophioglossidae", order: "Apiales", family: "Agapanthaceae", genus: "Abildgaardia"))
        save new Profile(scientificName: "genus2", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta", clazz: "Equisetopsida", subclazz: "Ophioglossidae", order: "Asterales", family: "Agapanthaceae", genus: "Abrophyllum"))

        save new Profile(scientificName: "species1", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta", clazz: "Equisetopsida", subclazz: "Ophioglossidae", order: "Agapanthaceae", genus: "Abrophyllum", species: "bla"))


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

        save new Profile(scientificName: "subclass1", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta", clazz: "Equisetopsida", subclazz: "Cycadidae"))
        save new Profile(scientificName: "subclass2", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta", clazz: "Equisetopsida", subclazz: "Magnoliidae"))
        save new Profile(scientificName: "subclass3", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Arthropoda", clazz: "Equisetopsida", subclazz: "Marattiidae"))
        save new Profile(scientificName: "subclass4", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Hygrophila", clazz: "Equisetopsida", subclazz: "Ophioglossidae"))

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

        save new Profile(scientificName: "subclass1", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta", clazz: "Equisetopsida", subclazz: "Cycadidae"))
        save new Profile(scientificName: "subclass2", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Charophyta", clazz: "Equisetopsida", subclazz: "Magnoliidae"))
        save new Profile(scientificName: "subclass3", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Arthropoda", clazz: "Equisetopsida", subclazz: "Marattiidae"))
        save new Profile(scientificName: "subclass4", opus: opus, classification: new Classification(kingdom: "Plantae", phylum: "Hygrophila", clazz: "Equisetopsida", subclazz: "Ophioglossidae"))

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
