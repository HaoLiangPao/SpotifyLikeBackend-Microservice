package com.csc301.profilemicroservice;

import static org.neo4j.driver.v1.Values.parameters;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jndi.toolkit.url.Uri;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.neo4j.driver.v1.Transaction;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Repository
public class PlaylistDriverImpl implements PlaylistDriver {

	private final	RestTemplate restTemplate = new RestTemplate();
	Driver driver = ProfileMicroserviceApplication.driver;
	DbQueryStatus dbQueryStatus = new DbQueryStatus("defaul message", DbQueryExecResult.QUERY_OK);
	String queryStr;
	StatementResult result;

	public static void InitPlaylistDb() {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nPlaylist:playlist) ASSERT exists(nPlaylist.plName)";
				trans.run(queryStr);
				trans.success();
			}
			session.close();
		}
	}

	@Override
	public DbQueryStatus likeSong(String userName, String songId) {
		try (Session session = driver.session()){
			try	(Transaction trans = session.beginTransaction()){
				String plName = userName + "-favorite";

				// check if the songId is already in the database (request from MongoDB)
				String songMicroGet = "http://localhost:3001/getSongById/{songId}";
				// ResponseEntity<SongResponse> response = restTemplate.getForEntity(songMicroURL, SongResponse.class, songId);
				ResponseEntity<String> res = restTemplate.getForEntity(songMicroGet, String.class, songId);
				ObjectMapper mapper = new ObjectMapper();
				JsonNode resJSON = mapper.readTree(res.getBody());
				JsonNode songMongo = resJSON.get("data");

				// An assumption is made: all the status code is 200 and I have to use null type exception
				// to test if a song is existed in the MongoDB or not

				// if song is found in MongoDB by Song MicroService
				try {
					System.out.println("Log-profileMicroService: the song is found in MongoDB");
					System.out.println(songMongo.get("id"));

					// check if the song is existing in the Neo4j database
					queryStr = "Match (s:song {songId:$songID}) RETURN s";
					StatementResult result = trans.run(queryStr, parameters("songID", songId));
					trans.success();

					// create song node if the song is new in Neo4j database
					if (!result.hasNext()) {
						// create the song node if the song is not existed in the database before
						queryStr = "CREATE (s:song {songId:$songID}) RETURN s";
						result = trans.run(queryStr, parameters("songID", songId));
						trans.success();
						System.out.println("Log-profileMicroService: the new song is added in the database");
					}
					// check if the relationship is already existed in Neo4j database
					queryStr = "MATCH (p:profile)-[r1:created]->(l:playlist)-[r2:includes]->(s:song) WHERE "
							+ "l.plName = $plName AND s.songId = $songID RETURN r2";
					result = trans.run(queryStr, parameters("plName",
							plName, "songID", songId));
					trans.success();
					// if the relationship is new to Neo4j
					if (!result.hasNext()) {
						// create or add the profile node into the database
						queryStr = "MATCH (l:playlist),(s:song) WHERE l.plName = $plName AND s.songId = $songID"
								+ " MERGE (l)-[r2:includes]->(s) RETURN r2";
						result = trans.run(queryStr, parameters("plName",
								plName, "songID", songId));
						trans.success();
						// after the relationship is created in Neo4j, favourite number should be increment in
						// MongoDB through Song MicroService
						String songMicroUpdate = "http://localhost:3001/updateSongFavouritesCount/{songId}";
						// URL parameters
						Map<String, String> urlParam = new HashMap<String, String>();
						urlParam.put("songId", songId);
						// Query parameters
						UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(songMicroUpdate)
								.queryParam("shouldDecrement", "false");
						// communication with SongMicroService
						res = restTemplate.exchange(builder.buildAndExpand(urlParam).toUri(), HttpMethod.PUT,
								HttpEntity.EMPTY, String.class);
						mapper = new ObjectMapper();
						resJSON = mapper.readTree(res.getBody());
						JsonNode updateStatus = resJSON.get("status");
						// if Song MicroService updated the favourites count
						if (updateStatus.asText().equals("OK")) {
							// create relationship between the profile and a playlist
							dbQueryStatus.setMessage("Song is successfully added to user's favorite");
							dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
						}
						// if the Song MicroService went wrong for some reason
						else {
							dbQueryStatus.setMessage("Failed to communicate with our Song Database");
							dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
						}
					}
					else {
							dbQueryStatus.setMessage("Song is already in user's favourites");
							dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
					}
				}
				// the song is not found in MongoDB by Song MicroService
				catch (Exception e){
					dbQueryStatus.setMessage("Sorry, the Song is not found in our DataBase");
					dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				}
			}
			// exceptions for mapTree
			catch (IOException e) {
				dbQueryStatus.setMessage("Internal Server Error");
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
			}
			session.close();
		}
		System.out.println(dbQueryStatus.getMessage());
		return dbQueryStatus;
	}

	@Override
	public DbQueryStatus unlikeSong(String userName, String songId) {
		try (Session session = driver.session()){
			try	(Transaction trans = session.beginTransaction()){
				String plName = userName + "-favorite";

				// check if the songId is already in the database
				queryStr = "MATCH (p:profile)-[r1:created]->(l:playlist)-[r2:includes]->(s:song)"
						+ " WHERE p.userName = $userName AND s.songId = $songID RETURN r2";
				StatementResult result = trans.run(queryStr, parameters( "userName",
						userName, "songID", songId));
				trans.success();
				System.out.println("Log-ProfileMicroService: the relationship between the user and"
						+ " the song is existed");
				// if the relationship between the user and the song is existed
				if (result.hasNext()){
					// if the relationship does exist, delete it
					queryStr = "MATCH (p:profile)-[r1:created]->(l:playlist)-[r2:includes]->(s:song)"
							+ " WHERE p.userName = $userName AND s.songId = $songID DELETE r2";
					result = trans.run(queryStr, parameters( "userName",
							userName, "songID", songId));
					trans.success();
					// create relationship between the profile and a playlist
					dbQueryStatus.setMessage("Song is successfully deleted from user's favorite");
					dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
				}
				else {
					// if the relationship does not exist, return an error message
					dbQueryStatus.setMessage("Song is not included in user's favorite");
					dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				}
			}
			session.close();
		}
		System.out.println(dbQueryStatus.getMessage());
		return dbQueryStatus;
	}

	@Override
	public DbQueryStatus deleteSongFromDb(String songId) {

		return null;
	}
}
