package com.csc301.songmicroservice;
import com.mongodb.BasicDBObjectBuilder;

import com.mongodb.DBObject;
import com.sun.xml.internal.fastinfoset.util.StringArray;

import java.util.ArrayList;
import org.bson.Document;
import org.bson.types.ObjectId;

public class SongConverter {
    public SongConverter(){

    }
    public Document toDocument(Song song){
        BasicDBObjectBuilder builder = BasicDBObjectBuilder.start()
                .append("songName", song.getSongName()).append("songArtistFullName", song.getSongArtistFullName())
                .append("songAlbum", song.getSongAlbum()).append("songAmountFavourites", song.getSongAmountFavourites());
        Document document = new Document(builder.get().toMap());

        return document;
    }
/*
    // convert DBObject Object to Person
    // take special note of converting ObjectId to String
    public Post toPost(Document doc) {
        Post p = new Post();
        p.setTitle((String) doc.get("title"));
        p.setAuthor((String) doc.get("author"));
        p.setContent((String) doc.get("content"));
        p.setTags((ArrayList<String>) doc.get("tags"));
        ObjectId id = (ObjectId) doc.get("_id");
        p.setId(id.toString());
        return p;
    }

    public postConvertor getPostConvertor(){
        return this;
    }
    */
}
