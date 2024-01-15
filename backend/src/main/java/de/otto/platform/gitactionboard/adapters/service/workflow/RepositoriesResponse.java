package de.otto.platform.gitactionboard.adapters.service.workflow;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.List;

@Setter
@Getter
public class RepositoriesResponse {
  private Organization organization;

  @Setter
  @Getter
  public static class Organization {
    private Team team;

    @Setter
    @Getter
    public static class Team {
      private String name;
      private Repositories repositories;

      @Setter
      @Getter
      public static class Repositories {
        private PageInfo pageInfo;
        private List<Edges> edges;

        @Setter
        @Getter
        public static class PageInfo {
          private String endCursor;
          private boolean hasNextPage;
        }

        @Setter
        @Getter
        public static class Edges {
          private Node node;

          @Setter
          @Getter
          public static class Node {
            private String name;
            private String url;
          }
        }
      }
    }
  }
}