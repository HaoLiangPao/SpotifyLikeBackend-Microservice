package com.csc301.profilemicroservice;

import static org.neo4j.driver.v1.Values.parameters;

import java.util.List;
import java.util.Map;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.springframework.stereotype.Repository;
import org.neo4j.driver.v1.Transaction;

@Repository
public class PlaylistDriverImpl implements PlaylistDriver {

	Driver driver = ProfileMicroserviceApplication.driver;
	DbQueryStatus dbQueryStatus = new DbQueryStatus("defaul message", DbQueryExecResult.QUERY_OK);
	String queryStr;

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

				// check if the songId is already in the database
				queryStr = "MATCH (s:song) WHERE s.songId = $songID return s";
				StatementResult result = trans.run(queryStr, parameters( "songID", songId));
				trans.success();
				// create the song node if the song is not existed in the database before
				if (!result.hasNext()) {
					queryStr = "CREATE (s:song {songId:$songID}) RETURN s";
					result = trans.run(queryStr, parameters("songID", songId));
					trans.success();
					System.out.println("Log-profileMicroService: the new song is added in the database");
				}
				// create or add the profile node into the database
				queryStr = "MATCH (l:playlist),(s:song) WHERE l.plName = $plName AND s.songId = $songID"
						+ " MERGE (l)-[r:includes]->(s) RETURN r";
				result = trans.run(queryStr, parameters("plName",
						plName, "songID", songId));
				trans.success();

				// create relationship between the profile and a playlist
				dbQueryStatus.setMessage("Song is successfully added to user's favorite");
				dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
			}
		}
		System.out.println(dbQueryStatus.getMessage());
		return dbQueryStatus;
	}

	@Override
	public DbQueryStatus unlikeSong(String userName, String songId) {
		
		return null;
	}

	@Override
	public DbQueryStatus deleteSongFromDb(String songId) {
		
		return null;
	}
}
