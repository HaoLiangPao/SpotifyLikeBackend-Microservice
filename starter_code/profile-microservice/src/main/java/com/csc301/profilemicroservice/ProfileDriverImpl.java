package com.csc301.profilemicroservice;

import static org.neo4j.driver.v1.Values.parameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import org.springframework.stereotype.Repository;
import org.neo4j.driver.v1.Transaction;

@Repository
public class ProfileDriverImpl implements ProfileDriver {

	Driver driver = ProfileMicroserviceApplication.driver;
	DbQueryStatus dbQueryStatus = new DbQueryStatus("default message", DbQueryExecResult.QUERY_OK);
  String queryStr;

	public static void InitProfileDb() {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.userName)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.password)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT nProfile.userName IS UNIQUE";
				trans.run(queryStr);
				trans.success();
			}
			session.close();
		}
	}
	
	@Override
  public DbQueryStatus createUserProfile(String userName, String fullName, String password) {
    try (Session session = driver.session()){
      try	(Transaction trans = session.beginTransaction()){
        // create or add the profile node into the database
        queryStr = "MERGE (p:profile{userName:$userName}) SET p.fullName = $fullName, p.password = "
            + "$password RETURN p.userName, p.fullName";
        StatementResult result = trans.run(queryStr, parameters("userName",
            userName, "fullName", fullName, "password", password));
        trans.success();
        System.out.println("Log-ProfileMicroService: profile is successfully created");
        //Get values from neo4j StatementResult object
        List<Record> records = result.list();
        Record record = records.get(0);
        Map recordMap = record.asMap();

        // create relationship between the profile and a playlist
        String plName = userName + "-favorites";
        queryStr = "MATCH (p {userName:$userName}) CREATE (p)-[r:created]->(l:playlist"
            + " {plName:$plName}) RETURN r";
        result = trans.run(queryStr, parameters("userName",
            userName, "plName", plName));
        trans.success();
        System.out.println("Log-ProfileMicroService: playlist associate to the profile is "
            + "successfully created as well");
	  // check if the parameters are all given
	  if (userName == null || fullName == null || password == null){
	    dbQueryStatus.setMessage("parameters are missing, please double check the parameters");
	    dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_BAD_REQUEST);
    }
	  // if there are some parameters missing
	  else {
      try (Session session = driver.session()) {
        try (Transaction trans = session.beginTransaction()) {
          // create or add the profile node into the database
          queryStr = "MERGE (p:profile{userName:$userName}) SET p.fullName = $fullName,"
              + " p.password = $password RETURN p.userName, p.fullName";
          StatementResult result = trans.run(queryStr, parameters("userName",
              userName, "fullName", fullName, "password", password));
          trans.success();
          System.out.println("Log-ProfileMicroService: profile is successfully created");

          // create relationship between the profile and a playlist
          String plName = userName + "-favorites";
          queryStr = "MATCH (p {userName:$userName}) CREATE (p)-[r:created]->(l:playlist"
              + " {plName:$plName}) RETURN r";
          result = trans.run(queryStr, parameters("userName",
              userName, "plName", plName));
          trans.success();
          System.out.println("Log-ProfileMicroService: playlist associate to the profile is "
              + "successfully created as well");

          dbQueryStatus.setMessage("profile is created and added to the database");
          dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
        }
        session.close();
      }
    }
    System.out.println(dbQueryStatus.getMessage());
    dbQueryStatus.setData(null);
    return dbQueryStatus;
  }

	@Override
	public DbQueryStatus followFriend(String userName, String friendUserName) {
    try (Session session = driver.session()){
      try	(Transaction trans = session.beginTransaction()){
        // create or add the profile node into the database
        queryStr = "MATCH (user:profile), (friend:profile) WHERE user.userName = $userName AND"
            + " friend.userName = $friendUserName MERGE (user)-[r:follows]->(friend) RETURN r";
        StatementResult result = trans.run(queryStr, parameters("userName",
            userName, "friendUserName", friendUserName));
        trans.success();

        // create relationship between the profile and a playlist
        dbQueryStatus.setMessage("Friend is successfully followed");
        dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
      }
    }
    System.out.println(dbQueryStatus.getMessage());
    dbQueryStatus.setData(null);
    return dbQueryStatus;
	}

	@Override
	public DbQueryStatus unfollowFriend(String userName, String friendUserName) {
    try (Session session = driver.session()){
      try	(Transaction trans = session.beginTransaction()){
        // create or add the profile node into the database
        queryStr = "MATCH (user)-[r:follows]->(friend) WHERE user.userName ="
            + " $userName AND friend.userName = $friendUserName DELETE r";
        StatementResult result = trans.run(queryStr, parameters("userName",
            userName, "friendUserName", friendUserName));
        trans.success();

        // create relationship between the profile and a playlist
        dbQueryStatus.setMessage("Friend is successfully unfollowed");
        dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
      }
    }
    System.out.println(dbQueryStatus.getMessage());
    dbQueryStatus.setData(null);
    return dbQueryStatus;
	}

	@Override
	public DbQueryStatus getAllSongFriendsLike(String userName) {
    try (Session session = driver.session()){
      try	(Transaction trans = session.beginTransaction()){
        // create or add the profile node into the database
        queryStr = "match (p:profile)-[r1:follows]->(p2:profile) where p.userName=$userName return collect(p2.userName) as friends";
        StatementResult result = trans.run(queryStr, parameters("userName",
            userName));
        trans.success();
        //Get values from neo4j StatementResult object
        List<Record> records = result.list();
        Record record = records.get(0);
        Map recordMap = record.asMap();
        JSONObject responseJSON = new JSONObject(recordMap);
        JSONObject outputJSON = new JSONObject();
        JSONArray friends = (JSONArray) responseJSON.get("friends");

        // iterate through the friend in friend list and get songs from their playlist
        for (int i=0; i < friends.length(); i++) {
          String friendName = (String) friends.get(i);
          queryStr = "match (p:profile)-[r1:created]->(l:playlist)-[r2:includes]->(s:song)"
              + " where p.userName=$friendName return collect(s.songName) as songs";
          result = trans.run(queryStr, parameters("friendName",
              friendName));
          trans.success();
          records = result.list();
          record = records.get(0);
          recordMap = record.asMap();
          JSONObject playlistJSON = new JSONObject(recordMap);
          outputJSON.put(friendName,playlistJSON.get("songs"));
        }

        // create relationship between the profile and a playlist
        dbQueryStatus.setMessage("All playlists are found from friends of this user");
        dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
        dbQueryStatus.setData(outputJSON.toMap());
      }
    }
    System.out.println(dbQueryStatus.getMessage());
    return dbQueryStatus;
	}
}
