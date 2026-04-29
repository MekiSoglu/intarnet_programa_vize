#!/bin/bash
# =============================================================
#  GlassFish entrypoint
#  - Container ilk acildiginda datasource'lari yaratir
#  - Sonra GlassFish'i foreground'da baslatir
# =============================================================
set -e

GF_HOME=/opt/glassfish7
ASADMIN=${GF_HOME}/bin/asadmin
DOMAIN_DIR=${GF_HOME}/glassfish/domains/domain1

# Marker dosya - ilk acilis mi?
if [ ! -f "${DOMAIN_DIR}/.datasource-initialized" ]; then
    echo "[entrypoint] First boot - initializing datasource..."

    ${ASADMIN} start-domain

    ${ASADMIN} multimode --file /opt/glassfish-resources.asadmin || {
        echo "[entrypoint] ERROR: Datasource setup failed"
        ${ASADMIN} stop-domain
        exit 1
    }

    touch "${DOMAIN_DIR}/.datasource-initialized"
    ${ASADMIN} stop-domain

    echo "[entrypoint] Datasource setup complete."
fi

echo "[entrypoint] Starting GlassFish in foreground..."
exec ${ASADMIN} start-domain --verbose