package com.skywash.api.config;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

/**
 * Fly.io / Heroku style: map DATABASE_URL=postgres://user:pass@host:port/db
 * into Spring Datasource properties when SPRING_DATASOURCE_URL is unset.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    String explicit = environment.getProperty("SPRING_DATASOURCE_URL");
    if (StringUtils.hasText(explicit) && !explicit.contains("localhost")) {
      return;
    }

    String databaseUrl = environment.getProperty("DATABASE_URL");
    if (!StringUtils.hasText(databaseUrl) || !databaseUrl.startsWith("postgres")) {
      return;
    }

    try {
      URI uri = URI.create(databaseUrl);
      String userInfo = uri.getUserInfo();
      if (userInfo == null || !userInfo.contains(":")) {
        return;
      }
      int colon = userInfo.indexOf(':');
      String user = userInfo.substring(0, colon);
      String pass = userInfo.substring(colon + 1);
      String path = uri.getPath();
      if (path == null || path.length() < 2) {
        return;
      }
      String db = path.startsWith("/") ? path.substring(1) : path;
      int q = db.indexOf('?');
      if (q >= 0) db = db.substring(0, q);
      int port = uri.getPort() > 0 ? uri.getPort() : 5432;
      String host = uri.getHost();
      if (host != null && host.endsWith(".flycast")) {
        host = host.substring(0, host.length() - ".flycast".length()) + ".internal";
      }
      String ssl = "?sslmode=disable";
      if (host != null && host.contains("neon.tech") || databaseUrl.contains("sslmode=require")) {
        ssl = "?sslmode=require";
      }
      String jdbc = "jdbc:postgresql://" + host + ":" + port + "/" + db + ssl;

      Map<String, Object> props = new HashMap<>();
      props.put("spring.datasource.url", jdbc);
      props.put("spring.datasource.username", user);
      props.put("spring.datasource.password", pass);
      environment.getPropertySources().addFirst(new MapPropertySource("flyDatabaseUrl", props));
    } catch (Exception ignored) {
      // Leave defaults; startup will fail clearly if DB is required.
    }
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 10;
  }
}
