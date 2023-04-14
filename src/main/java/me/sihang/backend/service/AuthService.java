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

import javax.validation.constraints.Null;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {
    private Bucket bucket;

    @Autowired
    public AuthService(Bucket bucket) {
        this.bucket = bucket;
    }

    public int insertAuth(String authName){

        JsonObject jsonObject = JsonObject.create();
        jsonObject.put("authName", authName);
        jsonObject.put("type", "auth");

        try{
            JsonDocument doc = JsonDocument.create(authName + "@auth", jsonObject);
            bucket.upsert(doc);
            return 1;
        }catch (Exception e){
            return 0;
        }
    }

    public int delAuth(String authName){
        try{
            bucket.remove(authName + "@auth");
            return 1;
        }catch (Exception e){
            return 0;
        }
    }

    public ArrayList<String> getAllAuth(){
        ArrayList<String> res = new ArrayList<>();
        N1qlQueryResult result = bucket.query(
                N1qlQuery.simple("select authName from `bm_sys` where type = 'auth' ;")
        );

        if (result.allRows().size() == 0){
            return res;
        }
        for(N1qlQueryRow row: result){
            res.add((String)row.value().get("authName"));
        }

        return res;
    }

    public ArrayList<String> getAuthsForRole(String[] roles){
        ArrayList<String> res = new ArrayList<>();

        ArrayList<String> allRolesList = new ArrayList<>();
        for(Object role: roles){
            allRolesList.add("'" + role.toString() + "'");
        }
        String roleStrs = "[" + String.join(",", allRolesList) + "]";

        N1qlQueryResult result = bucket.query(
                N1qlQuery.simple("select auths from `bm_sys` where type = 'role' and roleName in " + roleStrs + " ;")
        );

        if (result.allRows().size() == 0){
            return res;
        }

        JsonObject obj = result.allRows().get(0).value();
        JsonArray authLst = obj.getArray("auths");

        if(authLst != null){
            for(int i = 0; i < authLst.size(); i++){
                if(!res.contains(authLst.getString(i))){
                    res.add(authLst.getString(i));
                }
            }

        }


        return res;
    }

}
