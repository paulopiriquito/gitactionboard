package de.otto.platform.gitactionboard.adapters.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GraphqlResponse<T> {

  @JsonProperty("data")
  private T data;

  @JsonProperty("errors")
  private String[] errors;

  @JsonProperty("message")
  private String message;

  @JsonProperty("documentation_url")
  private String documentationUrl;
}