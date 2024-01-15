package de.otto.platform.gitactionboard.adapters.service.workflow;

import de.otto.platform.gitactionboard.adapters.service.ApiService;
import de.otto.platform.gitactionboard.adapters.service.GithubGraphqlClient;
import de.otto.platform.gitactionboard.domain.service.OrganizationWorkflowService;
import de.otto.platform.gitactionboard.domain.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;


@Service
@Slf4j
public class GithubOrganizationWorkflowService extends GithubWorkflowService implements OrganizationWorkflowService {

    Logger logger = org.slf4j.LoggerFactory.getLogger(GithubOrganizationWorkflowService.class);

    protected final GithubGraphqlClient githubGraphqlClient;

    public GithubOrganizationWorkflowService(ApiService apiService, GithubGraphqlClient githubGraphqlClient) {
        super(apiService);
        this.githubGraphqlClient = githubGraphqlClient;
    }

    @Override
    public CompletableFuture<List<Workflow>> fetchAllOrganizationWorkflows(String organizationName, String accessToken, String svcToken) throws IOException {
        CompletableFuture<List<String>> repositories = CompletableFuture.completedFuture(githubGraphqlClient.getUserRepositoriesInOrganization(accessToken));
        return repositories.thenCompose(repos -> {
            var workflowFutures = repos.stream()
                    .map(repo -> fetchWorkflows(repo, svcToken))
                    .toList();
            return CompletableFuture.allOf(workflowFutures.toArray(CompletableFuture[]::new))
                    .thenApply(v -> workflowFutures.stream()
                            .map(CompletableFuture::join)
                            .flatMap(List::stream)
                            .toList());
        });
    }
}