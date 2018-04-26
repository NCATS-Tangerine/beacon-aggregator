package bio.knowledge.server.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
/**
 * ServerConceptTypesByBeacon
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2018-04-02T12:58:15.341-07:00")

public class ServerConceptTypesByBeacon   {
  @JsonProperty("beacon")
  private Integer beacon = null;

  @JsonProperty("types")
  private List<ServerBeaconConceptType> types = new ArrayList<ServerBeaconConceptType>();

  public ServerConceptTypesByBeacon beacon(Integer beacon) {
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

  public ServerConceptTypesByBeacon types(List<ServerBeaconConceptType> types) {
    this.types = types;
    return this;
  }

  public ServerConceptTypesByBeacon addTypesItem(ServerBeaconConceptType typesItem) {
    this.types.add(typesItem);
    return this;
  }

   /**
   * Get types
   * @return types
  **/
  @ApiModelProperty(value = "")
  public List<ServerBeaconConceptType> getTypes() {
    return types;
  }

  public void setTypes(List<ServerBeaconConceptType> types) {
    this.types = types;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServerConceptTypesByBeacon serverConceptTypesByBeacon = (ServerConceptTypesByBeacon) o;
    return Objects.equals(this.beacon, serverConceptTypesByBeacon.beacon) &&
        Objects.equals(this.types, serverConceptTypesByBeacon.types);
  }

  @Override
  public int hashCode() {
    return Objects.hash(beacon, types);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ServerConceptTypesByBeacon {\n");
    
    sb.append("    beacon: ").append(toIndentedString(beacon)).append("\n");
    sb.append("    types: ").append(toIndentedString(types)).append("\n");
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
