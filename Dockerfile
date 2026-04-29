# =============================================================
#  GlassFish 7 Dockerfile (Jakarta EE 10)
#  - PostgreSQL 42.7.5 JDBC driver
#  - Datasource setup runtime'da entrypoint script ile
# =============================================================

FROM ghcr.io/eclipse-ee4j/glassfish:7.0.25

USER root

ENV POSTGRES_VERSION=42.7.5
ENV GF_HOME=/opt/glassfish7
ENV GF_DOMAIN_DIR=/opt/glassfish7/glassfish/domains/domain1

# 1) Klasorler
RUN mkdir -p ${GF_DOMAIN_DIR}/lib && \
    mkdir -p ${GF_DOMAIN_DIR}/autodeploy

# 2) PostgreSQL JDBC driver
RUN curl -fSL -o ${GF_DOMAIN_DIR}/lib/postgresql-${POSTGRES_VERSION}.jar \
        https://repo1.maven.org/maven2/org/postgresql/postgresql/${POSTGRES_VERSION}/postgresql-${POSTGRES_VERSION}.jar

# 3) Setup script + entrypoint
COPY docker/glassfish-resources.asadmin /opt/glassfish-resources.asadmin
COPY docker/entrypoint.sh /opt/entrypoint.sh
RUN chmod +x /opt/entrypoint.sh

# 4) WAR
COPY target/muhasebe-backend.war ${GF_DOMAIN_DIR}/autodeploy/muhasebe-backend.war

# 5) KRITIK: WAR kopyalamasindan SONRA chown yap
#    (her COPY komutu yeni dosyalari root sahipliginde birakir)
RUN chown -R glassfish:glassfish ${GF_DOMAIN_DIR} && \
    chown glassfish:glassfish /opt/glassfish-resources.asadmin /opt/entrypoint.sh

EXPOSE 8080 4848

ENTRYPOINT ["/opt/entrypoint.sh"]