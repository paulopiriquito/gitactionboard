package de.otto.platform.gitactionboard.domain.service;

import de.otto.platform.gitactionboard.domain.service.notifications.NotificationsService;
import de.otto.platform.gitactionboard.domain.workflow.JobDetails;
import de.otto.platform.gitactionboard.domain.workflow.Workflow;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PipelineService {
    private final OrganizationWorkflowService workflowService;
    private final JobDetailsService jobDetailsService;
    private final NotificationsService notificationsService;
    private final String organizationName;
    private final String svcToken;

    public PipelineService(
        OrganizationWorkflowService workflowService,
        JobDetailsService jobDetailsService,
        NotificationsService notificationsService,
        @Qualifier("ownerName") String organizationName,
        @Qualifier("svcToken") String svcToken
    ) {
        this.workflowService = workflowService;
        this.jobDetailsService = jobDetailsService;
        this.notificationsService = notificationsService;
        this.organizationName = organizationName;
        this.svcToken = svcToken;
    }

    @Cacheable(cacheNames = "jobDetails", sync = true, keyGenerator = "sharedCacheKeyGenerator")
    public List<JobDetails> fetchJobs(String accessToken) {
        final List<Workflow> workflows = fetchWorkflows(accessToken);

        final List<JobDetails> jobDetails = fetchJobs(workflows);

        notificationsService.sendNotificationsForWorkflowJobs(jobDetails);

        return jobDetails;
    }

    private List<JobDetails> fetchJobs(List<Workflow> workflows) {
        return workflows.stream()
            .parallel()
            .map(
                workflow -> {
                    log.info("Fetching job details for {}", workflow);
                    return jobDetailsService.fetchJobDetails(workflow, svcToken);
                })
            .map(CompletableFuture::join)
            .flatMap(Collection::stream)
            .toList();
    }

    private List<Workflow> fetchWorkflows(String accessToken) {
        log.info("Fetching workflows for {} organization", organizationName);

        try {
            return workflowService.fetchAllOrganizationWorkflows(organizationName, accessToken, svcToken)
                .join();
        } catch (IOException e) {
            log.error("Error fetching workflows for {} organization", organizationName, e);
            throw new RuntimeException(e);
        }
    }
}