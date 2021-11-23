package model;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.jetty.websocket.api.Session;
import redis.clients.jedis.Jedis;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Data {
    private String name;
    private int data_size;
    public ArrayList<Document> data;
    private List<String> fields;
    private Boolean file_end;

    public Data(String name) {
        this.name = name;
        this.data_size = 0;
        this.data = new ArrayList<Document>();
        this.fields = new ArrayList<String>();
        this.file_end = false;
    }

    public List<String> getFields()  {
        return this.fields;
    }

    public Boolean getFile_end() {
        return this.file_end;
    }

    private void setfields(List<String> fields) {
        this.fields = fields;
    }

    private void setDataSize() {
        this.data_size = this.data.size();
    }

    private void setFile_end() {
        this.file_end = true;
    }

    public void add_data(String data, MongoCollection collection) {
        List<String> dataarr = Arrays.asList(data.split(",", -1));
        if (this.fields.size() == 0) {
            this.setfields(dataarr);
        }
        else {
            LocalDateTime datetime = LocalDateTime.now();
            Document doc = new Document("_id",datetime);
            doc.append("filename",this.name);
            for (int i = 0; i < this.fields.size(); i++) {
                doc.append(this.fields.get(i), dataarr.get(i));
            }
//            this.data.add(doc);
            collection.insertOne(doc);
            this.data_size += 1;
        }
    }

    public int get_data_size() {
//        this.setDataSize();
        this.setFile_end();
        return this.data_size;
    }

    public List<Document> get_data(int page) {
        return this.data.subList(0,51);
    }

    public void search(Session session, MongoCollection collection, Map<String, String> keys, Integer page) {
        ArrayList<JsonObject> res = new ArrayList<JsonObject>();
        int idx = 0;
        BasicDBObject whereQuery = new BasicDBObject();
        MongoCursor<Document> cursor;
        whereQuery.put("filename", this.name);
        if (keys.size() == 0) {
            cursor = collection.find(whereQuery).skip(20 * page).limit(20).iterator();
        }
        else {
            for (Map.Entry<String, String> entry: keys.entrySet()) {
                String pattern = ".*" + entry.getValue() + ".*";
                whereQuery.put(entry.getKey(), new BasicDBObject("$regex", pattern));
            }
            cursor = collection.find(whereQuery).skip(20 * page).limit(20).iterator();
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