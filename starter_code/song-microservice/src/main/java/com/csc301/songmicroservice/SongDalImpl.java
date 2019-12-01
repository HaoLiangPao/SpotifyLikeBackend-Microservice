package com.csc301.songmicroservice;

import com.mongodb.client.MongoCollection;
import java.util.Hashtable;
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
	private DbQueryStatus dbQueryStatus = new DbQueryStatus("new query status",
			DbQueryExecResult.QUERY_OK);

	@Autowired
	// singularity design
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
		// TODO: handel the case of internal problem
		//store the id and change the type to be used in a mongodb query
		ObjectId objectId = new ObjectId(songId);
		Hashtable queryPair = new Hashtable();
		queryPair.put("_id", objectId);
		Document query = new Document(queryPair);
		// interact with database for deletion
		if (collection.deleteOne(query).getDeletedCount() != 0){
			dbQueryStatus.setMessage("Log: SongMicroService-delete operation is completed");
			//result for server-client interaction
			dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
		}
		else  {
			dbQueryStatus.setMessage("Error Message: SongMicroService-the post is not found in the"
					+ " database, delete did not complete");
			//result for server-client interaction
			dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
		}
		// log message no matter success or failure
		System.out.println(dbQueryStatus.getMessage());
		return dbQueryStatus;
	}

	@Override
	public DbQueryStatus updateSongFavouritesCount(String songId, boolean shouldDecrement) {
		// TODO Auto-generated method stub
		return null;
	}
}