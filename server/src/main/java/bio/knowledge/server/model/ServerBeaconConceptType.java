package bio.knowledge.server.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
/**
 * ServerBeaconConceptType
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2018-03-12T16:26:47.889-07:00")

public class ServerBeaconConceptType   {
  @JsonProperty("beacon")
  private Integer beacon = null;

  @JsonProperty("id")
  private String id = null;

  @JsonProperty("frequency")
  private Integer frequency = null;

  public ServerBeaconConceptType beacon(Integer beacon) {
    this.beacon = beacon;
    return this;
  }

   /**
   * aggregator index identifier of the given beacon 
   * @return beacon
  **/
  @ApiModelProperty(value = "aggregator index identifier of the given beacon ")
  public Integer getBeacon() {
    return beacon;
  }

  public void setBeacon(Integer beacon) {
    this.beacon = beacon;
  }

  public ServerBeaconConceptType id(String id) {
    this.id = id;
    return this;
  }

   /**
   * unique CURIE-encoded identifier of the given concept type, as used by the given beacon 
   * @return id
  **/
  @ApiModelProperty(value = "unique CURIE-encoded identifier of the given concept type, as used by the given beacon ")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public ServerBeaconConceptType frequency(Integer frequency) {
    this.frequency = frequency;
    return this;
  }

   /**
   * the number of instances of the specified concept type is used in statements within the given beacon 
   * @return frequency
  **/
  @ApiModelProperty(value = "the number of instances of the specified concept type is used in statements within the given beacon ")
  public Integer getFrequency() {
    return frequency;
  }

  public void setFrequency(Integer frequency) {
    this.frequency = frequency;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServerBeaconConceptType serverBeaconConceptType = (ServerBeaconConceptType) o;
    return Objects.equals(this.beacon, serverBeaconConceptType.beacon) &&
        Objects.equals(this.id, serverBeaconConceptType.id) &&
        Objects.equals(this.frequency, serverBeaconConceptType.frequency);
  }

  @Override
  public int hashCode() {
    return Objects.hash(beacon, id, frequency);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ServerBeaconConceptType {\n");
    
    sb.append("    beacon: ").append(toIndentedString(beacon)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    frequency: ").append(toIndentedString(frequency)).append("\n");
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

