package de.otto.platform.gitactionboard.config;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GithubActionConfig {
  @Bean(name = "authToken")
  public String githubAuthToken(@Value("${GITHUB_ACCESS_TOKEN:}") final String authToken) {
    if (authToken.isBlank()) return authToken;

    final String base64Creds = convertToBase64(String.format(":%s", authToken));
    return String.format("Basic %s", base64Creds);
  }

  private String convertToBase64(String plainCreds) {
    final byte[] base64CredsBytes = Base64.getEncoder().encode(plainCreds.getBytes(UTF_8));
    return new String(base64CredsBytes, UTF_8);
  }

  @Bean(name = "domainName")
  public String domainName(@Value("${DOMAIN_NAME:https://api.github.com}") String domainName) {
    return domainName;
  }

  @Bean(name = "ownerName")
  public String ownerName(@Value("${REPO_OWNER_NAME}") String ownerName) {
    if (ownerName.isBlank())
      throw new IllegalArgumentException(
              "REPO_OWNER_NAME environment variable is either empty or its not set");
    return ownerName;
  }

  @Bean(name = "svcToken")
  public String svcToken(@Value("${GITHUB_SVC_TOKEN}") final String svcToken) {
    if (svcToken.isBlank())
      throw new IllegalArgumentException(
              "GITHUB_SVC_TOKEN environment variable is either empty or its not set");
    return String.format("token %s", svcToken);
  }

  @Bean(name = "teamSlug")
  public String teamSlug(@Value("${REPO_TEAM_SLUG}") String teamSlug) {
    if (teamSlug.isBlank())
      throw new IllegalArgumentException(
              "REPO_TEAM_SLUG environment variable is either empty or its not set");
    return teamSlug;
  }

  @Bean(name = "excludedRepoNames")
  public List<String> excludedRepoNames(@Value("${REPO_EXCLUDE}") final String excludedRepos) {
    if (excludedRepos.isBlank()) return new ArrayList<>();
    return List.of(excludedRepos.split(","));
  }
}