package com.csc301.songmicroservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Repository
public class SongDalImpl implements SongDal {

	private final MongoTemplate db;
	private final MongoCollection collection;
	private final SongConverter converter;
	private final RestTemplate restTemplate = new RestTemplate();
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
	public DbQueryStatus addSong(Map songParams) {
		// Method does the implementation of add song into database then return a DbQueryStatus
		// check if the parameters are all given
		if (songParams == null){
			dbQueryStatus.setMessage("parameters are missing, please double check the parameters");
			dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_BAD_REQUEST);
		}
		// if there are some parameters missing
		else {
			try {
				// create a song object
				String songName = (String) songParams.get("songName");
				String songArtist = (String) songParams.get("songArtistFullName");
				String songAlbum = (String) songParams.get("songAlbum");
				Song songToAdd = new Song(songName, songArtist, songAlbum);

				// convert the song object to a document object for data base addition
				Document songDoc = converter.toDocument(songToAdd);
				// interaction with database
				collection.insertOne(songDoc);

				// encapsulation of log message
				dbQueryStatus
						.setMessage("Addition is complete, song got added to the data base successfully");
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
				System.out.println(dbQueryStatus.getMessage());

				// update the Object ID to the song object
				Document docAdded = (Document) collection.find(songDoc).iterator().next();
				songToAdd.setId(docAdded.getObjectId("_id"));
				// add the updated song object as the data
				dbQueryStatus.setData(songToAdd);
			}
			catch (Exception e) {
				dbQueryStatus.setMessage("Song addition failed");
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
			}
		}
		System.out.println(dbQueryStatus.getMessage());
		return dbQueryStatus;
	}

	@Override
	public DbQueryStatus findSongById(String songId) {
		// Method does the implementation of find song given an id then return a DbQueryStatus
		// containing song info

		// check if the parameters are all given
		if (songId == null){
			dbQueryStatus.setMessage("parameters are missing, please double check the parameters");
			dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_BAD_REQUEST);
		}
		// if there are some parameters missing
		else {
			try {
				objectId = new ObjectId(songId);
			} catch (Exception e) {
				dbQueryStatus.setMessage("The input songId is invalid");
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_BAD_REQUEST);
				return dbQueryStatus;
			}
			try {
				Hashtable queryPair = new Hashtable();
				queryPair.put("_id", objectId);
				Document query = new Document(queryPair);

				MongoCursor<Document> cursor = collection.find(query).iterator();
//		System.out.println("set is " + cursor.toString());
				if (cursor.hasNext()) {
					Document songDocFound = cursor.next();
					Song songFound = converter.toSong(songDocFound);

					dbQueryStatus.setMessage("The song is successfully found in the database");
					dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
					dbQueryStatus.setData(songFound);
				} else {
					//when object id is not existing int he database.
					dbQueryStatus.setMessage("The song with id given is not found in the database");
					dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
					dbQueryStatus.setData(null);
				}
			}
			catch (Exception e) {
				dbQueryStatus.setMessage("Find Song failed");
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
			}
		}
		System.out.println(dbQueryStatus.getMessage());
		return dbQueryStatus;
	}

	@Override
	public DbQueryStatus getSongTitleById(String songId) {
		// Method does the implementation of get the title of song given the id then
		// return a DbQueryStatus

		// check if the parameters are all given
		if (songId == null){
			dbQueryStatus.setMessage("parameters are missing, please double check the parameters");
			dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_BAD_REQUEST);
		}
		// if there are some parameters missing
		else {
			try {
				objectId = new ObjectId(songId);
			} catch (Exception e) {
				dbQueryStatus.setMessage("The input songId is invalid");
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_BAD_REQUEST);
				return dbQueryStatus;
			}
			Hashtable queryPair = new Hashtable();
			queryPair.put("_id", objectId);
			Document query = new Document(queryPair);

			MongoCursor<Document> cursor = collection.find(query).iterator();
//		System.out.println("set is " + cursor.toString());
			if (cursor.hasNext()) {
				Document songDocFound = cursor.next();
				Song songFound = converter.toSong(songDocFound);

				dbQueryStatus
						.setMessage("The song is successfully found in the database, title is returned");
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
				dbQueryStatus.setData(songFound.getSongName());
			} else {
				//when object id is not existing int he database.
				dbQueryStatus.setMessage("The song with id given is not found in the database");
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				dbQueryStatus.setData(null);
			}
		}
		System.out.println(dbQueryStatus.getMessage());
		return dbQueryStatus;
	}

	@Override
	public DbQueryStatus deleteSongById(String songId) {
		// Method does the implementation of remove song from database then return a DbQueryStatus

		// check if the parameters are all given
		if (songId == null){
			dbQueryStatus.setMessage("parameters are missing, please double check the parameters");
			dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_BAD_REQUEST);
		}
		// if there are some parameters missing
		else {
			//store the id and change the type to be used in a mongodb query
			try {
				objectId = new ObjectId(songId);
			} catch (Exception e) {
				dbQueryStatus.setMessage("The input songId is invalid");
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_BAD_REQUEST);
				return dbQueryStatus;
			}
			Hashtable queryPair = new Hashtable();
			queryPair.put("_id", objectId);
			Document query = new Document(queryPair);
			// interact with database for deletion
			if (collection.deleteOne(query).getDeletedCount() != 0) {
				dbQueryStatus.setMessage("Log: SongMicroService-delete operation is completed");
				//result for server-client interaction
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);

				String profileMicroDelete = "http://localhost:3002/deleteAllSongsFromDb/{songId}";
				// URL parameters
				Map<String, String> urlParam = new HashMap<String, String>();
				urlParam.put("songId", songId);
				// Query parameters
				UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(profileMicroDelete);
				// communication with ProfileMicroService
				ResponseEntity<String> res = restTemplate
						.exchange(builder.buildAndExpand(urlParam).toUri(), HttpMethod.PUT,
								HttpEntity.EMPTY, String.class);
				ObjectMapper mapper = new ObjectMapper();
				try {
					JsonNode resJSON = mapper.readTree(res.getBody());
					JsonNode updateStatus = resJSON.get("status");
				} catch (IOException e) {
					dbQueryStatus.setMessage("something went wrong with profileMicroService");
					//result for server-client interaction
					dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
				}
			} else {
				dbQueryStatus.setMessage("Error Message: SongMicroService-the post is not found in the"
						+ " database, delete did not complete");
				//result for server-client interaction
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}
		}
		// log message no matter success or failure
		System.out.println(dbQueryStatus.getMessage());
		dbQueryStatus.setData(null);
		return dbQueryStatus;
	}

	@Override
	public DbQueryStatus updateSongFavouritesCount(String songId, String shouldDecrementString) {
		// Method does the implementation of update song's favorite number in database
		// then return a DbQueryStatus

		// check if the parameters are all given and shouldDecrement String is correct
		if (songId == null || shouldDecrementString == null){
			dbQueryStatus.setMessage("parameters are missing, please double check the parameters");
			dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_BAD_REQUEST);
			dbQueryStatus.setData(null);
			return dbQueryStatus;
		}
		// if some bad parameters are entered for shouldDecrement
		if (! (shouldDecrementString.equals("true") || shouldDecrementString.equals("false")) ) {
			dbQueryStatus.setMessage("parameters are missing, please double check the parameters");
			dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_BAD_REQUEST);
			dbQueryStatus.setData(null);
			return dbQueryStatus;
		}
		try {
			objectId = new ObjectId(songId);
		} catch (Exception e) {
			dbQueryStatus.setMessage("The input songId is invalid");
			dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_BAD_REQUEST);
			dbQueryStatus.setData(null);
			return dbQueryStatus;
		}
		// change the String input to boolean for further operation
		boolean shouldDecrement = Boolean.parseBoolean(shouldDecrementString);
		Hashtable queryPair = new Hashtable();
		queryPair.put("_id", objectId);
		Document query = new Document(queryPair);
		MongoCursor<Document> cursor = collection.find(query).iterator();
		// the song is found in the database
		if (cursor.hasNext()) {
			System.out.println("Log-SongMicroService: The song is successfully found in the database");
			Document songDocFound = cursor.next();
			Song songFound = converter.toSong(songDocFound);
			long currentFavo = songFound.getSongAmountFavourites();
			// if its favorite number should be decrement
			if (shouldDecrement) {
				// if currentFavo is at least 1
				if (currentFavo - 1 >= 0) {
					currentFavo -= 1;
				}
			}
			// if its favorite number should be increment
			else {
				currentFavo += 1;
			}
			// create filter document
			BasicDBObjectBuilder builder = BasicDBObjectBuilder.start().append("_id", objectId);
			Document filter = new Document(builder.get().toMap());
			// create favo document
			builder = BasicDBObjectBuilder.start().append("songAmountFavourites", currentFavo);
			Document favo = new Document(builder.get().toMap());
			// create update document
			builder = BasicDBObjectBuilder.start().append("$set", favo);
			Document update = new Document(builder.get().toMap());
			collection.updateOne(filter, update);

			dbQueryStatus.setMessage("The favorite number is successfully updated");
			dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
		} else {
			//when object id is not existing int he database.
			dbQueryStatus.setMessage("The song with id given is not found in the database");
			dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
		}
		System.out.println(dbQueryStatus.getMessage());
		dbQueryStatus.setData(null);
		return dbQueryStatus;
	}
}