#
# WildFly 31 Dockerfile (basitlestirilmis)
# - Jakarta EE 10
# - PostgreSQL 42.7.5 JDBC driver modulu
# - Datasource jboss-cli batch dosyasi ile register ediliyor
#

FROM quay.io/wildfly/wildfly:31.0.1.Final-jdk17

USER root

# PostgreSQL JDBC driver'i indir
ENV POSTGRES_VERSION=42.7.5
RUN mkdir -p ${JBOSS_HOME}/modules/org/postgresql/main && \
    curl -fSL -o ${JBOSS_HOME}/modules/org/postgresql/main/postgresql-${POSTGRES_VERSION}.jar \
        https://repo1.maven.org/maven2/org/postgresql/postgresql/${POSTGRES_VERSION}/postgresql-${POSTGRES_VERSION}.jar

# Module XML'i kopyala
COPY docker/postgresql-module.xml ${JBOSS_HOME}/modules/org/postgresql/main/module.xml

# Datasource'u jboss-cli batch ile register et
COPY docker/datasource.cli /tmp/datasource.cli
RUN ${JBOSS_HOME}/bin/jboss-cli.sh --file=/tmp/datasource.cli && \
    rm -f /tmp/datasource.cli

# Yonetim kullanicisi
RUN ${JBOSS_HOME}/bin/add-user.sh admin admin123 --silent

# WAR dosyasini deploy et
COPY target/muhasebe-backend.war ${JBOSS_HOME}/standalone/deployments/

RUN chown -R jboss:jboss ${JBOSS_HOME}/standalone/ ${JBOSS_HOME}/modules/

USER jboss

EXPOSE 8080 9990

CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]
