package me.sihang.backend.service;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import me.sihang.backend.util.JsonReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Map;

@Service
public class DataService {
    private Bucket bucket;

    @Autowired
    public DataService(Bucket bucket) {
        this.bucket = bucket;
    }

    public boolean addClickCount(){

        JsonDocument clickCountInfoDoc = bucket.get("clickCount@monitor");
        if(clickCountInfoDoc == null){
            clickCountInfoDoc = createClickCountDocument();
        }
        JsonObject clickCountInfo = clickCountInfoDoc.content();
        int totalClickCount = clickCountInfo.getInt("totalClickCount");
        int dailyTotalClickCount = 0;

        SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd");
        long currentTimeMillis = System.currentTimeMillis();
        String currentDate = timeFormat.format(currentTimeMillis);
        JsonObject dailyClickCountObj = clickCountInfo.getObject("dailyClickCount");
        if(dailyClickCountObj.containsKey(currentDate)){
            dailyTotalClickCount = dailyClickCountObj.getInt(currentDate);
        }


        clickCountInfo.put("totalClickCount", totalClickCount + 1);
        dailyClickCountObj.put(currentDate, dailyTotalClickCount + 1);

        try{
            JsonDocument doc = JsonDocument.create("clickCount@monitor", clickCountInfo);
            bucket.upsert(doc);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    private JsonDocument createClickCountDocument(){
        JsonObject obj = JsonObject.create();
        obj.put("type", "monitor");
        obj.put("totalClickCount", 0);
        JsonObject dailyClickObj = JsonObject.create();
        obj.put("dailyClickCount", dailyClickObj);

        JsonDocument doc = JsonDocument.create("clickCount@monitor", obj);
        try {
            bucket.upsert(doc);
            return doc;
        } catch (Exception e) {
            return null;
        }
    }

    public JsonObject getClickCount(){
        JsonDocument clickCountInfoDoc = bucket.get("clickCount@monitor");
        return clickCountInfoDoc.content().removeKey("type");
    }

    public JsonObject getPluginConfig(){
        JsonReader jsonReader = new JsonReader();
        Map config = jsonReader.readJsonFile();
        JsonObject configObject = JsonObject.from(config);
        JsonObject returnObject = JsonObject.create();
        try{
            String hostname = configObject.get("hostname").toString();
            int backendPort = configObject.getObject("backend").getDouble("port").intValue();
            int frontPort = configObject.getObject("front").getDouble("port").intValue();
            returnObject = configObject.getObject("plugin");


            returnObject.put("hostname",hostname);
            returnObject.put("backendPort",backendPort);
            returnObject.put("frontPort",frontPort);
//            returnObject.put("regex",regex);
        }catch (Exception e){
            returnObject.put("status","failed");
            returnObject.put("errorMessage","please check the format of config.json");
        }

        return returnObject;
    }
}
