package de.otto.platform.gitactionboard.adapters.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.otto.platform.gitactionboard.adapters.service.workflow.CollaboratorResponse;
import de.otto.platform.gitactionboard.adapters.service.workflow.RepositoriesResponse;
import okhttp3.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GithubGraphqlClient {

  private static final Map<String, List<String>> userRepositories = new java.util.concurrent.ConcurrentHashMap<>();
  private static final Map<String, List<String>> verifiedForUser = new java.util.concurrent.ConcurrentHashMap<>();

  private static final String GITHUB_GRAPHQL_ENDPOINT = "/graphql";
  private final OkHttpClient client;
  private final String githubToken;
  private final String baseUri;
  private final String ownerName;
  private final String teamSlug;
  private final List<String> excludedRepoNames;
  private final Logger logger = org.slf4j.LoggerFactory.getLogger(GithubGraphqlClient.class);

  public GithubGraphqlClient(@Qualifier("domainName") String baseUri, @Qualifier("ownerName") String ownerName, @Qualifier("teamSlug") String teamSlug, @Qualifier("svcToken") String svcToken, @Qualifier("excludedRepoNames") List<String> excludedRepoNames) {
    this.baseUri = baseUri;
    this.ownerName = ownerName;
    this.client = new OkHttpClient();
    this.githubToken = svcToken;
    this.teamSlug = teamSlug;
    if (githubToken == null) {
      throw new RuntimeException("Github token is null");
    }
    this.excludedRepoNames = excludedRepoNames;
  }

  public List<String> getUserRepositoriesInOrganization(String accessCode) throws IOException {
    String username = getUsernameFromGithubToken(accessCode);
    List<String> verifiedUserRepositories = new ArrayList<>();
    List<String> repositories = getRepositoriesForTeam(teamSlug);
    if (excludedRepoNames != null && !excludedRepoNames.isEmpty()) {
      repositories = repositories.stream().filter(repoName -> !excludedRepoNames.contains(repoName)).toList();
    }
    userRepositories.putIfAbsent(username, new ArrayList<>());
    verifiedForUser.putIfAbsent(username, new ArrayList<>());

    verifiedUserRepositories = repositories.parallelStream()
        .filter(repoName -> {
          try {
            return userIsAContributor(username, repoName);
          } catch (IOException e) {
            logger.error("Error while checking if user {} is a contributor of repo {}", username, repoName, e);
            return false;
          }
        })
        .collect(Collectors.toList());

    return verifiedUserRepositories;
  }

  private boolean userIsAContributor(String username, String repoName) throws IOException {
    if (userRepositories.containsKey(username) && verifiedForUser.containsKey(username)) {
      if (userRepositories.get(username).isEmpty()){
        logger.info("User {} has no verified repositories yet", username);
      }
      if (userRepositories.get(username).contains(repoName) || verifiedForUser.get(username).contains(repoName)) {
        logger.info("Already verified if User {} is a contributor of repo {}", username, repoName);
        return true;
      }
    }
    logger.info("Checking if User {} is a contributor of repo {}", username, repoName);
    String graphqlQuery = String.format("{\"query\":\"query ($org: String!, $repo: String!, $user: String!) { organization(login: $org) { repository(name: $repo) { name, collaborators(query: $user, first: 1) { edges { node { login } } } } } }\",\"variables\":{\"org\":\"%s\",\"repo\":\"%s\", \"user\": \"%s\" }}", ownerName, repoName, username);
    RequestBody body = RequestBody.create(graphqlQuery, MediaType.get("application/json; charset=utf-8"));
    Request request = new Request.Builder().url(baseUri + GITHUB_GRAPHQL_ENDPOINT).post(body).addHeader("Authorization", githubToken).build();
    logger.debug("Request: " + request);
    Response response = client.newCall(request).execute();
    ObjectMapper mapper = new ObjectMapper();
    assert response.body() != null;

    GraphqlResponse<CollaboratorResponse> graphqlResponse = mapper.readValue(response.body().string(), new TypeReference<GraphqlResponse<CollaboratorResponse>>() {});

    if (graphqlResponse.getData() == null) {
      verifiedForUser.get(username).add(repoName);
      throw new RuntimeException("Error fetching collaborators for repository: " + graphqlResponse.getMessage());
    }
    if (graphqlResponse.getErrors() != null) {
      verifiedForUser.get(username).add(repoName);
      throw new RuntimeException("Error fetching collaborators for repository: " + Arrays.toString(graphqlResponse.getErrors()));
    }

    boolean isContributor = graphqlResponse
        .getData()
        .getOrganization()
        .getRepository()
        .getCollaborators()
        .getEdges().stream().anyMatch(collaborator -> collaborator.getNode().getLogin().equals(username));

    if (isContributor) {
      userRepositories.get(username).add(repoName);
    }
    else {
      logger.info("User {} is not a contributor of repo {}", username, repoName);
    }
    verifiedForUser.get(username).add(repoName);

    return isContributor;
  }

  private List<String> getRepositoriesForTeam(String teamSlug) throws IOException {
    String repoCursor;
    List<RepositoriesResponse> allTeamRepos = new ArrayList<>();
    boolean hasNextRepoPage;
    String graphqlQuery = String.format("{\"query\":\"query ($org: String!, $team: String!, $cursor: String ) { organization(login: $org) { team(slug: $team) { name, repositories(first: 100, after: $cursor) { pageInfo { endCursor, hasNextPage }, edges { node { name, url } } } } } }\",\"variables\":{\"org\":\"%s\",\"team\":\"%s\", \"cursor\": null }}", ownerName, teamSlug);
    do {
      RequestBody body = RequestBody.create(graphqlQuery, MediaType.get("application/json; charset=utf-8"));
      Request request = new Request.Builder().url(baseUri + GITHUB_GRAPHQL_ENDPOINT).post(body).addHeader("Authorization", githubToken).build();
      logger.debug("Request: " + request);
      Response response = client.newCall(request).execute();
      ObjectMapper mapper = new ObjectMapper();
      assert response.body() != null;

      GraphqlResponse<RepositoriesResponse> graphqlResponse = mapper.readValue(response.body().string(), new TypeReference<GraphqlResponse<RepositoriesResponse>>() {});

      if (graphqlResponse.getData() == null) {
        throw new RuntimeException("Error fetching repositories for team: " + graphqlResponse.getMessage());
      }
      if (graphqlResponse.getErrors() != null) {
        throw new RuntimeException("Error fetching repositories for team: " + Arrays.toString(graphqlResponse.getErrors()));
      }

      RepositoriesResponse repositoriesResponse = graphqlResponse.getData();

      allTeamRepos.add(repositoriesResponse);
      repoCursor = repositoriesResponse.getOrganization().getTeam().getRepositories().getPageInfo().getEndCursor();
      hasNextRepoPage = repositoriesResponse.getOrganization().getTeam().getRepositories().getPageInfo().isHasNextPage();
      graphqlQuery = String.format("{\"query\":\"query ($org: String!, $team: String!, $cursor: String ) { organization(login: $org) { team(slug: $team) { name, repositories(first: 100, after: $cursor) { pageInfo { endCursor, hasNextPage }, edges { node { name, url } } } } } }\",\"variables\":{\"org\":\"%s\",\"team\":\"%s\", \"cursor\": \"%s\" }}", ownerName, teamSlug, repoCursor);
    } while (hasNextRepoPage);

    return allTeamRepos.stream()
        .flatMap(repositoriesResponse -> repositoriesResponse.getOrganization().getTeam().getRepositories().getEdges().stream())
        .map(RepositoriesResponse.Organization.Team.Repositories.Edges::getNode)
        .map(RepositoriesResponse.Organization.Team.Repositories.Edges.Node::getName)
        .toList();
  }

  private String getUsernameFromGithubToken(String accessCode) throws IOException {
    String graphqlQuery = String.format("{\"query\":\"query { viewer { login } }\"}");
    RequestBody body = RequestBody.create(graphqlQuery, MediaType.get("application/json; charset=utf-8"));
    Request request = new Request.Builder().url(baseUri + GITHUB_GRAPHQL_ENDPOINT).post(body).addHeader("Authorization", accessCode).build();
    logger.debug("Request: " + request.toString());
    Response response = client.newCall(request).execute();
    ObjectMapper mapper = new ObjectMapper();
    assert response.body() != null;
    return mapper.readValue(response.body().string(), new TypeReference<GraphqlResponse<GithubUserResponse>>(){}).getData().getViewer().getLogin();
  }


}