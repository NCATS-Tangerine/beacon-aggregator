package bio.knowledge.server.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
/**
 * ServerStatement
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2017-12-19T18:00:36.924-08:00")

public class ServerStatement   {
  @JsonProperty("id")
  private String id = null;

  @JsonProperty("subject")
  private ServerStatementSubject subject = null;

  @JsonProperty("predicate")
  private ServerStatementPredicate predicate = null;

  @JsonProperty("object")
  private ServerStatementObject object = null;

  @JsonProperty("beacon")
  private String beacon = null;

  public ServerStatement id(String id) {
    this.id = id;
    return this;
  }

   /**
   * CURIE-encoded identifier for statement (can be used to retrieve associated evidence)
   * @return id
  **/
  @ApiModelProperty(value = "CURIE-encoded identifier for statement (can be used to retrieve associated evidence)")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public ServerStatement subject(ServerStatementSubject subject) {
    this.subject = subject;
    return this;
  }

   /**
   * Get subject
   * @return subject
  **/
  @ApiModelProperty(value = "")
  public ServerStatementSubject getSubject() {
    return subject;
  }

  public void setSubject(ServerStatementSubject subject) {
    this.subject = subject;
  }

  public ServerStatement predicate(ServerStatementPredicate predicate) {
    this.predicate = predicate;
    return this;
  }

   /**
   * Get predicate
   * @return predicate
  **/
  @ApiModelProperty(value = "")
  public ServerStatementPredicate getPredicate() {
    return predicate;
  }

  public void setPredicate(ServerStatementPredicate predicate) {
    this.predicate = predicate;
  }

  public ServerStatement object(ServerStatementObject object) {
    this.object = object;
    return this;
  }

   /**
   * Get object
   * @return object
  **/
  @ApiModelProperty(value = "")
  public ServerStatementObject getObject() {
    return object;
  }

  public void setObject(ServerStatementObject object) {
    this.object = object;
  }

  public ServerStatement beacon(String beacon) {
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
    ServerStatement serverStatement = (ServerStatement) o;
    return Objects.equals(this.id, serverStatement.id) &&
        Objects.equals(this.subject, serverStatement.subject) &&
        Objects.equals(this.predicate, serverStatement.predicate) &&
        Objects.equals(this.object, serverStatement.object) &&
        Objects.equals(this.beacon, serverStatement.beacon);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, subject, predicate, object, beacon);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ServerStatement {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    subject: ").append(toIndentedString(subject)).append("\n");
    sb.append("    predicate: ").append(toIndentedString(predicate)).append("\n");
    sb.append("    object: ").append(toIndentedString(object)).append("\n");
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

