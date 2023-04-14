package me.sihang.backend.service;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import io.netty.util.internal.StringUtil;
import me.sihang.backend.util.MD5;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.*;

@Service
public class DraftService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    private Bucket bucket;

    @Autowired
    public DraftService(Bucket bucket) {
        this.bucket = bucket;
    }

    public boolean isDraftExisted(String username, String caseId) {
        N1qlQueryResult result = bucket.query(
          N1qlQuery.parameterized("select username from `bm_sys` where type = 'draft' and caseId = $1 and username = $2", JsonArray.from(caseId, username))
        );
        if (result.allRows().size() > 0)
            return true;
        else
            return false;
    }

    public String getDraftStatus(String username, String caseId) {
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select status from `bm_sys` where type = 'draft' and caseId = $1 and username = $2", JsonArray.from(caseId, username))
        );
        try {
            return (String) result.allRows().get(0).value().get("status");
        } catch (Exception e) {
            return null;
        }

    }

    public ArrayList<String> getModelResults(String caseId) {
        ArrayList<String> ret = new ArrayList<>();
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select username from `bm_sys` where type = 'draft' and caseId = $1 and status = '1' ", JsonArray.from(caseId))
        );
//        如果为空，立即返回，这时ret就是一个空的List
        if (result.allRows().size() == 0)
            return ret;
        for (N1qlQueryRow row: result) {
            String username = row.value().getString("username");
            if (username.equals("bm_sys3.0")||username.equals("bm_sys")||username.equals("bm_sys2.0"))
            {
                ret.add(username);
            }
        }
        return ret;
    }

    public ArrayList<String> getAnnoResults(String caseId) {
        ArrayList<String> ret = new ArrayList<>();
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select username from `bm_sys` where type = 'draft' and caseId = $1 and status = '1' ", JsonArray.from(caseId))
        );
        for (N1qlQueryRow row: result) {
            String username = row.value().getString("username");
            if (!username.equals("error")&&!username.equals("bm_sys")&&!username.equals("bm_sys2.0")&&!username.equals("bm_sys3.0"))
            {
                ret.add(username);
            }

        }
        return ret;
    }


    public JsonArray getRect(String caseId, String username) {
        return getRect(caseId, username,null,null);
    }

    public JsonArray getRect(String caseId, String username,String minDiameter,String texture) {
        N1qlQueryResult result;

        if (username != null)
        {
            result = bucket.query(
                    N1qlQuery.parameterized("select rects from `bm_sys` where type = 'draft' and caseId = $1 and username = $2", JsonArray.from(caseId, username))
            );
        }else
        {
            result = bucket.query(
                    N1qlQuery.parameterized("select rects from `bm_sys` where type = 'draft' and caseId = $1 ", JsonArray.from(caseId))
            );
        }



        try {
            JsonObject obj = (JsonObject) result.allRows().get(0).value();
            JsonArray array = obj.getArray("rects");

            //debug
            System.out.println("rects:"+array);

            JsonArray retArray = JsonArray.create();
            for (int i = 0; i < array.size(); i ++) {
                String noduleId = (String) array.get(i);
                JsonObject nodule = bucket.get(noduleId).content();

                if (minDiameter != null && Double.parseDouble(minDiameter) > 0 && nodule.getDouble("diameter") < Double.parseDouble(minDiameter) )
                {
                    continue;
                }
                if (texture != null && !texture.equals("-1") && nodule.getInt("texture") != Integer.parseInt(texture))
                {
                    continue;
                }


                if(!nodule.containsKey("modified")){
                    int x1 = Math.max(nodule.getInt("x1") - 4, 0);
                    int y1 = Math.max(nodule.getInt("y1") - 4, 0);
                    int x2 = Math.min(nodule.getInt("x2") + 4, 512);
                    int y2 = Math.min(nodule.getInt("y2") + 4, 512);
                    nodule.put("x1", x1);
                    nodule.put("y1", y1);
                    nodule.put("x2", x2);
                    nodule.put("y2", y2);
                }
                retArray.add(nodule);
            }
            // debug
            System.out.println("retArray:"+retArray);

            return retArray;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public JsonObject getRectForFollow(String caseId1,String caseId2, String username) {

        JsonObject res = JsonObject.create();
        res.put("rects1",JsonArray.create());
        res.put("rects2",JsonArray.create());

        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select rects from `bm_sys` where type = 'draft' and caseId = $1 and username = $2", JsonArray.from(caseId1, username))
        );

        if(result.allRows().size() == 0){
            result = bucket.query(
                    N1qlQuery.parameterized("select rects from `bm_sys` where type = 'draft' and caseId = $1 and username = $2", JsonArray.from(caseId1, "bm_sys"))
            );
        }

        try {
            JsonObject obj = (JsonObject) result.allRows().get(0).value();
            JsonArray array = obj.getArray("rects");
            JsonArray retArray = JsonArray.create();
            for (int i = 0; i < array.size(); i ++) {
                String noduleId = (String) array.get(i);
                JsonObject nodule = bucket.get(noduleId).content();
                if(!nodule.containsKey("modified")){
                    int x1 = Math.max(nodule.getInt("x1") - 4, 0);
                    int y1 = Math.max(nodule.getInt("y1") - 4, 0);
                    int x2 = Math.min(nodule.getInt("x2") + 4, 512);
                    int y2 = Math.min(nodule.getInt("y2") + 4, 512);
                    nodule.put("x1", x1);
                    nodule.put("y1", y1);
                    nodule.put("x2", x2);
                    nodule.put("y2", y2);
                }
                retArray.add(nodule);
                res.put("rects1",retArray);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return res;
        }

        N1qlQueryResult result2 = bucket.query(
                N1qlQuery.parameterized("select rects from `bm_sys` where type = 'draft' and caseId = $1 and username = $2", JsonArray.from(caseId2, username))
        );

        if(result2.allRows().size() == 0){
            result2 = bucket.query(
                    N1qlQuery.parameterized("select rects from `bm_sys` where type = 'draft' and caseId = $1 and username = $2", JsonArray.from(caseId2, "bm_sys"))
            );
        }

        try {
            JsonObject obj = (JsonObject) result2.allRows().get(0).value();
            JsonArray array = obj.getArray("rects");
            JsonArray retArray = JsonArray.create();
            for (int i = 0; i < array.size(); i ++) {
                String noduleId = (String) array.get(i);
                JsonObject nodule = bucket.get(noduleId).content();
                if(!nodule.containsKey("modified")){
                    int x1 = Math.max(nodule.getInt("x1") - 4, 0);
                    int y1 = Math.max(nodule.getInt("y1") - 4, 0);
                    int x2 = Math.min(nodule.getInt("x2") + 4, 512);
                    int y2 = Math.min(nodule.getInt("y2") + 4, 512);
                    nodule.put("x1", x1);
                    nodule.put("y1", y1);
                    nodule.put("x2", x2);
                    nodule.put("y2", y2);
                }
                retArray.add(nodule);
                res.put("rects2",retArray);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return res;
        }

        return res;
    }

    public boolean updateRect(String caseId, String tokenUsername, JsonArray newRects) {
        JsonArray updatedRect = newRects;
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select *, meta(bm_sys).id as docKey from `bm_sys` where type = 'draft' and caseId = $1 and username = $2", JsonArray.from(caseId, tokenUsername))
        );
        if (result.allRows().size() == 0) {
            return false;
        }

        JsonObject obj = (JsonObject) result.allRows().get(0).value().get("bm_sys");
        String docKey = (String) result.allRows().get(0).value().get("docKey");
        JsonArray oldRects = (JsonArray) obj.get("rects");
        for (int j = 0; j < oldRects.size(); j ++) {
            bucket.remove((String) oldRects.get(j));
        }
        JsonArray rects = JsonArray.create();
        for (int i = 0; i < updatedRect.size(); i ++) {
            JsonObject rectObj = (JsonObject) updatedRect.get(i);
            if(!rectObj.containsKey("modified")){
                int x1 = rectObj.getInt("x1") + 4;
                int y1 = rectObj.getInt("y1") + 4 ;
                int x2 = rectObj.getInt("x2") - 4 ;
                int y2 = rectObj.getInt("y2") - 4 ;
                rectObj.put("x1", x1);
                rectObj.put("y1", y1);
                rectObj.put("x2", x2);
                rectObj.put("y2", y2);
            }

            rectObj.removeKey("highlight");
            rectObj.put("status", 0);
            String noduleId = tokenUsername + "#" + caseId + "#" + String.valueOf(i) + "@nodule";
            JsonDocument doc = JsonDocument.create(noduleId, rectObj);
            bucket.upsert(doc);
            rects.add(noduleId);
        }

        obj.put("rects", rects);
        obj.put("lastActive", String.valueOf((long) (System.currentTimeMillis() / 1000)));
        JsonDocument doc = JsonDocument.create(docKey, obj);
        try {
            bucket.upsert(doc);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean removeDraft(String username, String caseId) {
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("delete from `bm_sys` where type = 'draft' and username = $1 and caseId = $2", JsonArray.from(username, caseId))
        );

        String nodule_key = username + "#" + caseId + "%";
        N1qlQueryResult result2 = bucket.query(
                N1qlQuery.parameterized("delete from `bm_sys` where type = 'nodule' and meta(bm_sys).id like $1", JsonArray.from(nodule_key))
        );

        String status = result.status();
        String status2 = result2.status();
        if (status.equals("success") && status2.equals("success"))
            return true;
        else
            return false;
    }

    public boolean createUserDraftFromModel(String caseId, String tokenUsername, JsonArray newRectStr){
        JsonObject jsonObject = JsonObject.create();
        jsonObject.put("username", tokenUsername);
        jsonObject.put("status", "1");
        jsonObject.put("caseId", caseId);
        jsonObject.put("type", "draft");

        JsonArray rects = JsonArray.create();
        for (int i = 0; i < newRectStr.size(); i ++) {
            JsonObject rectObj = (JsonObject) newRectStr.get(i);
            if(!rectObj.containsKey("modified")){
                int x1 = rectObj.getInt("x1") + 4;
                int y1 = rectObj.getInt("y1") + 4 ;
                int x2 = rectObj.getInt("x2") - 4 ;
                int y2 = rectObj.getInt("y2") - 4 ;
                rectObj.put("x1", x1);
                rectObj.put("y1", y1);
                rectObj.put("x2", x2);
                rectObj.put("y2", y2);
            }
            rectObj.removeKey("highlight");
            String noduleId = tokenUsername + "#" + caseId + "#" + String.valueOf(i) + "@nodule";
            JsonDocument doc = JsonDocument.create(noduleId, rectObj);
            bucket.upsert(doc);
            rects.add(noduleId);
        }
        jsonObject.put("rects", rects);
        jsonObject.put("lastActive", String.valueOf((long) (System.currentTimeMillis() / 1000)));

//        String keyBeforeHash = tokenUsername + "@" + caseId + "_draft";
        String key = tokenUsername + "#" + caseId + "@draft";
        JsonDocument doc = JsonDocument.create(key, jsonObject);
        try{
            bucket.upsert(doc);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    public boolean createDraft(String caseId, String tokenUsername, String username) {
        JsonObject jsonObject = JsonObject.create();
        if (username.equals("origin")) {
            jsonObject.put("username", tokenUsername);
            jsonObject.put("status", "0");
            jsonObject.put("caseId", caseId);
            jsonObject.put("type", "draft");
            jsonObject.put("rects", new ArrayList<>());
            // insert a plain one
        } else {
            N1qlQueryResult result = bucket.query(
                    N1qlQuery.parameterized("select * from `bm_sys` where type = 'draft' and caseId = $1 and username = $2", JsonArray.from(caseId, username))
            );

            jsonObject = (JsonObject) result.allRows().get(0).value().get("bm_sys");
            JsonArray oldRects = (JsonArray) jsonObject.get("rects");
            JsonArray newRects = JsonArray.create();
            for (int i = 0; i < oldRects.size(); i ++) {
                String noduleId = (String) oldRects.get(i);
                String newNoduleId = noduleId.replace(username, tokenUsername);
                JsonObject rect = bucket.get(noduleId).content();
//                rect.removeKey("texture");
//                rect.removeKey("calcification");
//                rect.removeKey("spiculation");
//                rect.removeKey("lobulation");
                rect.removeKey("huMin");
                rect.removeKey("huMax");
                rect.removeKey("huMean");
                rect.removeKey("volume");
                rect.put("status", 0);
                JsonDocument noduleDoc = JsonDocument.create(newNoduleId, rect);
                bucket.upsert(noduleDoc);
                newRects.add(newNoduleId);
            }
            jsonObject.put("username", tokenUsername);
            jsonObject.put("rects", newRects);
            jsonObject.put("status", "0");
        }

        jsonObject.put("lastActive", String.valueOf((long) (System.currentTimeMillis() / 1000)));

//        String keyBeforeHash = tokenUsername + "@" + caseId + "_draft";
        String key = tokenUsername + "#" + caseId + "@draft";
        JsonDocument doc = JsonDocument.create(key, jsonObject);
        try{
            bucket.upsert(doc);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public ArrayList<Object> getDraftsForUsername(String username, String status) {
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select caseId, lastActive from `bm_sys` where type = 'draft' and username = $1 and status = $2", JsonArray.from(username, status))
        );
        ArrayList<Object> retList = new ArrayList<>();
        for (N1qlQueryRow row : result) {
            JsonObject jo = row.value();
            String caseId = (String) row.value().get("caseId");
            String lastActive = (String) row.value().get("lastActive");
            HashMap<String, String> tmp = new HashMap<>();
            tmp.put("caseId", caseId);
            tmp.put("lastActive", lastActive);
            retList.add(tmp);
        }
        return retList;
    }

    public boolean changeDraftStatus(String username, String caseId, String newStatus) {
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select *, meta(bm_sys).id as docKey from `bm_sys` where type = 'draft' and caseId = $1 and username = $2", JsonArray.from(caseId, username))
        );
        JsonObject draftContent = (JsonObject) result.allRows().get(0).value().get("bm_sys");
        String key = (String) result.allRows().get(0).value().get("docKey");
        draftContent.put("status", newStatus);
        JsonDocument doc = JsonDocument.create(key, draftContent);
        try {
            bucket.upsert(doc);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 获取病例信息
    public JsonObject getReportInfo(String caseId, String username){

        JsonObject res = JsonObject.create();

        N1qlQueryResult result1 = bucket.query(
                N1qlQuery.parameterized("select patientId,date from `fetal_sys` where type = 'record' and caseId = $1", JsonArray.from(caseId))
        );

        JsonObject recordInfo = (JsonObject) result1.allRows().get(0).value();
        String patientID = recordInfo.getString("patientId");
        String date = recordInfo.getString("date");

        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select patientBirth,patientSex,patientName from `fetal_sys` where type = 'info' and patientId = $1", JsonArray.from(patientID))
        );

        JsonObject obj = (JsonObject) result.allRows().get(0).value();
        String patientBirth = obj.getString("patientBirth");
        String patientSex = obj.getString("patientSex");
        String patientName = obj.getString("patientName");

        String birthYear = "";//如果长度大于4，就是取前4位为出生年，否则为空
        if(patientBirth.length() >4){
            birthYear = patientBirth.substring(0,4);
        }
        Calendar currentdate = Calendar.getInstance();
        String curentYear = String.valueOf(currentdate.get(Calendar.YEAR));

        res.put("patientName",patientName);
        res.put("patientBirth",patientBirth);
        res.put("patientSex",patientSex);
        res.put("patientID",patientID);
        res.put("date",date);
        int age = 0;
        try{
            if(!birthYear.isEmpty()){
                age = Integer.parseInt(curentYear)-Integer.parseInt(birthYear);
            }
            res.put("age",age);
        }catch (Exception e){
            res.put("age",0);
        }

        System.out.println("getReportInfo:"+res);
        return res;

    }

    public JsonArray getLobeInfo(String caseId){
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select lobes from `bm_sys` where type = 'draft' and caseId = $1 and username = 'bm_sys'", JsonArray.from(caseId))
        );
        try {
            JsonObject obj = (JsonObject) result.allRows().get(0).value();
            JsonArray array = obj.getArray("lobes");
            return array;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    // 数据库查找centerline
    // fetal_sys中未存入，或与MPR有关
    public JsonArray getCenterLine(String caseId){
//        N1qlQueryResult result = bucket.query(
//                N1qlQuery.parameterized("select centerline from `bm_sys` where type = 'vesseldraft' and caseId = $1 and username = 'bm_sys'", JsonArray.from(caseId))
//        );
//        try {
//            JsonObject obj = (JsonObject) result.allRows().get(0).value();
//            JsonArray array = obj.getArray("centerline");
//            return array;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
        return null;

    }

    public JsonObject getRectsForFollowUp(String earlierCaseId,String earlierUsername,String laterCaseId,String laterUsername){
        JsonArray earlierRectsDocs = this.getRectsByCaseIdAndUsername(earlierCaseId, earlierUsername);
        JsonArray laterRectsDocs = this.getRectsByCaseIdAndUsername(laterCaseId, laterUsername);
        JsonArray earlierRects;
        JsonArray laterRects;
        if(earlierRectsDocs == null ){
            earlierRects = JsonArray.create();
        }else {
            earlierRects = this.getRectsByDocIds(earlierRectsDocs);
        }
        if(laterRectsDocs == null ){
            laterRects = JsonArray.create();
        }else {
            laterRects = this.getRectsByDocIds(laterRectsDocs);
        }

        JsonArray earlier_copy = JsonArray.from(earlierRects);
        JsonArray later_copy = JsonArray.from(laterRects);
        String patientId = getPatientIdByCaseId(earlierCaseId);
        JsonArray noduleMatches = getNoduleMatch(patientId);
        JsonArray matchArrayForEarlier = JsonArray.create();
        JsonArray matchArrayForLater = JsonArray.create();
        JsonArray matchPairs = JsonArray.create();  //这里面只保存和这两例病例相关的match组
        if (noduleMatches != null && !earlierCaseId.equals(laterCaseId)){
            for (int i = 0; i < noduleMatches.size(); i++) {
                JsonArray match = noduleMatches.getArray(i);
                int earlierMatchIndex = findMatchIndex(match, earlierRectsDocs);
                int laterMatchIndex = findMatchIndex(match, laterRectsDocs);
                if (earlierMatchIndex != -1 && laterMatchIndex != -1){
                    JsonArray matchPair = JsonArray.create();//这里面只保存和这两例病例相关的match
                    matchArrayForEarlier.add(earlierRects.get(earlierMatchIndex));
                    matchPair.add(earlierRectsDocs.get(earlierMatchIndex));
                    earlierRectsDocs = removeElementByIndex(earlierRectsDocs,earlierMatchIndex);
                    earlierRects = removeElementByIndex(earlierRects,earlierMatchIndex);
                    matchArrayForLater.add(laterRects.get(laterMatchIndex));
                    matchPair.add(laterRectsDocs.get(laterMatchIndex));
                    laterRectsDocs = removeElementByIndex(laterRectsDocs,laterMatchIndex);
                    laterRects = removeElementByIndex(laterRects,laterMatchIndex);
                    matchPairs.add(matchPair);
                }
            }
        }

        JsonObject returnObject = JsonObject.create();

        //laterRects没有匹配成功的，说明是新增的，加入到newMap中
        returnObject.put("new",laterRects);
        //earlierRects没有匹配成功的，说明已经消失了，加入到vanishMap中
        returnObject.put("vanish",earlierRects);

        JsonArray matchArray = JsonArray.create();
        for (int i = 0; i < matchArrayForEarlier.size(); i++) {
            JsonObject matchObject = JsonObject.create();
            matchObject.put("later",matchArrayForLater.get(i));
            matchObject.put("earlier",matchArrayForEarlier.get(i));
            matchArray.add(matchObject);
        }

        returnObject.put("match",matchArray);
        returnObject.put("earlier",earlier_copy.get(0));
        returnObject.put("later",later_copy.get(0));
        returnObject.put("matchPairs",matchPairs);

        returnObject.put("patientId",patientId);
        return returnObject;
    }

    public int findMatchIndex(JsonArray matchSequence,JsonArray rects){
        if(matchSequence == null || rects==null) return -1;
        String[] matchSequenceArr = new String[matchSequence.size()];
        for (int i = 0; i < matchSequence.size(); i++) {
            matchSequenceArr[i] = matchSequence.getString(i);
        }
        List<String> matchSequenceList = Arrays.asList(matchSequenceArr);
        for (int i = 0; i < rects.size(); i++) {
            if(matchSequenceList.contains(rects.getString(i))){
                return i;
            }
        }
        return -1;
    }

    public JsonArray getRectsByDocIds(JsonArray noduleIdArray){
        JsonArray returnArray = JsonArray.create();
        for (int i = 0; i < noduleIdArray.size(); i++) {
            String noduleId = noduleIdArray.getString(i);
            JsonObject nodule = bucket.get(noduleId).content();
            nodule.put("documentId",noduleId);
            if(!nodule.containsKey("modified")){
                int x1 = Math.max(nodule.getInt("x1") - 4, 0);
                int y1 = Math.max(nodule.getInt("y1") - 4, 0);
                int x2 = Math.min(nodule.getInt("x2") + 4, 512);
                int y2 = Math.min(nodule.getInt("y2") + 4, 512);
                nodule.put("x1", x1);
                nodule.put("y1", y1);
                nodule.put("x2", x2);
                nodule.put("y2", y2);
            }
            returnArray.add(nodule);
        }
        return returnArray;
    }

    // 获取patientID
    public String getPatientIdByCaseId(String caseId){
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select patientId from `fetal_sys` where type = 'record' and caseId = $1", JsonArray.from(caseId))
        );
        if (result.allRows().size()==0) return null;
        JsonObject obj = (JsonObject) result.allRows().get(0).value();
        String patientId = obj.getString("patientId");
        return patientId;
    }

    public JsonArray getNoduleMatch(String patientId){
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select match from `bm_sys` where type = 'nodule_match' and patientId = $1", JsonArray.from(patientId))
        );

        if (result.allRows().size()==0) return null;

        JsonObject obj = (JsonObject) result.allRows().get(0).value();
        JsonArray rectsArray = obj.getArray("match");
        return rectsArray;
    }

    public JsonArray getRectsByCaseIdAndUsername(String caseId,String username){
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select rects from `bm_sys` where type = 'draft' and caseId = $1 and username = $2", JsonArray.from(caseId, username))
        );

        if (result.allRows().size()==0) return null;

        JsonObject obj = (JsonObject) result.allRows().get(0).value();
        JsonArray rectsArray = obj.getArray("rects");
        return rectsArray;
    }

    public JsonArray removeElementByIndex(JsonArray array, int index){
        JsonArray retArray = JsonArray.create();
        for (int i = 0; i < array.size(); i++) {
            if (i != index)
                retArray.add(array.get(i));
        }
        return retArray;
    }


    public boolean isPreprocessOk(String caseId){
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select username from `fetal_sys` where type = 'draft' and caseId = $1", JsonArray.from(caseId))
        );
        try {
            if(result.allRows().size() == 0){
                //draft表中没有查到数据，说明正在处理中，属于正常情况，返回true.
                return true;
            }

            ArrayList<String> usernameList = new ArrayList<>();
            for (int i = 0; i < result.allRows().size(); i++) {
                usernameList.add(result.allRows().get(i).value().getString("username"));
            }
            //只要有模型处理结果，就认为是处理成功了
            if (usernameList.contains("fteal_sys")) {
                return true;
            }else if(usernameList.contains("error")){
                //如果没有bm_sys，但是有error，则人物是处理失败
                return false;
            }else {
                return true;
            }

        } catch (Exception e) {
            return false;
        }
    }

    public JsonArray getLymph(String caseId, String username) {
        N1qlQueryResult result;

        if (username != null)
        {
            result = bucket.query(
                    N1qlQuery.parameterized("select rects from `bm_sys` where type = 'lymphdraft' and caseId = $1 and username = $2", JsonArray.from(caseId, username))
            );
        }else

        {
            result = bucket.query(
                    N1qlQuery.parameterized("select rects from `bm_sys` where type = 'draft' and caseId = $1 ", JsonArray.from(caseId))
            );
        }


        try {
            JsonObject obj = (JsonObject) result.allRows().get(0).value();
            JsonArray array = obj.getArray("rects");
            JsonArray retArray = JsonArray.create();
            for (int i = 0; i < array.size(); i ++) {
                String noduleId = (String) array.get(i);
                JsonObject nodule = bucket.get(noduleId).content();
                nodule.put("documentId",noduleId);
                retArray.add(nodule);
            }
            return retArray;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public JsonObject getPatienIDandHorbBycaseId(String caseId){
        ArrayList<String> ret = new ArrayList<>();
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select patientId, horb from `fetal_sys` where type = 'record' and caseId = $1", JsonArray.from(caseId))
        );
        if (result.allRows().size() == 0){
            return null;
        }
        JsonObject patientInfo = (JsonObject) result.allRows().get(0).value();
        return patientInfo;

    }

    public String getBirthByPatientId(String patientId){
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select patientBirth from `fetal_sys` where type = 'info' and patientId = $1", JsonArray.from(patientId))
        );
        if (result.allRows().size() == 0){
            return null;
        }
        String patientBirth = result.allRows().get(0).value().getString("patientBirth");
        return patientBirth;

    }

    // 获取所有的diameter, volume, area
    public ArrayList<HashMap<String, Object>> getDiameterTest() {
        ArrayList<JsonArray> ret = new ArrayList<>();
        String n1qlQuery = "select meta(fetal_sys).id as docKey, diameter, volume, area from fetal_sys where type = 'draft' and (status = '1' or status = '2') ";

        N1qlQueryResult result = bucket.query(N1qlQuery.simple(n1qlQuery));

        ArrayList<HashMap<String, Object>> ary = new ArrayList<>();
        // 在fetal_sys中数据分布于不同jsonarray中，需要遍历所有的jsonarray
        // 查找所有的diameter, volume, area

        for (N1qlQueryRow row: result) {
            // debug
//            System.out.println("row_value:"+row.value());
            HashMap<String, Object> tmp = new HashMap<>();
            JsonArray diameter = row.value().getArray("diameter");
            JsonArray volume = row.value().getArray("volume");
            JsonArray area = row.value().getArray("area");
            // debug
//            System.out.println("diameter"+diameter);
            for(int i = 0; i < diameter.size(); i++){
                JsonObject rowJSON = diameter.getObject(i);
                if (Objects.equals(rowJSON.getString("name"), "CBPD")){
                    tmp.put("CBPD", rowJSON.getDouble("value"));
                }else if (Objects.equals(rowJSON.getString("name"), "TCD")){
                    tmp.put("TCD", rowJSON.getDouble("value"));
                }else if (Objects.equals(rowJSON.getString("name"), "SW")){
                    tmp.put("SW", rowJSON.getDouble("value"));
                }else if (Objects.equals(rowJSON.getString("name"), "SW")) {
                    tmp.put("SW", rowJSON.getDouble("value"));
                }else if (Objects.equals(rowJSON.getString("name"), "SH")) {
                    tmp.put("SH", rowJSON.getDouble("value"));
                }else if (Objects.equals(rowJSON.getString("name"), "TAW")) {
                    tmp.put("TAW", rowJSON.getDouble("value"));
                }else if (Objects.equals(rowJSON.getString("name"), "LAD")) {
                    tmp.put("LAD", rowJSON.getDouble("value"));
                }else if (Objects.equals(rowJSON.getString("name"), "RAD")) {
                    tmp.put("RAD", rowJSON.getDouble("value"));
                }
            }

            for(int i = 0; i < volume.size(); i++){
                JsonObject rowJSON = volume.getObject(i);
                if (Objects.equals(rowJSON.getString("name"), "LHV")){
                    tmp.put("LHV", rowJSON.getDouble("value"));
                }else if (Objects.equals(rowJSON.getString("name"), "RHV")){
                    tmp.put("RHV", rowJSON.getDouble("value"));
                }
            }

            for(int i = 0; i < area.size(); i++){
                JsonObject rowJSON = area.getObject(i);
                if (Objects.equals(rowJSON.getString("name"), "TA")){
                    tmp.put("TA", rowJSON.getDouble("value"));
                }
            }

            String docKey = row.value().getString("docKey");
            //debug
//            System.out.println("subdockey:"+docKey);
            int caseIdStartIndex = docKey.indexOf("#") + 1;
            int caseIdEndIndex = docKey.lastIndexOf("@");

            String caseId_sub = docKey.substring(caseIdStartIndex, caseIdEndIndex);
            String username = row.value().getString("docKey").split("#")[0];

            tmp.put("caseId", caseId_sub);
            tmp.put("username", username);

            JsonObject patienInfo = getPatienIDandHorbBycaseId(caseId_sub);
            if (patienInfo != null){
                String patientID = patienInfo.getString("patientId");
                tmp.put("patientID", patientID);
                int horb = patienInfo.getInt("horb");
                tmp.put("horb", horb);
                String patientBirth = getBirthByPatientId(patientID);
                tmp.put("patientBirth", patientBirth);

            }else {
                tmp.put("patientID", null);
                tmp.put("horb", null);
                tmp.put("patientBirth", null);
            }
            ary.add(tmp);
        }

        return ary;

    }

    // 用于postman测试
    public ArrayList<JsonObject> postTest2() {
        ArrayList<JsonObject> ret = new ArrayList<>();
        String n1qlQuery = "select meta(fetal_sys).id as docKey, V from fetal_sys as F unnest F.volume as V where  type = 'draft' and (status = '1' or status = '2') ";

        N1qlQueryResult result = bucket.query(N1qlQuery.simple(n1qlQuery));
        for (N1qlQueryRow row: result) {
            System.out.println("postTest2:"+row.value());
            ret.add(row.value());
        }
        return ret;
    }

    // 构建默认参数
    public JsonArray getPoints(String caseId, String username) {
        return getPoints(caseId, username,null,null);
    }
    // 获取fetal_sys中diameter的点坐标
    public JsonArray getPoints(String caseId, String username,String week,String texture) {
        N1qlQueryResult result;
        if (username != null)
        {
            result = bucket.query(
                    N1qlQuery.parameterized("select diameter,volume,area from `fetal_sys` where type = 'draft' and caseId = $1 and username = $2", JsonArray.from(caseId, username))
            );
        }else
        {
            result = bucket.query(
                    N1qlQuery.parameterized("select diameter,volume,area from `fetal_sys" +
                            "` where type = 'draft' and caseId = $1 ", JsonArray.from(caseId))
            );
        }

        try {
            JsonObject obj = (JsonObject) result.allRows().get(0).value();
            JsonArray diameter = obj.getArray("diameter");
            JsonArray volume = obj.getArray("volume");
            JsonObject area = obj.getArray("area").getObject(0);

            JsonArray retArray = JsonArray.create();
            JsonObject slice_idx_AreaAndVolume = JsonObject.create();

            // 在fetal_sys中数据分布于不同jsonarray中，需要遍历所有的jsonarray
            // 查找所有的diameter的点
            // texture 1长度 2宽度 3面积 4体积
            for(int i = 0; i < diameter.size(); i++){
                JsonObject tmp = JsonObject.create();
                JsonObject tmp2 = JsonObject.create();
                JsonObject rowJSON = diameter.getObject(i);
                tmp.put("nodule_no", i);
                if (Objects.equals(rowJSON.getString("name"), "CBPD")){
                    tmp.put("slice_idx", rowJSON.getArray("point1").get(2));
                    tmp.put("name", rowJSON.getString("name"));
                    tmp.put("x1", rowJSON.getArray("point1").get(1));
                    tmp.put("x2", rowJSON.getArray("point2").get(1));
                    tmp.put("y1", rowJSON.getArray("point1").get(0));
                    tmp.put("y2", rowJSON.getArray("point2").get(0));

                    tmp2.put("x1", rowJSON.getArray("point1").get(1));
                    tmp2.put("x2", rowJSON.getArray("point2").get(1));
                    tmp2.put("y1", rowJSON.getArray("point1").get(0));
                    tmp2.put("y2", rowJSON.getArray("point2").get(0));

                    tmp.put("measure",tmp2);
                    tmp.put("texture", 2);


                }else if (Objects.equals(rowJSON.getString("name"), "TCD")){
                    slice_idx_AreaAndVolume.put("slice_idx_TCD", rowJSON.getArray("point1").get(2));
                    tmp.put("slice_idx", rowJSON.getArray("point1").get(2));
                    tmp.put("name", rowJSON.getString("name"));
                    tmp.put("x1", rowJSON.getArray("point1").get(1));
                    tmp.put("x2", rowJSON.getArray("point2").get(1));
                    tmp.put("y1", rowJSON.getArray("point1").get(0));
                    tmp.put("y2", rowJSON.getArray("point2").get(0));

                    tmp2.put("x1", rowJSON.getArray("point1").get(1));
                    tmp2.put("x2", rowJSON.getArray("point2").get(1));
                    tmp2.put("y1", rowJSON.getArray("point1").get(0));
                    tmp2.put("y2", rowJSON.getArray("point2").get(0));

                    tmp.put("measure",tmp2);
                    tmp.put("texture", 2);

                }else if (Objects.equals(rowJSON.getString("name"), "SW")){
                    tmp.put("slice_idx", rowJSON.getArray("point1").get(2));
                    tmp.put("name", rowJSON.getString("name"));
                    tmp.put("x1", rowJSON.getArray("point1").get(1));
                    tmp.put("x2", rowJSON.getArray("point2").get(1));
                    tmp.put("y1", rowJSON.getArray("point1").get(0));
                    tmp.put("y2", rowJSON.getArray("point2").get(0));

                    tmp2.put("x1", rowJSON.getArray("point1").get(1));
                    tmp2.put("x2", rowJSON.getArray("point2").get(1));
                    tmp2.put("y1", rowJSON.getArray("point1").get(0));
                    tmp2.put("y2", rowJSON.getArray("point2").get(0));

                    tmp.put("measure",tmp2);
                    tmp.put("texture", 1);

                }else if (Objects.equals(rowJSON.getString("name"), "SH")) {
                    tmp.put("slice_idx", rowJSON.getArray("point1").get(2));
                    tmp.put("name", rowJSON.getString("name"));
                    tmp.put("x1", rowJSON.getArray("point1").get(1));
                    tmp.put("x2", rowJSON.getArray("point2").get(1));
                    tmp.put("y1", rowJSON.getArray("point1").get(0));
                    tmp.put("y2", rowJSON.getArray("point2").get(0));

                    tmp2.put("x1", rowJSON.getArray("point1").get(1));
                    tmp2.put("x2", rowJSON.getArray("point2").get(1));
                    tmp2.put("y1", rowJSON.getArray("point1").get(0));
                    tmp2.put("y2", rowJSON.getArray("point2").get(0));

                    tmp.put("measure",tmp2);
                    tmp.put("texture", 2);

                }else if (Objects.equals(rowJSON.getString("name"), "TAW")) {
                    tmp.put("slice_idx", rowJSON.getArray("point1").get(2));
                    tmp.put("name", rowJSON.getString("name"));
                    tmp.put("x1", rowJSON.getArray("point1").get(1));
                    tmp.put("x2", rowJSON.getArray("point2").get(1));
                    tmp.put("y1", rowJSON.getArray("point1").get(0));
                    tmp.put("y2", rowJSON.getArray("point2").get(0));

                    tmp2.put("x1", rowJSON.getArray("point1").get(1));
                    tmp2.put("x2", rowJSON.getArray("point2").get(1));
                    tmp2.put("y1", rowJSON.getArray("point1").get(0));
                    tmp2.put("y2", rowJSON.getArray("point2").get(0));

                    tmp.put("measure",tmp2);
                    tmp.put("texture", 1);

                }else if (Objects.equals(rowJSON.getString("name"), "LAD")) {
                    slice_idx_AreaAndVolume.put("slice_idx_LRAD", rowJSON.getArray("point1").get(2));
                    tmp.put("slice_idx", rowJSON.getArray("point1").get(2));
                    tmp.put("name", rowJSON.getString("name"));
                    tmp.put("x1", rowJSON.getArray("point1").get(1));
                    tmp.put("x2", rowJSON.getArray("point2").get(1));
                    tmp.put("y1", rowJSON.getArray("point1").get(0));
                    tmp.put("y2", rowJSON.getArray("point2").get(0));

                    tmp2.put("x1", rowJSON.getArray("point1").get(1));
                    tmp2.put("x2", rowJSON.getArray("point2").get(1));
                    tmp2.put("y1", rowJSON.getArray("point1").get(0));
                    tmp2.put("y2", rowJSON.getArray("point2").get(0));

                    tmp.put("measure",tmp2);
                    tmp.put("texture", 1);

                }else if (Objects.equals(rowJSON.getString("name"), "RAD")) {
                    tmp.put("slice_idx", rowJSON.getArray("point1").get(2));
                    tmp.put("name", rowJSON.getString("name"));
                    tmp.put("x1", rowJSON.getArray("point1").get(1));
                    tmp.put("x2", rowJSON.getArray("point2").get(1));
                    tmp.put("y1", rowJSON.getArray("point1").get(0));
                    tmp.put("y2", rowJSON.getArray("point2").get(0));

                    tmp2.put("x1", rowJSON.getArray("point1").get(1));
                    tmp2.put("x2", rowJSON.getArray("point2").get(1));
                    tmp2.put("y1", rowJSON.getArray("point1").get(0));
                    tmp2.put("y2", rowJSON.getArray("point2").get(0));

                    tmp.put("measure",tmp2);
                    tmp.put("texture", 1);
                }

                retArray.add(tmp);
            }

            for(int i = 0; i < volume.size(); i++) {
                JsonObject tmp = JsonObject.create();
//                JsonObject tmp2 = JsonObject.create();
                JsonObject rowJSON = volume.getObject(i);
                tmp.put("nodule_no",diameter.size()+i);
                tmp.put("name", rowJSON.getString("name"));
                tmp.put("value", rowJSON.getDouble("value"));
                tmp.put("texture", 4);
                tmp.put("slice_idx", slice_idx_AreaAndVolume.get("slice_idx_LRAD"));
                retArray.add(tmp);
            }

            JsonObject tmp = JsonObject.create();
            tmp.put("nodule_no", volume.size()+diameter.size());
            tmp.put("name", area.getString("name"));
            tmp.put("value", area.getDouble("value"));
            tmp.put("texture", 3);
            tmp.put("slice_idx", slice_idx_AreaAndVolume.get("slice_idx_TCD"));

            retArray.add(tmp);
            // debug
//            System.out.println("retArray:"+retArray);
            return retArray;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}
