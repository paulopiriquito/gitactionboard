package de.otto.platform.gitactionboard.adapters.service;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GithubUserResponse {
    private Viewer viewer;

    @Getter
    @Setter
    public static class Viewer {
        private String login;
    }
}