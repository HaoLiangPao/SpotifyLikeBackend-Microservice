package com.csc301.songmicroservice;
import com.mongodb.BasicDBObjectBuilder;

import com.mongodb.DBObject;
import com.sun.xml.internal.fastinfoset.util.StringArray;

import java.util.ArrayList;
import org.bson.Document;
import org.bson.types.ObjectId;

public class SongConverter {

    public Document toDocument(Song song){
        BasicDBObjectBuilder builder = BasicDBObjectBuilder.start()
                .append("songName", song.getSongName()).append("songArtistFullName", song.getSongArtistFullName())
                .append("songAlbum", song.getSongAlbum()).append("songAmountFavourites", song.getSongAmountFavourites());
        Document document = new Document(builder.get().toMap());

        return document;
    }

    // convert DBObject Object to Person
    // take special note of converting ObjectId to String
    public Song toSong(Document doc) {
        Song s = new Song((String) doc.get(Song.KEY_SONG_NAME),
            (String) doc.get(Song.KEY_SONG_ARTIST_FULL_NAME),
            (String) doc.get(Song.KEY_SONG_ALBUM));
        s.setId(doc.getObjectId("_id"));
        s.setSongAmountFavourites((Long) doc.get("songAmountFavourites"));
        return s;
    }

//    public postConvertor getPostConvertor(){
//        return this;
//    }
}
