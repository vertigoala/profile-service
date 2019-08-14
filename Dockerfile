#
# profile-service
#
FROM tomcat:8.5-jre8-alpine

#ARG ARTIFACT_URL=https://nexus.ala.org.au/service/local/repositories/releases/content/au/org/ala/profile-service/2.4/profile-service-2.4.war
ARG ARTIFACT_URL=https://ala-rnp.s3.amazonaws.com/ala-assets/brasil/profile-service-2.5-SNAPSHOT.war
ARG WAR_NAME=profile-service

RUN mkdir -p /data/profile-service/config \
             /data/profile-service/snapshots \
             /data/profile-service/attachments \
             /data/db

RUN wget $ARTIFACT_URL -q -O /tmp/$WAR_NAME && \
    mkdir -p $CATALINA_HOME/webapps/$WAR_NAME && \
    unzip /tmp/$WAR_NAME -d $CATALINA_HOME/webapps/$WAR_NAME && \
    rm /tmp/$WAR_NAME
RUN apk add --update curl zip tini && \
    wget https://sibbr-modules.sfo2.digitaloceanspaces.com/namematching.zip -q -O /opt/namematchingprof.zip && \
    unzip -o /opt/namematchingprof.zip -d /data/lucene && \
    rm /opt/namematchingprof.zip

# Tomcat configs
COPY ./tomcat-conf/* /usr/local/tomcat/conf/	

EXPOSE 8080

# NON-ROOT
RUN addgroup -g 101 tomcat && \
    adduser -G tomcat -u 101 -S tomcat && \
    chown -R tomcat:tomcat /usr/local/tomcat && \
    chown -R tomcat:tomcat /data

USER tomcat

ENV CATALINA_OPTS '-Dgrails.env=production'
ENV JAVA_OPTS '-Dport.shutdown=8005 -Dport.http=8080'

ENTRYPOINT ["tini", "--"]
CMD ["catalina.sh", "run"]
