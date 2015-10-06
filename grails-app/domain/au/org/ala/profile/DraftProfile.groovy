package au.org.ala.profile

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class DraftProfile {

    String uuid
    String guid                 //taxon GUID / LSID
    String scientificName
    String nameAuthor
    String fullName
    Name matchedName
    String rank
    String nslNameIdentifier
    String nslNomenclatureIdentifier

    String primaryImage
    List<String> excludedImages
    List<String> specimenIds
    List<Authorship> authorship
    List<Classification> classification
    List<Link> links
    List<Link> bhlLinks
    List<Attribute> attributes
    List<Publication> publications
    List<Bibliography> bibliography
    List<LocalImage> stagedImages
    List<LocalImage> privateImages

    String lastAttributeChange

    Date dateCreated
    Date draftDate = new Date()
    String createdBy

    static embedded = ['authorship', 'classification', 'draft', 'links', 'bhlLinks', 'publications', 'bibliography', 'attributes', 'stagedImages', 'privateImages']

    static constraints = {
        nameAuthor nullable: true
        guid nullable: true
        primaryImage nullable: true
        nslNameIdentifier nullable: true
        nslNomenclatureIdentifier nullable: true
        rank nullable: true
        fullName nullable: true
        matchedName nullable: true
        createdBy nullable: true
        lastAttributeChange nullable: true
    }
}
