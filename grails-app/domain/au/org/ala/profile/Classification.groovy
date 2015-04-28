package au.org.ala.profile

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@ToString
@EqualsAndHashCode
class Classification {
    String rank
    String kingdom
    String kingdomGuid
    String phylum
    String phylumGuid
    String clazz
    String clazzGuid
    String subclazz
    String subclazzGuid
    String order
    String orderGuid
    String family
    String familyGuid
    String genus
    String genusGuid
    String species
    String speciesGuid

    static constraints = {
        rank nullable: true
        kingdom nullable: true
        kingdomGuid nullable: true
        phylum nullable: true
        phylumGuid nullable: true
        clazz nullable: true
        clazzGuid nullable: true
        subclazz nullable: true
        subclazzGuid nullable: true
        order nullable: true
        orderGuid nullable: true
        family nullable: true
        familyGuid nullable: true
        genus nullable: true
        genusGuid nullable: true
        species nullable: true
        speciesGuid nullable: true
    }
}
