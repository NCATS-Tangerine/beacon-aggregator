package bio.knowledge.server.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
/**
 * Predicate
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2017-10-11T17:55:45.568-07:00")

public class ServerPredicate   {
  @JsonProperty("id")
  private String id = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("beacon")
  private String beacon = null;

  public ServerPredicate id(String id) {
    this.id = id;
    return this;
  }

   /**
   * CURIE-encoded identifier of predicate resource 
   * @return id
  **/
  @ApiModelProperty(value = "CURIE-encoded identifier of predicate resource ")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public ServerPredicate name(String name) {
    this.name = name;
    return this;
  }

   /**
   * human readable name of predicate 
   * @return name
  **/
  @ApiModelProperty(value = "human readable name of predicate ")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ServerPredicate beacon(String beacon) {
    this.beacon = beacon;
    return this;
  }

   /**
   * beacon ID 
   * @return beacon
  **/
  @ApiModelProperty(value = "beacon ID ")
  public String getBeacon() {
    return beacon;
  }

  public void setBeacon(String beacon) {
    this.beacon = beacon;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServerPredicate serverPredicate = (ServerPredicate) o;
    return Objects.equals(this.id, serverPredicate.id) &&
        Objects.equals(this.name, serverPredicate.name) &&
        Objects.equals(this.beacon, serverPredicate.beacon);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, beacon);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Predicate {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    beacon: ").append(toIndentedString(beacon)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}