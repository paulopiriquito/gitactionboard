package de.otto.platform.gitactionboard.adapters.service.workflow;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CollaboratorResponse {
  private Organization organization;

  @Getter
  @Setter
  public static class Organization {
    private Repository repository;

    @Getter
    @Setter
    public static class Repository {
      private String name;
      private Collaborators collaborators;

      @Getter
      @Setter
      public static class Collaborators {
        private List<Edges> edges;

        @Getter
        @Setter
        public static class Edges {
          private Node node;

          @Getter
          @Setter
          public static class Node {
            private String login;
          }
        }
      }
    }
  }
}