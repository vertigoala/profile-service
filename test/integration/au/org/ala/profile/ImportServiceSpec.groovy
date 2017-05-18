package au.org.ala.profile

class ImportServiceSpec extends BaseIntegrationSpec {

    ImportService importService
    MasterListService masterListService
    NameService nameService

    def setup() {
        masterListService = Stub(MasterListService)
        nameService = Stub(NameService)
        importService = new ImportService()
        importService.masterListService = masterListService
        importService.nameService = nameService

        masterListService.getMasterList(_) >> { opus -> [['name': 'a', 'scientificName': 'a'], ['name': 'b', 'scientificName': 'b'], ['name': 'c', 'scientificName': 'c']]}

        def nsldump = [
                [scientificName    : 'a',
                 scientificNameHtml: '<common><name id=\'1\'><element class=\'a\'>a</element></name></common>',
                 fullName          : 'a',
                 fullNameHtml      : '<common><name id=\'1\'><element class=\'a\'>a</element></name></common>',
                 url               : "http://name/a",
                 nslIdentifier     : 'a',
                 rank              : 'genus',
                 nameAuthor        : 'author',
                 nslProtologue     : "Ching, R.C. (1963), Acta Phytotaxonomica Sinica 8 1963".trim(),
                 valid             : true,
                 status            : 'legitimate']
        ]

        def nslDumpResult = [
                bySimpleName: nsldump.groupBy { it.canonicalName },
                byFullName: nsldump.groupBy { it.scientificName }
        ]

        nameService.loadNSLSimpleNameDump() >> nslDumpResult
    }


    def testSyncMasterList() {
        given:
        def opus = new Opus(title: "opus", shortName: 'opus', dataResourceUid: "123", glossary: new Glossary(), masterListUid: 'a')
        save opus
        def a = new Profile(opus: opus, scientificName: 'a', profileStatus: Profile.STATUS_EMPTY)
        def b = new Profile(opus: opus, scientificName: 'b', profileStatus: Profile.STATUS_PARTIAL)
        def d = new Profile(opus: opus, scientificName: 'd', profileStatus: Profile.STATUS_EMPTY)
        def e = new Profile(opus: opus, scientificName: 'e', profileStatus: Profile.STATUS_PARTIAL)

        save a
        save b
        save d
        save e

        when:
        importService.syncMasterList(opus)
        def a1 = Profile.findByScientificName('a')
        def b1 = Profile.findByScientificName('b')
        def c1 = Profile.findByScientificName('c')
        def d1 = Profile.findByScientificName('d')
        def e1 = Profile.findByScientificName('e')

        then:
        a1 != null
        a1.profileStatus == Profile.STATUS_EMPTY

        b1 != null
        b1.profileStatus == Profile.STATUS_PARTIAL

        c1 != null
        c1.profileStatus == Profile.STATUS_EMPTY
        c1.uuid != null
        c1.uuid != ''

        d1 == null

        e1 != null
        e1.profileStatus == Profile.STATUS_PARTIAL
    }

}
