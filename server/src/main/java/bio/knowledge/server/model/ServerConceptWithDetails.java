package bio.knowledge.server.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
/**
 * A single record of a given concept clique with details 
 */
@ApiModel(description = "A single record of a given concept clique with details ")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2017-12-19T18:00:36.924-08:00")

public class ServerConceptWithDetails   {
  @JsonProperty("clique")
  private String clique = null;

  @JsonProperty("name")
  private String name = null;

  @JsonProperty("type")
  private String type = null;

  @JsonProperty("taxon")
  private String taxon = null;

  @JsonProperty("aliases")
  private List<String> aliases = new ArrayList<String>();

  @JsonProperty("entries")
  private List<ServerConceptBeaconEntry> entries = new ArrayList<ServerConceptBeaconEntry>();

  public ServerConceptWithDetails clique(String clique) {
    this.clique = clique;
    return this;
  }

   /**
   * CURIE identifying the equivalent concept clique to which the concept belongs. 
   * @return clique
  **/
  @ApiModelProperty(value = "CURIE identifying the equivalent concept clique to which the concept belongs. ")
  public String getClique() {
    return clique;
  }

  public void setClique(String clique) {
    this.clique = clique;
  }

  public ServerConceptWithDetails name(String name) {
    this.name = name;
    return this;
  }

   /**
   * Canonical human readable name of the key concept of the clique 
   * @return name
  **/
  @ApiModelProperty(value = "Canonical human readable name of the key concept of the clique ")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ServerConceptWithDetails type(String type) {
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

  public ServerConceptWithDetails taxon(String taxon) {
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

  public ServerConceptWithDetails aliases(List<String> aliases) {
    this.aliases = aliases;
    return this;
  }

  public ServerConceptWithDetails addAliasesItem(String aliasesItem) {
    this.aliases.add(aliasesItem);
    return this;
  }

   /**
   * set of alias CURIES in the equivalent concept clique of the concept 
   * @return aliases
  **/
  @ApiModelProperty(value = "set of alias CURIES in the equivalent concept clique of the concept ")
  public List<String> getAliases() {
    return aliases;
  }

  public void setAliases(List<String> aliases) {
    this.aliases = aliases;
  }

  public ServerConceptWithDetails entries(List<ServerConceptBeaconEntry> entries) {
    this.entries = entries;
    return this;
  }

  public ServerConceptWithDetails addEntriesItem(ServerConceptBeaconEntry entriesItem) {
    this.entries.add(entriesItem);
    return this;
  }

   /**
   * List of details specifically harvested from beacons, indexed by beacon 
   * @return entries
  **/
  @ApiModelProperty(value = "List of details specifically harvested from beacons, indexed by beacon ")
  public List<ServerConceptBeaconEntry> getEntries() {
    return entries;
  }

  public void setEntries(List<ServerConceptBeaconEntry> entries) {
    this.entries = entries;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServerConceptWithDetails serverConceptWithDetails = (ServerConceptWithDetails) o;
    return Objects.equals(this.clique, serverConceptWithDetails.clique) &&
        Objects.equals(this.name, serverConceptWithDetails.name) &&
        Objects.equals(this.type, serverConceptWithDetails.type) &&
        Objects.equals(this.taxon, serverConceptWithDetails.taxon) &&
        Objects.equals(this.aliases, serverConceptWithDetails.aliases) &&
        Objects.equals(this.entries, serverConceptWithDetails.entries);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clique, name, type, taxon, aliases, entries);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ServerConceptWithDetails {\n");
    
    sb.append("    clique: ").append(toIndentedString(clique)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    taxon: ").append(toIndentedString(taxon)).append("\n");
    sb.append("    aliases: ").append(toIndentedString(aliases)).append("\n");
    sb.append("    entries: ").append(toIndentedString(entries)).append("\n");
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

