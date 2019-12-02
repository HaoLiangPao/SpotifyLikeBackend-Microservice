package com.csc301.profilemicroservice;

import static org.neo4j.driver.v1.Values.parameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import org.springframework.stereotype.Repository;
import org.neo4j.driver.v1.Transaction;

@Repository
public class ProfileDriverImpl implements ProfileDriver {

	Driver driver = ProfileMicroserviceApplication.driver;
	DbQueryStatus dbQueryStatus = new DbQueryStatus("defaul message", DbQueryExecResult.QUERY_OK);
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
        queryStr = "MERGE (p:Profile{userName:$userName}) SET p.fullName = $fullName, p.password = "
            + "$password RETURN p.userName, p.fullName";
        StatementResult result = trans.run(queryStr, parameters("userName",
            userName, "fullName", fullName, "password", password));
        //Get values from neo4j StatementResult object
        List<Record> records = result.list();
        Record record = records.get(0);
        Map recordMap = record.asMap();

        // create relationship between the profile and a playlist

        dbQueryStatus.setMessage("Profile is created and added to the database");
        dbQueryStatus.setdbQueryExecResult(DbQueryExecResult.QUERY_OK);
      }
    }
    System.out.println(dbQueryStatus.getMessage());
    return dbQueryStatus;
  }

	@Override
	public DbQueryStatus followFriend(String userName, String frndUserName) {
		
		return null;
	}

	@Override
	public DbQueryStatus unfollowFriend(String userName, String frndUserName) {
		
		return null;
	}

	@Override
	public DbQueryStatus getAllSongFriendsLike(String userName) {
			
		return null;
	}
}
