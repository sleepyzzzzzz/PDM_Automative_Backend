package model;

import com.google.gson.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.eclipse.jetty.websocket.api.Session;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DispatchAdapter {
    private static DispatchAdapter dispatchAdapter;
    private static Map<String, Data> file_data = new HashMap<String, Data>();

    public static DispatchAdapter getInstance() {
        if (dispatchAdapter == null) {
            dispatchAdapter = new DispatchAdapter();
        }
        return dispatchAdapter;
    }

    public void getFiles(Session session) {
        int idx = 0;
        ArrayList<JsonObject> res = new ArrayList<JsonObject>();
        for (Map.Entry<String, Data> entry: this.file_data.entrySet()) {
            JsonObject result = new JsonObject();
            result.addProperty(String.valueOf(idx), String.valueOf(entry.getKey()));
            res.add(result);
        }
        try {
            session.getRemote().sendString(String.valueOf(res));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void CreateData(Session session, MongoCollection collection, String name, JsonElement data, int dataSize) {
        JsonParser parser = new JsonParser();
        JsonObject msg = new JsonObject();
        if (file_data == null || file_data.get(name) == null) {
            Data file = new Data(name, dataSize);
            file_data.put(name, file);
        }
        if (file_data.get(name).getFile_end()) {
            msg.addProperty("msg", name + " file already loaded");
            String json_msg = new Gson().toJson(msg);
            try {
                session.getRemote().sendString(json_msg);
                file_data.get(name).setFile_end(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            for (int i = 0; i < data.getAsJsonArray().size(); i++) {
                String newdata = parser.parse(String.valueOf(data.getAsJsonArray().get(i))).getAsString().replace("\r", "");
                if (newdata.length() != 0) {
                    file_data.get(name).add_data(newdata, collection);
                }
            }
            msg.addProperty("msg", name + " file loaded");
            String json_msg = new Gson().toJson(msg);
            try {
                session.getRemote().sendString(json_msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void send_data_size_fields(Session session, String name) {
        JsonObject fields = new JsonObject();
        fields.addProperty("fields", new Gson().toJson(file_data.get(name).getFields()));
        String json_fields = new Gson().toJson(fields);
        JsonObject dataSize = new JsonObject();
        dataSize.addProperty("dataSize", new Gson().toJson(file_data.get(name).get_data_size()));
        String json_size = new Gson().toJson(fields);
        System.out.println("send fields");
        try {
            session.getRemote().sendString(json_fields);
            session.getRemote().sendString(json_size);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void SearchData(Session session, MongoCollection collection, String name, String keys, int page) {
        Gson gson = new Gson();
        String[] search_keys = keys.substring(1, keys.length() - 1).split(",");
        Map<String, String> Filteredkeys = new HashMap<String, String>();
        if (search_keys.length != 1) {
            for (int i = 0; i < search_keys.length; i += 2) {
                String newsearch_key = search_keys[i].replace("[", "").replace("]", "");
                String newsearch_value = search_keys[i + 1].replace("[", "").replace("]", "");
                String searchkey = newsearch_key.substring(1, newsearch_key.length() - 1);
                String searchvalue = newsearch_value.substring(1, newsearch_value.length() - 1);
                Filteredkeys.put(searchkey, searchvalue);
            }
        }
        file_data.get(name).search(session, collection, Filteredkeys, page);
    }
}
