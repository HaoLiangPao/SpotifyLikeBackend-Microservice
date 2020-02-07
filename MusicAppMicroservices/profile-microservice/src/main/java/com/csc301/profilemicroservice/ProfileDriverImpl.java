package com.csc301.profilemicroservice;

import static org.neo4j.driver.v1.Values.parameters;

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
	  // Method does implementation of create profile and return QbQueryStatus
	  // check if the parameters are all given

	  if (userName == null || fullName == null || password == null){
	    dbQueryStatus.setMessage("parameters are missing, please double check the parameters");
	    dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_BAD_REQUEST);
    }
	  // if there are some parameters missing
	  else {
      try (Session session = driver.session()) {
        try (Transaction trans = session.beginTransaction()) {
          // check if the profile is existed in Neo4j
          queryStr = "MATCH (user:profile) WHERE user.userName ="
              + " $userName RETURN user";
          StatementResult result = trans.run(queryStr, parameters("userName",
              userName));
          trans.success();
          // if the profile is not existed
          if (!result.hasNext()){
            // create or add the profile node into the database
            queryStr = "MERGE (p:profile{userName:$userName}) SET p.fullName = $fullName,"
                + " p.password = $password RETURN p.userName, p.fullName";
            result = trans.run(queryStr, parameters("userName",
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
          else {
            dbQueryStatus.setMessage("profile is already existed in the database");
            dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_OK_EXISTED);
          }
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
	  // Method does implementation of follow relationship and return QbQueryStatus
    // check if the parameters are all given, and two names are not equal

    if (userName == null || friendUserName == null || userName.equals(friendUserName)){
      dbQueryStatus.setMessage("parameters are missing / or given names are the same,"
          + " please double check the parameters");
      dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_BAD_REQUEST);
    }
    // if there are some parameters missing
    else {
      try (Session session = driver.session()) {
        try (Transaction trans = session.beginTransaction()) {
          // check user
          queryStr = "MATCH (user:profile)WHERE user.userName ="
              + " $userName RETURN user";
          StatementResult userFound = trans.run(queryStr, parameters("userName", userName));
          trans.success();
          // check friend
          queryStr = "MATCH (friend:profile)WHERE friend.userName ="
              + " $friendUserName RETURN friend";
          StatementResult friendFound = trans.run(queryStr, parameters( "friendUserName", friendUserName));
          trans.success();
          // we have user and friend in the database
          if (userFound.hasNext() && friendFound.hasNext()) {
            // check if the relationship is existed in Neo4j
            queryStr = "MATCH (user:profile)-[r:follows]->(friend:profile) WHERE user.userName ="
                + " $userName AND friend.userName = $friendUserName RETURN r";
            StatementResult result = trans.run(queryStr, parameters("userName",
                userName, "friendUserName", friendUserName));
            trans.success();

            // if the relationship is existed in Neo4j
            if (!result.hasNext()) {
              // create or add the relationship node into the database
              queryStr =
                  "MATCH (user:profile), (friend:profile) WHERE user.userName = $userName AND"
                      + " friend.userName = $friendUserName MERGE (user)-[r:follows]->(friend) RETURN r";
              result = trans.run(queryStr, parameters("userName",
                  userName, "friendUserName", friendUserName));
              trans.success();

              // create relationship between the profile and a playlist
              dbQueryStatus.setMessage("Friend is successfully followed");
              dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
            } else {
              // create relationship between the profile and a playlist
              dbQueryStatus.setMessage("Friendship is existed");
              dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_OK_EXISTED);
            }
          }
          else {
            // create relationship between the profile and a playlist
            dbQueryStatus.setMessage("Either user or friend is not found in database");
            dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
          }
        }
      }
    }
    System.out.println(dbQueryStatus.getMessage());
    dbQueryStatus.setData(null);
    return dbQueryStatus;
	}

	@Override
	public DbQueryStatus unfollowFriend(String userName, String friendUserName) {
	  // Method does implementation of unfollow relationship and return QbQueryStatus
    // check if the parameters are all given

    if (userName == null || friendUserName == null || userName == friendUserName){
      dbQueryStatus.setMessage("parameters are missing, please double check the parameters");
      dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_BAD_REQUEST);
    }
    // if there are some parameters missing
    else {
      try (Session session = driver.session()) {
        try (Transaction trans = session.beginTransaction()) {
          // check user
          queryStr = "MATCH (user:profile)WHERE user.userName ="
              + " $userName RETURN user";
          StatementResult userFound = trans.run(queryStr, parameters("userName", userName));
          trans.success();
          // check friend
          queryStr = "MATCH (friend:profile)WHERE friend.userName ="
              + " $friendUserName RETURN friend";
          StatementResult friendFound = trans.run(queryStr, parameters( "friendUserName", friendUserName));
          trans.success();
          // we have user and friend in the database
          if (userFound.hasNext() && friendFound.hasNext()) {
            // check if the relationship is existed in Neo4j
            queryStr = "MATCH (user:profile)-[r:follows]->(friend:profile) WHERE user.userName ="
                + " $userName AND friend.userName = $friendUserName RETURN r";
            StatementResult result = trans.run(queryStr, parameters("userName",
                userName, "friendUserName", friendUserName));
            trans.success();
            // if the relationship is existed in Neo4j
            if (result.hasNext()) {
              // create or add the profile node into the database
              queryStr = "MATCH (user)-[r:follows]->(friend) WHERE user.userName ="
                  + " $userName AND friend.userName = $friendUserName DELETE r";
              result = trans.run(queryStr, parameters("userName",
                  userName, "friendUserName", friendUserName));
              trans.success();
              // create relationship between the profile and a playlist
              dbQueryStatus.setMessage("Friend is successfully unfollowed");
              dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
            } else {
              // create relationship between the profile and a playlist
              dbQueryStatus.setMessage("Friendship is not existed in database");
              dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
            }
          }
          else {
            // create relationship between the profile and a playlist
            dbQueryStatus.setMessage("Either user or friend is not found in database");
            dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
          }
        }
      }
    }
    System.out.println(dbQueryStatus.getMessage());
    dbQueryStatus.setData(null);
    return dbQueryStatus;
	}

	@Override
	public DbQueryStatus getAllSongFriendsLike(String userName) {
	  // Method does implementation of get all song of the given user's friend like and
    // return QbQueryStatus check if the parameters are all given

    if (userName == null){
      dbQueryStatus.setMessage("parameters are missing, please double check the parameters");
      dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_BAD_REQUEST);
    }
    // if there are some parameters missing
    else {
      try (Session session = driver.session()) {
        try (Transaction trans = session.beginTransaction()) {
          // check user
          queryStr = "MATCH (user:profile)WHERE user.userName ="
              + " $userName RETURN user";
          StatementResult userFound = trans.run(queryStr, parameters("userName", userName));
          trans.success();
          // only execute when the user is found
          if (userFound.hasNext()) {
            // create or add the profile node into the database
            queryStr = "match (p:profile)-[r1:follows]->(p2:profile) where p.userName=$userName return collect(p2.userName) as friends";
            StatementResult result = trans.run(queryStr, parameters("userName",
                userName));
            trans.success();
            // if the user is in the database
            if (result.hasNext()) {
              //Get values from neo4j StatementResult object
              List<Record> records = result.list();
              Record record = records.get(0);
              Map recordMap = record.asMap();
              JSONObject responseJSON = new JSONObject(recordMap);
              JSONObject outputJSON = new JSONObject();
              JSONArray friends = (JSONArray) responseJSON.get("friends");
              // iterate through the friend in friend list and get songs from their playlist
              for (int i = 0; i < friends.length(); i++) {
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
                outputJSON.put(friendName, playlistJSON.get("songs"));
              }
              // create relationship between the profile and a playlist
              dbQueryStatus.setMessage("All playlists are found from friends of this user");
              dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
              dbQueryStatus.setData(outputJSON.toMap());
            } else {
              // create relationship between the profile and a playlist
              dbQueryStatus.setMessage("the user is not found in the database");
              dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
              dbQueryStatus.setData(null);
            }
          }
          else {
            // create relationship between the profile and a playlist
            dbQueryStatus.setMessage("the user is not found in the database");
            dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
            dbQueryStatus.setData(null);
          }
        }
      }
      catch (Exception e) {
        // create relationship between the profile and a playlist
        dbQueryStatus.setMessage("Server is out of Service");
        dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_ERROR_GENERIC);
        dbQueryStatus.setData(null);
        return dbQueryStatus;
      }
    }
    System.out.println(dbQueryStatus.getMessage());
    return dbQueryStatus;
	}

}
