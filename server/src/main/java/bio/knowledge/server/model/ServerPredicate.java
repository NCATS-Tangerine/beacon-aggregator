package bio.knowledge.server.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
/**
 * ServerPredicate
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2017-12-19T18:00:36.924-08:00")

public class ServerPredicate   {
  @JsonProperty("name")
  private String name = null;

  @JsonProperty("beacons")
  private List<ServerPredicateBeacon> beacons = new ArrayList<ServerPredicateBeacon>();

  public ServerPredicate name(String name) {
    this.name = name;
    return this;
  }

   /**
   * exact unique human readable name of predicate relation 
   * @return name
  **/
  @ApiModelProperty(value = "exact unique human readable name of predicate relation ")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ServerPredicate beacons(List<ServerPredicateBeacon> beacons) {
    this.beacons = beacons;
    return this;
  }

  public ServerPredicate addBeaconsItem(ServerPredicateBeacon beaconsItem) {
    this.beacons.add(beaconsItem);
    return this;
  }

   /**
   * list of metadata for beacons that support the use of this predicate relation 
   * @return beacons
  **/
  @ApiModelProperty(value = "list of metadata for beacons that support the use of this predicate relation ")
  public List<ServerPredicateBeacon> getBeacons() {
    return beacons;
  }

  public void setBeacons(List<ServerPredicateBeacon> beacons) {
    this.beacons = beacons;
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
    return Objects.equals(this.name, serverPredicate.name) &&
        Objects.equals(this.beacons, serverPredicate.beacons);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, beacons);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ServerPredicate {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    beacons: ").append(toIndentedString(beacons)).append("\n");
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

