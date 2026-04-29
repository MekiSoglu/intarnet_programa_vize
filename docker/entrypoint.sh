#!/bin/bash
# =============================================================
#  GlassFish entrypoint
# =============================================================
set -e

GF_HOME=/opt/glassfish7
ASADMIN="${GF_HOME}/bin/asadmin"
AUTODEPLOY_DIR="${GF_HOME}/glassfish/domains/domain1/autodeploy"

# Onceki basarisiz autodeploy markerlarini temizle
echo "[entrypoint] Cleaning up old autodeploy markers..."
rm -f "${AUTODEPLOY_DIR}"/*.war_deployFailed 2>/dev/null
rm -f "${AUTODEPLOY_DIR}"/*.war_deployed 2>/dev/null
rm -rf "${AUTODEPLOY_DIR}/.autodeploystatus" 2>/dev/null

echo "[entrypoint] Starting GlassFish for setup..."
${ASADMIN} start-domain

# Datasource var mi?
if ${ASADMIN} list-jdbc-resources 2>/dev/null | grep -q "^jdbc/MuhasebeDS$"; then
    echo "[entrypoint] OK: jdbc/MuhasebeDS already exists, skipping setup."
else
    echo "[entrypoint] Creating MuhasebePool connection pool..."
    ${ASADMIN} create-jdbc-connection-pool \
        --datasourceclassname=org.postgresql.ds.PGSimpleDataSource \
        --restype=javax.sql.DataSource \
        --property "user=postgres:password=postgres:databaseName=muhasebedb:serverName=postgres:portNumber=5432" \
        MuhasebePool

    echo "[entrypoint] Creating jdbc/MuhasebeDS resource..."
    ${ASADMIN} create-jdbc-resource --connectionpoolid=MuhasebePool jdbc/MuhasebeDS

    echo "[entrypoint] Pinging connection pool..."
    ${ASADMIN} ping-connection-pool MuhasebePool || {
        echo "[entrypoint] WARNING: ping failed (postgres may not be ready)"
    }

    echo "[entrypoint] Datasource setup complete."
fi

echo "[entrypoint] Stopping domain to restart in foreground..."
${ASADMIN} stop-domain

echo "[entrypoint] Starting GlassFish in foreground..."
exec ${ASADMIN} start-domain --verbose