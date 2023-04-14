package me.sihang.backend.util;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import me.sihang.backend.domain.ConfigPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

@Component
public class DiskMonitor {

    @Autowired
    private Bucket bucket;

    @Autowired
    private ConfigPath configPath;

    public void updateDiskInfo(){
        JsonDocument diskInfoDoc = bucket.get("disk@monitor");
        if(diskInfoDoc == null){
            diskInfoDoc = createDiskDocument();
        }
        JsonObject diskInfo = diskInfoDoc.content();

        if(!diskInfo.getString("enableAutoUpdate").equals("true")){
            return;
        }

        File[] roots = File.listRoots();
        JsonReader jsonReader = new JsonReader();
//        Map config = jsonReader.readJsonFile();
        String dataPath  = configPath.getDatapath();
//        String dataPath = config.get("datapath").toString();
        for (File file : roots) {
            if (dataPath.startsWith(file.getPath())){
                long free = file.getFreeSpace();
                long total = file.getTotalSpace();
                long use = total - free;
                SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                long currentTimeMillis = System.currentTimeMillis();
                String currentTime = timeFormat.format(currentTimeMillis);

                diskInfo.put("avail",change(free) + "G");
                diskInfo.put("used",change(use) + "G");
                diskInfo.put("totalSize",change(total) + "G");
                diskInfo.put("usedPercents",bfb(use, total));
                diskInfo.put("lastUpdateTime",currentTime);
                JsonDocument doc = JsonDocument.create("disk@monitor", diskInfo);
                try {
                    bucket.upsert(doc);
                    return;
                } catch (Exception e) {
                    return;
                }
            }
        }
    }



    private JsonDocument createDiskDocument(){
        JsonObject obj = JsonObject.create();
        obj.put("type", "monitor");
        obj.put("totalSize", "");
        obj.put("avail", "");
        obj.put("used", "");
        obj.put("usedPercents", "");
        obj.put("object", "disk");
        obj.put("lastUpdateTime", "");
        obj.put("enableAutoUpdate", "true");

        JsonDocument doc = JsonDocument.create("disk@monitor", obj);
        try {
            bucket.upsert(doc);
            return doc;
        } catch (Exception e) {
            return null;
        }
    }

    private long change(long num) {
        // return num;
        return num / 1024 / 1024 / 1024;
    }

    private String bfb(Object num1, Object num2) {
        double val1 = Double.valueOf(num1.toString());
        double val2 = Double.valueOf(num2.toString());
        if (val2 == 0) {
            return "0.0%";
        } else {
            DecimalFormat df = new DecimalFormat("#0.00");
            return df.format(val1 / val2 * 100) + "%";
        }
    }

}
