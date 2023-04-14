package me.sihang.backend.util;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConstantMap {
    private String MD5_validation_flag = "true";

    @Autowired
    private Bucket bucket;

    public void setMD5_validation_flag(String MD5_validation_flag) {
        this.MD5_validation_flag = MD5_validation_flag;
    }

    public String getMD5_validation_flag() {
        return MD5_validation_flag;
    }

    public JsonDocument createMD5ValidationFlagDocument(){
        JsonObject obj = JsonObject.create();
        obj.put("type", "constant");
        obj.put("MD5ValidationFlag", "true");

        JsonDocument doc = JsonDocument.create("MD5ValidationFlag@constant", obj);
        try {
            bucket.upsert(doc);
            return doc;
        } catch (Exception e) {
            return null;
        }
    }
}
