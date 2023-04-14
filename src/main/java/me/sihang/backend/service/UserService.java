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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

@Service
public class UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    private Bucket bucket;
    //private Bucket bucket_app;

    @Autowired
    public UserService(Bucket bucket) {
        this.bucket = bucket;
        //this.bucket_app = bucket_app;
    }

    public JsonObject isValidUser(String username, String password) {
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select `username`, `realname`, `privilege` from `bm_sys` where `type` = 'user' and `username` = $1 and `password` = $2;", JsonArray.from(username, password))
        );
        if (result.allRows().size() == 0) {
            return null;
        }
        JsonObject resultObj = (JsonObject) result.allRows().get(0).value();
        return resultObj;
    }

    public JsonObject isValidUser_app(String username, String password) {
        String encrytedPassword = MD5.parse(password);
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select `userId`,`phonenumber`,`type` from `bm_sys_app` where  `username` = $1 and `password` = $2;", JsonArray.from(username, encrytedPassword))
        );
        if (result.allRows().size() == 0) {
            return null;
        }
        JsonObject resultObj = (JsonObject) result.allRows().get(0).value();
        return resultObj;
    }

    public ArrayList<HashMap<String, String>> getMyDrafts(String username) {
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select `caseId`, `status`, `lastActive` from `bm_sys` where type = 'draft' and username = $1;", JsonArray.from(username))
        );
        if (result.allRows().size() == 0)
            return null;
        ArrayList<HashMap<String, String>> arr = new ArrayList<>();
        for (N1qlQueryRow row : result) {
            JsonObject jo = row.value();
            HashMap<String, String> rowMap = new HashMap<>();
            rowMap.put("caseId", (String) jo.get("caseId"));
            rowMap.put("status", (String) jo.get("status"));
            rowMap.put("lastActive", String.valueOf(jo.get("lastActive")));
            rowMap.put("link", "/case/" + (String) jo.get("caseId") + "/" + username);
            arr.add(rowMap);
        }
        return arr;
    }

    public Map<String,String> insertUserInfo(String username){
        Map<String,String> res = new HashMap<>();
        JsonObject jsonObject = JsonObject.create();
        jsonObject.put("username", username);
        jsonObject.put("realname", username);
        jsonObject.put("password", "e10adc3949ba59abbe56e057f20f883e");
        jsonObject.put("privilege", "1");
        //用户创建时间 20210715 liwei
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String createTime = dateFormat.format(System.currentTimeMillis());
        jsonObject.put("createTime", createTime);
        JsonArray role = JsonArray.create();
        role.add("general");
        jsonObject.put("roles", role);
        jsonObject.put("type", "user");

        try{
            N1qlQueryResult result = bucket.query(
                    N1qlQuery.parameterized("select username  from `bm_sys` where type = 'user' and  `username` = $1;", JsonArray.from(username))
            );
            if (result.allRows().size() != 0) {
                res.put("status","existed");
            }
            else{
                System.out.println(jsonObject);
                JsonDocument doc = JsonDocument.create(username + "@user", jsonObject);
                JsonDocument upsert = bucket.upsert(doc);
                res.put("status","ok");
            }
        }catch (Exception e){
            res.put("status","failed");
        }
        return res;
    }

    public Map<String,String> insertUserInfo(String username,String password, String[] roles){
        Map<String,String> res = new HashMap<>();
        if(this.isUserExisted(username)){
            res.put("status","existed");
            return res;
        }
        JsonObject jsonObject = JsonObject.create();
        jsonObject.put("username", username);
        jsonObject.put("realname", username);
        jsonObject.put("password", password);
        jsonObject.put("privilege", "1");
        //用户创建时间 20210715 liwei
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String createTime = dateFormat.format(System.currentTimeMillis());
        jsonObject.put("createTime", createTime);
        JsonArray role = JsonArray.from(roles);
        jsonObject.put("roles", role);
        jsonObject.put("type", "user");

        try{
            N1qlQueryResult countResult = bucket.query(
                    N1qlQuery.simple("select count(*) as cnt from `bm_sys` where `type` = 'user'")
            );
            int no = countResult.allRows().get(0).value().getInt("cnt") + 1;

            JsonDocument doc = JsonDocument.create(username + "@user", jsonObject);
            bucket.upsert(doc);
            res.put("status","ok");
            Integer totalUserPages = no % 10 == 0 ? (int) no / 10 : (int) no / 10 + 1;
            res.put("newTotalPage",totalUserPages.toString());
        }catch (Exception e){
            res.clear();
            res.put("status","failed");
        }
        return res;
    }

    public boolean isUserExisted(String username){
        N1qlQueryResult userResult = bucket.query(
                N1qlQuery.parameterized("select `username` from `bm_sys` where `type` = 'user' and `username` = $1;", JsonArray.from(username))
        );
        if (userResult.allRows().size() != 0) {
            return true;
        }else {
            return false;
        }
    }


    public Map<String,String> updateUserInfo(String username,String password, String[] roles){
        HashMap<String,String> res = new HashMap<>();
        JsonObject user = this.bucket.get(username + "@user").content();
        JsonArray roleLst = JsonArray.create();
        for(String role : roles){
            roleLst.add(role);
        }
        user.put("roles",roleLst);
        user.put("password",password);
        try{
            JsonDocument doc = JsonDocument.create(username + "@user", user);
            bucket.upsert(doc);
            res.put("status","ok");
        }catch (Exception e){
            res.put("status","failed");
        }
        return res;
    }

    public boolean isAdmin(String username,String password){
        String encrytedPassword = MD5.parse(password);
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select roles  from `bm_sys` where type = 'user' and  `username` = $1 and `password` = $2;", JsonArray.from(username, encrytedPassword))
        );
        if (result.allRows().size() == 0) {
            return false;
        }

        String roles = (String) result.allRows().get(0).value().get("roles");
        return roles.contains("admin");
    }

    public ArrayList<String> getRolesForUser(String username){
        ArrayList<String> res = new ArrayList<>();
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select roles from `bm_sys` where type = 'user' and  `username` = $1;", JsonArray.from(username))
        );
        if (result.allRows().size() == 0) {
            return res;
        }

        JsonObject obj = result.allRows().get(0).value();

        if(!obj.containsKey("roles")){
            return res;
        }

        JsonArray roles = obj.getArray("roles");

        for(int i = 0; i< roles.size(); i++){
            if(!res.contains(roles.getString(i))){
                res.add(roles.getString(i));
            }
        }

        return res;


    }

    public HashMap<String,String> updateRolesForUser(String username, String[] roles){
        HashMap<String,String> res = new HashMap<>();
        JsonObject user = this.bucket.get(username + "@user").content();
        JsonArray roleLst = JsonArray.create();
        for(String role : roles){
            roleLst.add(role);
        }
        user.put("roles",roleLst);
        try{
            JsonDocument doc = JsonDocument.create(username + "@user", user);
            bucket.upsert(doc);
            res.put("status","ok");
        }catch (Exception e){
            res.put("status","failed");
        }

        return res;
    }

    public Integer getTotalUserPages(){
        N1qlQueryResult result = bucket.query(
                N1qlQuery.simple("select count(*) as cnt from `bm_sys` where `type` = 'user'")
        );
        int no = result.allRows().get(0).value().getInt("cnt");
        return no % 10 == 0 ? (int) no / 10 : (int) no / 10 + 1;
    }

    public ArrayList getUserAtpage(String page,String orderBy){
        if(!orderBy.isEmpty()){
            orderBy = " order by "+orderBy;
        }

        ArrayList<Map<String,Object>> res = new ArrayList<>();
        int offset = (Integer.parseInt(page) - 1) * 10;
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select username, roles,createTime from `bm_sys` where `type` = 'user' "+orderBy+" limit 10 offset $1", JsonArray.from(offset))
        );

        for(N1qlQueryRow row : result){
            JsonObject obj = row.value();
            Map<String,Object> user = new HashMap<>();
            user.put("username",obj.getString("username"));
            if(obj.containsKey("roles")){
                user.put("roles", obj.getArray("roles"));
            }
            if(obj.containsKey("createTime")){
                user.put("createTime", obj.getString("createTime"));
            }
            else {
                user.put("createTime", "2021-01-01");
            }

            res.add(user);
        }

        return res;
    }

    public Map<String,String> delUser(String userName){
        Map<String,String> res = new HashMap<>();
        try{
            N1qlQueryResult result = bucket.query(
                    N1qlQuery.simple("select count(*) as cnt from `bm_sys` where `type` = 'user'")
            );
            int no = result.allRows().get(0).value().getInt("cnt") - 1;

            bucket.remove(userName + "@user");
            res.put("status","ok");
            Integer totalUserPages = no % 10 == 0 ? (int) no / 10 : (int) no / 10 + 1;
            res.put("newTotalPage",totalUserPages.toString());
            return res;
        }catch (Exception e){
            res.put("status","failed");
            return res;
        }
    }

    public HashMap<String,String> getRemoteAddr(){
        HashMap<String,String> res = new HashMap<>();
        try{
            String remoteAddr = MDC.get("REMOTE_ADDR");
            if (!StringUtils.isEmpty(remoteAddr)) {
                res.put("remoteAddr",remoteAddr);
            }else {
                res.put("remoteAddr","unknown");
            }

        }catch (Exception e){
            res.put("remoteAddr","unknown");
        }
        return res;
    }

    public Map<String,String> addUserUsageDuration(String username,String studyId,String caseId,String duration){
        Map<String,String> res = new HashMap<>();
        int durationInt = 0;
        try {
            durationInt = Integer.parseInt(duration);
        }catch (Exception e){
            res.put("status","failed");
            res.put("errorMessage","["+duration+"] parseInt failed.");
            return res;
        }

        JsonDocument durationRecordDoc = bucket.get(username+"@duration_record");
        if(durationRecordDoc == null){
            durationRecordDoc = createDurationRecordDoc(username, studyId, caseId, durationInt);
        }else {
            JsonObject durationRecord = durationRecordDoc.content();
            Integer count = durationRecord.getInt("count");
            int totalDuration = durationRecord.getInt("totalDuration");
            JsonArray durationList = durationRecord.getArray("durationList");
            durationRecord.put("count",count+1);
            durationRecord.put("totalDuration",totalDuration + durationInt);
            durationRecord.put("meanDuration",(totalDuration+durationInt)/(count+1.0));
            JsonObject durationElement = JsonObject.create();
            durationElement.put("studyId",studyId);
            durationElement.put("caseId",caseId);
            durationElement.put("duration",durationInt);
            SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            long currentTimeMillis = System.currentTimeMillis();
            String eventTime = timeFormat.format(currentTimeMillis);
            durationElement.put("completeTime",eventTime);
            durationList.add(durationElement);
        }
        try {
            bucket.upsert(durationRecordDoc);
            res.put("status","success");
            return res;
        } catch (Exception e) {
            res.put("status","failed");
            res.put("errorMessage","upsert failed.");
            return res;
        }
    }

    private JsonDocument createDurationRecordDoc(String username,String studyId,String caseId,int durationInt){
        JsonObject record = JsonObject.create();
        JsonArray durationList = JsonArray.create();
        JsonObject durationElement = JsonObject.create();

        record.put("type", "duration_record");
        record.put("operatorName", username);
        record.put("count", 1);
        record.put("totalDuration", durationInt);
        record.put("meanDuration", durationInt);

        SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long currentTimeMillis = System.currentTimeMillis();
        String eventTime = timeFormat.format(currentTimeMillis);
        durationElement.put("studyId",studyId);
        durationElement.put("caseId",caseId);
        durationElement.put("duration",durationInt);
        durationElement.put("completeTime",eventTime);
        durationList.add(durationElement);
        record.put("durationList", durationList);

        JsonDocument doc = JsonDocument.create(username+"@duration_record", record);
        return doc;
    }

    public Map<String,String> saveCustomConfig(String username,String filterAndSorter){
        Map<String,String> res = new HashMap<>();
        int durationInt = 0;

        JsonDocument customConfigDoc = bucket.get(username+"@custom_config");
        if(customConfigDoc == null){
            customConfigDoc = createCustomConfigDoc(username);
        }
        JsonObject customConfig = customConfigDoc.content();
        if(filterAndSorter!=null){
            customConfig.put("filterAndSorter",filterAndSorter);
        }

        try {
            bucket.upsert(customConfigDoc);
            res.put("status","success");
            return res;
        } catch (Exception e) {
            res.put("status","failed");
            res.put("errorMessage","upsert failed.");
            return res;
        }
    }

    private JsonDocument createCustomConfigDoc(String username){
        JsonObject record = JsonObject.create();

        record.put("type", "custom_config");
        record.put("username", username);
        record.put("filterAndSorter", "");

        JsonDocument doc = JsonDocument.create(username+"@custom_config", record);
        return doc;
    }

    public JsonObject getCustomConfig(String username){
        JsonDocument customConfigDoc = bucket.get(username+"@custom_config");
        if(customConfigDoc == null){
            return null;
        }
        return customConfigDoc.content();
    }

}
