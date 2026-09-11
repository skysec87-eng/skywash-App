#!/bin/sh
# Map DATABASE_URL (Fly / Neon / Heroku) into Spring JDBC env vars.
set -e
if [ -n "$DATABASE_URL" ]; then
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
  case "$host" in
    *.flycast) host="${host%.flycast}.internal" ;;
  esac
  sslmode=disable
  case "$DATABASE_URL" in
    *sslmode=require*|*neon.tech*) sslmode=require ;;
  esac
  export SPRING_DATASOURCE_URL="jdbc:postgresql://${host}:${port}/${db}?sslmode=${sslmode}"
  export SPRING_DATASOURCE_USERNAME="$user"
  export SPRING_DATASOURCE_PASSWORD="$pass"
fi
exec java $JAVA_OPTS -jar app.jar
