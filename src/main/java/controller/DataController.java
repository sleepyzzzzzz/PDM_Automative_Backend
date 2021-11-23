package controller;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.eclipse.jetty.websocket.api.Session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static spark.Spark.*;

@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping(value="/sample", method = RequestMethod.GET, produces = "application/json")
public class DataController {
    static Map<Session, String> sessionuser = new ConcurrentHashMap<>();
    static int nextUserId = 1;
    private static DataController datacontroller;

    /**
     * Get singleton instance of DataController.
     * @return DataController singleton
     */
    public static DataController getInstance() {
        if (datacontroller == null) {
            datacontroller = new DataController();
        }
        return datacontroller;
    }

    /**
     * Frontend data entry point.
     * @param args arguments
     */
    public static void main(String[] args) {
        port(getHerokuAssignedPort());
        staticFiles.location("/public");

        webSocket("/platform", WebSocketController.class);
        init();

        Gson gson = new Gson();
    }

    /**
     * Get the heroku assigned port number.
     * @return The heroku assigned port number
     */
    private static int getHerokuAssignedPort() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (processBuilder.environment().get("PORT") != null) {
            return Integer.parseInt(processBuilder.environment().get("PORT"));
        }
        return 4567;
    }
}
