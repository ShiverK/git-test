package me.sihang.backend.service;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;
import me.sihang.backend.util.MD5;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public class RoleService {

    private Bucket bucket;

    @Autowired
    public RoleService(Bucket bucket) {
        this.bucket = bucket;
    }

    public int upsertRole(String roleName, String[] authLst){

        JsonObject jsonObject = JsonObject.create();
        jsonObject.put("roleName", roleName);
        jsonObject.put("type", "role");

        JsonArray auths = JsonArray.create();
        for(String authName : authLst){
            auths.add(authName);
        }

        jsonObject.put("auths", auths);

        try{
            JsonDocument doc = JsonDocument.create(roleName + "@role", jsonObject);
            bucket.upsert(doc);
            return 1;
        }catch (Exception e){
            return 0;
        }
    }

    public int delRole(String roleName){
        try{
            bucket.remove(roleName + "@role");
            return 1;
        }catch (Exception e){
            return 0;
        }
    }

    public ArrayList<String> getAllRole(){
        ArrayList<String> res = new ArrayList<>();
        N1qlQueryResult result = bucket.query(
                N1qlQuery.simple("select roleName from `bm_sys` where type = 'role' ;")
        );

        if (result.allRows().size() == 0){
            return res;
        }
        for(N1qlQueryRow row: result){
            res.add((String)row.value().get("roleName"));
        }

        return res;
    }

    public ArrayList<String> getAuthsByRole(String roleName){
        ArrayList<String> res = new ArrayList<>();
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select auths from `bm_sys` where type = 'role' and roleName = $1 ;", JsonArray.from(roleName))
        );

        if (result.allRows().size() == 0){
            return res;
        }

        JsonObject obj = result.allRows().get(0).value();
        JsonArray auths = obj.getArray("auths");
        //System.out.println(auths.toString());

        for(int i = 0; i < auths.size(); i++){
            res.add(auths.get(i).toString());
        }
        return res;
    }
}
