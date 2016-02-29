package au.org.ala.profile
import grails.test.mixin.*
import org.springframework.web.multipart.MultipartFile
import spock.lang.Ignore
import spock.lang.Specification

/**
 * Created by shi131 on 26/02/2016.
 */
@TestFor(AttachmentService)
class AttachmentServiceSpec extends Specification{

    def "Filename will have correct extension"(){

        given: "an absolute file name with extension"
            String originalName = '/data/snapshot/445.pdf'
            String expectedExtension = 'zip'
        when: "we check the extension"
            String finalName = service.makeSureFileExtionsionIsCorrect(expectedExtension,originalName)
        then:"the final extension will be what we want"
            finalName.substring(finalName.indexOf('.')+1) == expectedExtension
  }

    def "Filename will include absolute path"(){
        given: "an absolute file name"
            String originalName = '/data/snapshot/445.pdf'
            String expectedExtension = 'zip'
        when: "we check the extension"
            String finalName = service.makeSureFileExtionsionIsCorrect(expectedExtension,originalName)
        then: "we retain the absolute path"
            finalName.contains('/data/snapshot')
            finalName.equalsIgnoreCase('/data/snapshot/445.zip')
    }


       @Ignore
    def "When we convert a file, the name will be retained"(){
        given:"an existing multipart file"
        MultipartFile multipartFile = null
        String originalFileName =  multipartFile.getOriginalFilename()
        when:"we convert it to a File"
        File convertedFile = service.convertMultiPartToFile(originalFileName)
        then:"it will retain a sensible name"
        convertedFile.getName() == originalFileName
    }
}
