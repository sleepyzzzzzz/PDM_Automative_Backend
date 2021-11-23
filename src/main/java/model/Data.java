package model;

import com.google.gson.JsonObject;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.jetty.websocket.api.Session;
import redis.clients.jedis.Jedis;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Data {
    private String name;
    private int data_size;
    private List<String> fields;
    private Boolean file_end;

    public Data(String name, int data_size) {
        this.name = name;
        this.data_size = data_size;
        this.fields = new ArrayList<String>();
        this.file_end = false;
    }

    public List<String> getFields()  {
        return this.fields;
    }

    private int getData_size() {return 10000000;}

    public Boolean getFile_end() {
        return this.file_end;
    }

    private void setfields(List<String> fields) {
        this.fields = fields;
    }

    public void setFile_end(Boolean end) {
        this.file_end = end;
    }

    public void add_data(String data, MongoCollection collection) {
        List<String> dataarr = Arrays.asList(data.split(",", -1));
        if (this.getFields().size() == 0) {
            this.setfields(dataarr);
        }
        else {
            LocalDateTime datetime = LocalDateTime.now();
            Document doc = new Document("_id",datetime);
            doc.append("filename",this.name);
            for (int i = 0; i < this.fields.size(); i++) {
                doc.append(this.fields.get(i), dataarr.get(i));
            }
            System.out.println("here");
            collection.insertOne(doc);
        }
    }

    public int get_data_size() {
        this.setFile_end(true);
        return this.getData_size();
    }

    public void search(Session session, MongoCollection collection, Map<String, String> keys, int page) {
        ArrayList<JsonObject> res = new ArrayList<JsonObject>();
        int idx = page * 20;
        BasicDBObject whereQuery = new BasicDBObject();
        MongoCursor<Document> cursor;
        whereQuery.put("filename", this.name);
        if (keys.size() == 0) {
            cursor = collection.find(whereQuery).sort(new BasicDBObject("_id",1)).skip(20* page).limit(20).iterator();
        }
        else {
            for (Map.Entry<String, String> entry: keys.entrySet()) {
                String pattern = ".*" + entry.getValue() + ".*";
                whereQuery.put(entry.getKey(), new BasicDBObject("$regex", pattern));
            }
            cursor = collection.find(whereQuery).sort(new BasicDBObject("_id",1)).skip(20 * page).limit(20).iterator();
        }
        try {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                String json_doc = doc.toJson();
                JsonObject result = new JsonObject();
                result.addProperty(String.valueOf(idx), json_doc);
                res.add(result);
                idx += 1;
            }
        }
        finally {
            cursor.close();
        }
        try {
            session.getRemote().sendString(String.valueOf(res));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}