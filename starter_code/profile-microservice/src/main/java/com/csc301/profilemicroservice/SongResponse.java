package com.csc301.profilemicroservice;

import java.io.Serializable;

public class SongResponse implements Serializable {

  private String songId;

  public String getSongId() {
    return songId;
  }

  public void setSongId(String songId) {
    this.songId = songId;
  }
}
