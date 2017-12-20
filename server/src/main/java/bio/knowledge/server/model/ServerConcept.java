package bio.knowledge.server.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
/**
 * ServerConcept
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2017-12-19T18:00:36.924-08:00")

public class ServerConcept   {
  @JsonProperty("clique")
  private String clique = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("type")
  private String type = null;

  @JsonProperty("taxon")
  private String taxon = null;

  public ServerConcept clique(String clique) {
    this.clique = clique;
    return this;
  }

   /**
   * CURIE identifying the inferred equivalent concept clique to which the concept belongs. This is assigned by an identifier precedence heuristic by the beacon-aggregator 
   * @return clique
  **/
  @ApiModelProperty(value = "CURIE identifying the inferred equivalent concept clique to which the concept belongs. This is assigned by an identifier precedence heuristic by the beacon-aggregator ")
  public String getClique() {
    return clique;
  }

  public void setClique(String clique) {
    this.clique = clique;
  }

  public ServerConcept name(String name) {
    this.name = name;
    return this;
  }

   /**
   * Canonical human readable name of the concept 
   * @return name
  **/
  @ApiModelProperty(value = "Canonical human readable name of the concept ")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ServerConcept type(String type) {
    this.type = type;
    return this;
  }

   /**
   * Concept semantic type as a CURIE into a data type ontology 
   * @return type
  **/
  @ApiModelProperty(value = "Concept semantic type as a CURIE into a data type ontology ")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public ServerConcept taxon(String taxon) {
    this.taxon = taxon;
    return this;
  }

   /**
   * NCBI identifier of Taxon associated the concept (if applicable) 
   * @return taxon
  **/
  @ApiModelProperty(value = "NCBI identifier of Taxon associated the concept (if applicable) ")
  public String getTaxon() {
    return taxon;
  }

  public void setTaxon(String taxon) {
    this.taxon = taxon;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServerConcept serverConcept = (ServerConcept) o;
    return Objects.equals(this.clique, serverConcept.clique) &&
        Objects.equals(this.name, serverConcept.name) &&
        Objects.equals(this.type, serverConcept.type) &&
        Objects.equals(this.taxon, serverConcept.taxon);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clique, name, type, taxon);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ServerConcept {\n");
    
    sb.append("    clique: ").append(toIndentedString(clique)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    taxon: ").append(toIndentedString(taxon)).append("\n");
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

