package de.otto.platform.gitactionboard.domain.service;

import de.otto.platform.gitactionboard.domain.workflow.Workflow;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface OrganizationWorkflowService {
  CompletableFuture<List<Workflow>> fetchAllOrganizationWorkflows(String organizationName, String accessToken, String svcToken) throws IOException;
}