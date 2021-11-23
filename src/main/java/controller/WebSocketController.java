package controller;

import com.google.gson.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import model.DispatchAdapter;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebSocket(maxIdleTime = 3600 * 1000)
public class WebSocketController {

    private static MongoClient client = null;
    public MongoCollection collection = null;
    /**
     * Open user's session.
     * @param user The user whose session is opened.
     */
    @OnWebSocketConnect
    public void onConnect(Session session) {
        System.out.println(session);
        System.out.println("connect");
        try {
            this.client = MongoClients.create("mongodb+srv://zzzpdm:1317@cluster0.1ypxs.mongodb.net/myFirstDatabase?retryWrites=true&w=majority");
            MongoDatabase db = this.client.getDatabase("testdb");
            this.collection = db.getCollection("test");
            System.out.println("Connect to database successfully");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Close the user's session.
     * @param user The use whose session is closed.
     */
    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        System.out.println("close");
        System.out.println(statusCode);
        System.out.print(reason);
    }

    /**
     * Send request from view.
     * @param user The session user sending the message.
     * @param message The request to be executed.
     */
    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        JsonParser parser = new JsonParser();
        JsonObject obj = (JsonObject) parser.parse(message);
        String type = obj.get("type").getAsString();
        String filename = obj.get("name").getAsString();
        switch(type) {
            case "availableFile":
                DispatchAdapter.getInstance().getFiles(session);
                break;
            case "file":
                JsonElement filedata = obj.get("data");
                DispatchAdapter.getInstance().CreateData(session, this.collection, filename, filedata);
                break;
            case "field_size":
                DispatchAdapter.getInstance().send_data_size_fields(session, filename);
                break;
            case "searchkeys":
                String keys = obj.get("keys").getAsString();
                int page = obj.get("page").getAsInt();
                DispatchAdapter.getInstance().SearchData(session, this.collection, filename, keys, page);
                break;
            default:
                break;
        }
    }
}
