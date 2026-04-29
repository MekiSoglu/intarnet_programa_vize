# =============================================================
#  GlassFish 7 Dockerfile (Jakarta EE 10)
#  - PostgreSQL 42.7.5 JDBC driver lib/ klasoruunde
#  - Datasource setup runtime'da entrypoint script ile yapilir
# =============================================================

FROM ghcr.io/eclipse-ee4j/glassfish:7.0.25

USER root

# GlassFish 7.0.25 imajinda gercek path:
#   /opt/glassfish7/...
# asadmin: /opt/glassfish7/bin/asadmin

ENV POSTGRES_VERSION=42.7.5
ENV GF_HOME=/opt/glassfish7
ENV GF_DOMAIN_DIR=/opt/glassfish7/glassfish/domains/domain1

# 1) Lib ve autodeploy klasorleri (yoksa olustur)
RUN mkdir -p ${GF_DOMAIN_DIR}/lib && \
    mkdir -p ${GF_DOMAIN_DIR}/autodeploy

# 2) PostgreSQL JDBC driver indir
RUN curl -fSL -o ${GF_DOMAIN_DIR}/lib/postgresql-${POSTGRES_VERSION}.jar \
        https://repo1.maven.org/maven2/org/postgresql/postgresql/${POSTGRES_VERSION}/postgresql-${POSTGRES_VERSION}.jar

# 3) WAR'i autodeploy klasorune
COPY target/muhasebe-backend.war ${GF_DOMAIN_DIR}/autodeploy/muhasebe-backend.war

# 4) Datasource setup script + entrypoint
COPY docker/glassfish-resources.asadmin /opt/glassfish-resources.asadmin
COPY docker/entrypoint.sh /opt/entrypoint.sh
RUN chmod +x /opt/entrypoint.sh

EXPOSE 8080 4848

ENTRYPOINT ["/opt/entrypoint.sh"]