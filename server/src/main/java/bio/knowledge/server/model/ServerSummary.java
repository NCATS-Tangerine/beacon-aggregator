package bio.knowledge.server.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
/**
 * ServerSummary
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2017-12-19T18:00:36.924-08:00")

public class ServerSummary   {
  @JsonProperty("id")
  private String id = null;

  @JsonProperty("idmap")
  private String idmap = null;

  @JsonProperty("frequency")
  private Integer frequency = null;

  @JsonProperty("beacon")
  private String beacon = null;

  public ServerSummary id(String id) {
    this.id = id;
    return this;
  }

   /**
   * the concept type (semantic group) 
   * @return id
  **/
  @ApiModelProperty(value = "the concept type (semantic group) ")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public ServerSummary idmap(String idmap) {
    this.idmap = idmap;
    return this;
  }

   /**
   * The URL to execute the exactmatches API call on the id
   * @return idmap
  **/
  @ApiModelProperty(value = "The URL to execute the exactmatches API call on the id")
  public String getIdmap() {
    return idmap;
  }

  public void setIdmap(String idmap) {
    this.idmap = idmap;
  }

  public ServerSummary frequency(Integer frequency) {
    this.frequency = frequency;
    return this;
  }

   /**
   * the number of instances of the specified type 
   * @return frequency
  **/
  @ApiModelProperty(value = "the number of instances of the specified type ")
  public Integer getFrequency() {
    return frequency;
  }

  public void setFrequency(Integer frequency) {
    this.frequency = frequency;
  }

  public ServerSummary beacon(String beacon) {
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
    ServerSummary serverSummary = (ServerSummary) o;
    return Objects.equals(this.id, serverSummary.id) &&
        Objects.equals(this.idmap, serverSummary.idmap) &&
        Objects.equals(this.frequency, serverSummary.frequency) &&
        Objects.equals(this.beacon, serverSummary.beacon);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, idmap, frequency, beacon);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ServerSummary {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    idmap: ").append(toIndentedString(idmap)).append("\n");
    sb.append("    frequency: ").append(toIndentedString(frequency)).append("\n");
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

