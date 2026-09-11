#!/bin/sh
# Map Fly/Heroku DATABASE_URL into Spring Boot JDBC env vars.
set -e
if [ -n "$DATABASE_URL" ]; then
  # Format: postgres://user:pass@host:port/db?params
  without_scheme="${DATABASE_URL#postgres://}"
  without_scheme="${without_scheme#postgresql://}"
  userpass="${without_scheme%%@*}"
  hostpart="${without_scheme#*@}"
  user="${userpass%%:*}"
  pass="${userpass#*:}"
  hostport="${hostpart%%/*}"
  dbpath="${hostpart#*/}"
  db="${dbpath%%\?*}"
  host="${hostport%%:*}"
  port="${hostport##*:}"
  if [ "$port" = "$hostport" ]; then
    port=5432
  fi
  # Private DNS: prefer .internal over .flycast
  case "$host" in
    *.flycast) host="${host%.flycast}.internal" ;;
  esac
  export SPRING_DATASOURCE_URL="jdbc:postgresql://${host}:${port}/${db}?sslmode=disable"
  export SPRING_DATASOURCE_USERNAME="$user"
  export SPRING_DATASOURCE_PASSWORD="$pass"
fi
exec java $JAVA_OPTS -jar app.jar
