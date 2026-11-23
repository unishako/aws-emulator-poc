#!/bin/sh
# エミュレータが生成した CA 証明書を信頼ストアに取り込み、認証情報を SDK に渡してアプリを起動する。
set -eu

EMU_DIR="${EMU_DIR:-/emu/9297}"
TRUSTSTORE=/tmp/truststore.p12

keytool -importcert -noprompt -alias emulator \
  -file "$EMU_DIR/ca.pem" \
  -keystore "$TRUSTSTORE" -storetype PKCS12 -storepass changeit

export AWS_SHARED_CREDENTIALS_FILE="$EMU_DIR/emulator_credentials"

exec java \
  -Djavax.net.ssl.trustStore="$TRUSTSTORE" \
  -Djavax.net.ssl.trustStorePassword=changeit \
  -jar /app/app.jar
