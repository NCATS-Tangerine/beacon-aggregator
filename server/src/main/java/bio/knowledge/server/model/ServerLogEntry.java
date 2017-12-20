package bio.knowledge.server.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
/**
 * ServerLogEntry
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2017-12-19T18:00:36.924-08:00")

public class ServerLogEntry   {
  @JsonProperty("timestamp")
  private String timestamp = null;

  @JsonProperty("beacon")
  private String beacon = null;

  @JsonProperty("query")
  private String query = null;

  @JsonProperty("message")
  private String message = null;

  public ServerLogEntry timestamp(String timestamp) {
    this.timestamp = timestamp;
    return this;
  }

   /**
   * timestamp 
   * @return timestamp
  **/
  @ApiModelProperty(value = "timestamp ")
  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  public ServerLogEntry beacon(String beacon) {
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

  public ServerLogEntry query(String query) {
    this.query = query;
    return this;
  }

   /**
   * URL of the API call executed by the aggregator 
   * @return query
  **/
  @ApiModelProperty(value = "URL of the API call executed by the aggregator ")
  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public ServerLogEntry message(String message) {
    this.message = message;
    return this;
  }

   /**
   * error message 
   * @return message
  **/
  @ApiModelProperty(value = "error message ")
  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServerLogEntry serverLogEntry = (ServerLogEntry) o;
    return Objects.equals(this.timestamp, serverLogEntry.timestamp) &&
        Objects.equals(this.beacon, serverLogEntry.beacon) &&
        Objects.equals(this.query, serverLogEntry.query) &&
        Objects.equals(this.message, serverLogEntry.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(timestamp, beacon, query, message);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ServerLogEntry {\n");
    
    sb.append("    timestamp: ").append(toIndentedString(timestamp)).append("\n");
    sb.append("    beacon: ").append(toIndentedString(beacon)).append("\n");
    sb.append("    query: ").append(toIndentedString(query)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
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

