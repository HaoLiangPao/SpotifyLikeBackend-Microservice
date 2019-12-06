package com.csc301.profilemicroservice;

import java.io.Serializable;
import org.json.JSONObject;

public class SongResponse implements Serializable {

  private String path;
  private JSONObject data;
  private String message;
  private String status;

  public JSONObject getData() {
    return data;
  }

  public String getMessage() {
    return message;
  }

  public String getPath() {
    return path;
  }

  public String getStatus() {
    return status;
  }

  public void setData(JSONObject data) {
    this.data = data;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
