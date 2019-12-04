package com.csc301.songmicroservice;

import java.util.Map;

public interface SongDal {
	DbQueryStatus addSong(Map songToAdd);
	DbQueryStatus findSongById(String songId);
	DbQueryStatus getSongTitleById(String songId);
	DbQueryStatus deleteSongById(String songId);
	DbQueryStatus updateSongFavouritesCount(String songId, String shouldDecrement);
}
