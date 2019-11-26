package com.csc301.songmicroservice;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class SongDalImpl implements SongDal {

	private final MongoTemplate db;
	private final MongoCollection collection;
	private final SongConverter converter;
	@Autowired
	public SongDalImpl(MongoTemplate mongoTemplate) {
		this.db = mongoTemplate;
		if (!db.collectionExists("songs")) {
			collection = this.db.createCollection("songs");
		}
		else{
			collection = db.getCollection("songs");

		}
		converter = new SongConverter();
	}

	@Override
	public DbQueryStatus addSong(Song songToAdd) {
		// TODO Auto-generated method stub
		///String songName = songToAdd.getSongName();
		///String songArtist = songToAdd.getSongArtistFullName();
		///String songAlbum = songToAdd.getSongAlbum();
		Document songDoc = converter.toDocument(songToAdd);
		DbQueryStatus dbQueryStatus = new DbQueryStatus("ok", DbQueryExecResult.QUERY_OK);
		dbQueryStatus.setData(songDoc);
		//db.insert(songToAdd, "songs");
		collection.insertOne(songDoc);
		return dbQueryStatus;
	}

	@Override
	public DbQueryStatus findSongById(String songId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DbQueryStatus getSongTitleById(String songId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DbQueryStatus deleteSongById(String songId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DbQueryStatus updateSongFavouritesCount(String songId, boolean shouldDecrement) {
		// TODO Auto-generated method stub
		return null;
	}
}