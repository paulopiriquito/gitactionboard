package de.otto.platform.gitactionboard.adapters.service.workflow;

import de.otto.platform.gitactionboard.adapters.service.ApiService;
import de.otto.platform.gitactionboard.adapters.service.workflow.WorkflowsResponse.WorkflowIdentifier;
import de.otto.platform.gitactionboard.domain.service.WorkflowService;
import de.otto.platform.gitactionboard.domain.workflow.Workflow;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GithubWorkflowService implements WorkflowService{
  protected final ApiService apiService;

  public GithubWorkflowService(ApiService apiService) {
    this.apiService = apiService;
  }

  @Async
    public CompletableFuture<List<Workflow>> fetchWorkflows(String repoName, String accessToken) {
        return CompletableFuture.supplyAsync(
                        () -> {
                            log.info("Fetching workflows for {} repository", repoName);

                            return getWorkflowIdentifiers(repoName, accessToken).stream()
                                    .map(workflowIdentifier -> buildWorkflow(repoName, workflowIdentifier))
                                    .toList();
                        })
                .exceptionally(
                        throwable -> {
                            log.error(throwable.getMessage(), throwable);
                            return Collections.emptyList();
                        });
    }

    private List<WorkflowIdentifier> getWorkflowIdentifiers(String repoName, String accessToken) {
        final String url = String.format("/%s/actions/workflows", repoName);

        return Optional.ofNullable(apiService.getForObject(url, accessToken, WorkflowsResponse.class))
                .map(WorkflowsResponse::getWorkflows)
                .orElse(Collections.emptyList())
                .stream()
                .filter(WorkflowIdentifier::isActive)
                .toList();
    }

    private Workflow buildWorkflow(String repoName, WorkflowIdentifier workflowIdentifier) {
        return Workflow.builder()
                .name(workflowIdentifier.getName())
                .id(workflowIdentifier.getId())
                .repoName(repoName)
                .build();
    }
}