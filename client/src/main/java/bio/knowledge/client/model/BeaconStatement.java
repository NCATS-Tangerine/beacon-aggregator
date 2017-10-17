/*
 * Translator Knowledge Beacon API
 * This is the Translator Knowledge Beacon Aggregator web service application programming interface (API). 
 *
 * OpenAPI spec version: 1.0.14
 * Contact: richard@starinformatics.com
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package bio.knowledge.client.model;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;

import io.swagger.annotations.ApiModelProperty;

/**
 * BeaconStatement
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2017-10-10T17:05:39.215-07:00")
public class BeaconStatement {
  @SerializedName("id")
  private String id = null;

  @SerializedName("subject")
  private BeaconStatementSubject subject = null;

  @SerializedName("predicate")
  private BeaconStatementPredicate predicate = null;

  @SerializedName("object")
  private BeaconStatementObject object = null;

  public BeaconStatement id(String id) {
    this.id = id;
    return this;
  }

   /**
   * CURIE-encoded identifier for statement (can be used to retrieve associated evidence)
   * @return id
  **/
  @ApiModelProperty(example = "null", value = "CURIE-encoded identifier for statement (can be used to retrieve associated evidence)")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public BeaconStatement subject(BeaconStatementSubject subject) {
    this.subject = subject;
    return this;
  }

   /**
   * Get subject
   * @return subject
  **/
  @ApiModelProperty(example = "null", value = "")
  public BeaconStatementSubject getSubject() {
    return subject;
  }

  public void setSubject(BeaconStatementSubject subject) {
    this.subject = subject;
  }

  public BeaconStatement predicate(BeaconStatementPredicate predicate) {
    this.predicate = predicate;
    return this;
  }

   /**
   * Get predicate
   * @return predicate
  **/
  @ApiModelProperty(example = "null", value = "")
  public BeaconStatementPredicate getPredicate() {
    return predicate;
  }

  public void setPredicate(BeaconStatementPredicate predicate) {
    this.predicate = predicate;
  }

  public BeaconStatement object(BeaconStatementObject object) {
    this.object = object;
    return this;
  }

   /**
   * Get object
   * @return object
  **/
  @ApiModelProperty(example = "null", value = "")
  public BeaconStatementObject getObject() {
    return object;
  }

  public void setObject(BeaconStatementObject object) {
    this.object = object;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BeaconStatement statement = (BeaconStatement) o;
    return Objects.equals(this.id, statement.id) &&
        Objects.equals(this.subject, statement.subject) &&
        Objects.equals(this.predicate, statement.predicate) &&
        Objects.equals(this.object, statement.object);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, subject, predicate, object);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BeaconStatement {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    subject: ").append(toIndentedString(subject)).append("\n");
    sb.append("    predicate: ").append(toIndentedString(predicate)).append("\n");
    sb.append("    object: ").append(toIndentedString(object)).append("\n");
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

