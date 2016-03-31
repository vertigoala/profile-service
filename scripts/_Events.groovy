import grails.util.Environment
import org.apache.catalina.connector.*
import org.apache.catalina.startup.Tomcat

eventCreateWarStart = { warName, stagingDir ->
    ant.propertyfile(file: "${stagingDir}/WEB-INF/classes/application.properties") {
        entry(key: "app.build", value: new Date().format("dd/MM/yyyy HH:mm:ss"))
    }
}

eventConfigureTomcat = { Tomcat tomcat ->
    if (Environment.current == Environment.DEVELOPMENT) {
        println "### Enabling AJP/1.3 connector"

        def ajpConnector = new Connector("org.apache.coyote.ajp.AjpProtocol")
        ajpConnector.port = 8010
        ajpConnector.protocol = 'AJP/1.3'
        ajpConnector.redirectPort = 8443
        ajpConnector.enableLookups = false
        ajpConnector.setProperty('redirectPort', '8443')
        ajpConnector.setProperty('protocol', 'AJP/1.3')
        ajpConnector.setProperty('enableLookups', 'false')
        tomcat.service.addConnector ajpConnector

        println ajpConnector.toString()


        println "### Ending enabling AJP connector"
    }
}