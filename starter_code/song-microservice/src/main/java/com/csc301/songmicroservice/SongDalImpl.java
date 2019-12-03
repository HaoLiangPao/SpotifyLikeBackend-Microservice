package com.csc301.songmicroservice;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
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
	private ObjectId objectId;

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
		// convert the song object to a document object for data base addition
		Document songDoc = converter.toDocument(songToAdd);
		// interaction with database
		collection.insertOne(songDoc);

		// encapsulation of log message
		dbQueryStatus.setMessage("Addition is complete, song got added to the data base successfully");
		dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
		System.out.println(dbQueryStatus.getMessage());

		// update the Object ID to the song object
		Document docAdded = (Document) collection.find(songDoc).iterator().next();
		songToAdd.setId(docAdded.getObjectId("_id"));
		// add the updated song object as the data
		dbQueryStatus.setData(songToAdd);
		return dbQueryStatus;
	}

	@Override
	public DbQueryStatus findSongById(String songId) {
		// TODO Auto-generated method stub
		try {
			objectId = new ObjectId(songId);
		}
		catch (Exception e){
			dbQueryStatus.setMessage("The input songId is invalid");
			dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_WRONG_PARAMETER);
			return dbQueryStatus;
		}
		Hashtable queryPair = new Hashtable();
		queryPair.put("_id", objectId);
		Document query = new Document(queryPair);

		MongoCursor<Document> cursor = collection.find(query).iterator();
//		System.out.println("set is " + cursor.toString());
		if (cursor.hasNext()){
			Document songDocFound = cursor.next();
			Song songFound = converter.toSong(songDocFound);

			dbQueryStatus.setMessage("The song is successfully found in the database");
			dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
			dbQueryStatus.setData(songFound);
		}
		else {
			//when object id is not existing int he database.
			dbQueryStatus.setMessage("The song with id given is not found in the database");
			dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			dbQueryStatus.setData(null);
		}
		System.out.println(dbQueryStatus.getMessage());
		return dbQueryStatus;
	}

	@Override
	public DbQueryStatus getSongTitleById(String songId) {
		// TODO Auto-generated method stub
		ObjectId objectId = new ObjectId(songId);
		Hashtable queryPair = new Hashtable();
		queryPair.put("_id", objectId);
		Document query = new Document(queryPair);

		MongoCursor<Document> cursor = collection.find(query).iterator();
//		System.out.println("set is " + cursor.toString());
		if (cursor.hasNext()){
			Document songDocFound = cursor.next();
			Song songFound = converter.toSong(songDocFound);

			dbQueryStatus.setMessage("The song is successfully found in the database");
			dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
			dbQueryStatus.setData(songFound.getSongName());
		}
		else {
			//when object id is not existing int he database.
			dbQueryStatus.setMessage("The song with id given is not found in the database");
			dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			dbQueryStatus.setData(null);
		}
		System.out.println(dbQueryStatus.getMessage());
		return dbQueryStatus;
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
		dbQueryStatus.setData(null);
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