package me.sihang.backend.controller;

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.google.gson.Gson;
import me.sihang.backend.service.DraftService;
import me.sihang.backend.service.RecordService;
import me.sihang.backend.service.TokenService;
import me.sihang.backend.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/draft")
@CrossOrigin
public class DraftCotroller {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    private final DraftService draftServ;
    private final TokenService tokenServ;
    private final RecordService recordService;


    @Autowired
    public DraftCotroller(DraftService draftServ, TokenService tokenServ, RecordService recordService) {
        this.draftServ = draftServ;
        this.tokenServ = tokenServ;
        this.recordService = recordService;
    }

    @PostMapping(value = "getDataPath")
    public String getDataPath(@RequestHeader Map<String, String> headers, String caseId, String username) {
        System.out.println(caseId+username+headers);
        ArrayList<String> modelUsernames = draftServ.getModelResults(caseId);
        String ret = "";
        if (modelUsernames == null || modelUsernames.isEmpty())
            ret = "origin";
        else
            if(modelUsernames.contains("bm_sys"))
                ret = "bm_sys"; // return default bm_sys
            else{
                ret = modelUsernames.get(0);
            }

        if (!headers.containsKey("authorization")) {

            if(username != null){
                String currentUsername = username;
                boolean isDraftExisted = draftServ.isDraftExisted(currentUsername, caseId);
                if (isDraftExisted)
                    ret = currentUsername;
                return ret;
            }else{
                return ret;
            }
        }
        String token = headers.get("authorization").split("Bearer ")[1];
        Map<String, String> retMap = tokenServ.validToken(token);
        if (retMap.get("status").equals("failed")) {
            return ret;
        } else {
            String currentUsername = retMap.get("username");
            boolean isDraftExisted = draftServ.isDraftExisted(currentUsername, caseId);
            if (isDraftExisted)
                ret = currentUsername;
            return ret;
        }
    }

    @PostMapping(value = "getRectsForCaseIdAndUsername")
    public String getRectsForCaseIdAndUsername(String caseId, String username,String minDiameter,String texture) {
        try{
            Double.parseDouble(minDiameter);
        }catch (Exception e){
            minDiameter = "0";
        }

//        JsonArray rects = draftServ.getRect(caseId, username,minDiameter,texture);
        JsonArray rects = draftServ.getPoints(caseId, username,minDiameter,texture);
        if (rects == null)
            return "[]";
        List<Object> lst = rects.toList();
        return new Gson().toJson(lst);
    }

    @PostMapping(value = "getRectsForFollowAndUsername")
    public String getRectsForCaseIdAndUsername(String caseId1,String caseId2, String username) {
        JsonObject rects = draftServ.getRectForFollow(caseId1,caseId2, username);
        if (rects == null)
            return null;
        Map<String,Object> res = rects.toMap();
        return new Gson().toJson(res);
    }

    @PostMapping(value = "getRectsForCaseId_APP")
    public String getRectsForCaseIdAndUsername(String caseId) {
        JsonArray rects = draftServ.getRect(caseId, "bm_sys");
        if (rects == null)
            return null;
        List<Object> lst = rects.toList();
        return new Gson().toJson(lst);
    }

    @PostMapping(value = "getReportInfoForCaseIdAndUsername")
    public String getReportInfoForCaseIdAndUsername(String caseId, String username) {
        JsonObject reportInfo = draftServ.getReportInfo(caseId, username);
        // debug
//        System.out.println("reportInfo:"+reportInfo);

        Map<String,Object> res = reportInfo.toMap();
        return new Gson().toJson(res);
    }

    @PostMapping(value = "removeDraft")
    public boolean removeDraft(@RequestHeader Map<String, String> headers, String caseId) {
        String token = headers.get("authorization").split("Bearer ")[1];
        Map<String, String> retMap = new HashMap<>();
        Map<String, String> servMap = tokenServ.validToken(token);
        retMap.put("status", "failed");
        if (!servMap.get("status").equals("okay")) {
            return false;
        }
        String username = servMap.get("username");
        return draftServ.removeDraft(username, caseId);

    }

    @PostMapping(value = "createNewDraft")
    public String createNewDraft(@RequestHeader Map<String, String> headers, String caseId, String username) {
        String token = headers.get("authorization").split("Bearer ")[1];
        Map<String, String> retMap = new HashMap<>();
        Map<String, String> servMap = tokenServ.validToken(token);
        retMap.put("status", "failed");
        if (!servMap.get("status").equals("okay")) {
            return new Gson().toJson(retMap);
        }
        String tokenUsername = servMap.get("username");
//        if (!tokenUsername.equals(username)) {
//            return new Gson().toJson(retMap);
//        }
        boolean isDraftExistedForUsername = draftServ.isDraftExisted(username, caseId);
        boolean isDraftExistedForTokenUsername = draftServ.isDraftExisted(tokenUsername, caseId);
        if (!isDraftExistedForUsername && !username.equals("origin")) {
            retMap.replace("status", "cannotFork");
            return new Gson().toJson(retMap);
        }
        if (isDraftExistedForTokenUsername) {
            retMap.replace("status", "alreadyExisted");
            String nextPath = "/case/" + caseId + "/" + tokenUsername;
            retMap.put("nextPath", nextPath);
            return new Gson().toJson(retMap);
        }

        boolean createSuc = draftServ.createDraft(caseId, tokenUsername, username);

        if (createSuc) {
            retMap.replace("status", "okay");
            String nextPath = "/case/" + caseId + "/" + tokenUsername;
            retMap.put("nextPath", nextPath);
            return new Gson().toJson(retMap);
        } else
            return new Gson().toJson(retMap);
    }

    @PostMapping(value = "createUserDraftFromModel")
    public String createUserDraftFromModel(@RequestHeader Map<String, String> headers, String caseId, String newRectStr) {
        LOGGER.info("caseId:"+caseId);
        LOGGER.info("newRectStr:"+newRectStr);
        String token = headers.get("authorization").split("Bearer ")[1];
        Map<String, String> retMap = new HashMap<>();
        Map<String, String> servMap = tokenServ.validToken(token);
        retMap.put("status", "failed");
        if (!servMap.get("status").equals("okay")) {
            return new Gson().toJson(retMap);
        }
        String tokenUsername = servMap.get("username");

        boolean isDraftExistedForTokenUsername = draftServ.isDraftExisted(tokenUsername, caseId);

        if (isDraftExistedForTokenUsername) {
            retMap.replace("status", "alreadyExisted");
            String nextPath = "/case/" + caseId + "/" + tokenUsername;
            retMap.put("nextPath", nextPath);
            return new Gson().toJson(retMap);
        }

        JsonArray newRects = JsonArray.fromJson(newRectStr);
        boolean createSuc = draftServ.createUserDraftFromModel(caseId, tokenUsername, newRects);

        if (createSuc) {
            retMap.replace("status", "okay");
            String nextPath = "/case/" + caseId + "/" + tokenUsername;
            retMap.put("nextPath", nextPath);
            return new Gson().toJson(retMap);
        } else
            return new Gson().toJson(retMap);
    }

    @PostMapping(value = "updateRects")
    public String updateRects(@RequestHeader Map<String, String> headers, String caseId, String newRectStr, String username) {
        String token = headers.get("authorization").split("Bearer ")[1];
        Map<String, String> retMap = new HashMap<>();
        retMap.put("status", "failed");
        Map<String, String> servMap = tokenServ.validToken(token);
        if (!servMap.get("status").equals("okay")) {
            return new Gson().toJson(retMap);
        }
        //String tokenUsername = servMap.get("username");
        JsonArray newRects = JsonArray.fromJson(newRectStr);
        boolean updateSuc = draftServ.updateRect(caseId, username, newRects);
        if(updateSuc){
            retMap.put("status", "ok");
            return new Gson().toJson(retMap);
        }else {
            return new Gson().toJson(retMap);
        }
    }

    @PostMapping(value = "getModelResults")
    public String getModelResults(@RequestHeader Map<String, String> headers, String caseId) {
        System.out.println("函数: "+ caseId);
        ArrayList<String> modelUsernames = draftServ.getModelResults(caseId);
        Map<String, Object> retMap = new HashMap<>();
        retMap.put("status", "okay");
        retMap.put("dataList", modelUsernames);
        return new Gson().toJson(retMap);
    }

    @PostMapping(value = "getAnnoResults")
    public String getAnnoResults(@RequestHeader Map<String, String> headers, String caseId) {
        ArrayList<String> modelUsernames = draftServ.getAnnoResults(caseId);
        Map<String, Object> retMap = new HashMap<>();
        retMap.put("status", "okay");
        retMap.put("dataList", modelUsernames);
        return new Gson().toJson(retMap);
    }

    @PostMapping(value = "submitDraft")
    public boolean submitDraft(@RequestHeader Map<String, String> headers, String caseId, String username) {
        String token = headers.get("authorization").split("Bearer ")[1];
        Map<String, String> tokenMap = tokenServ.validToken(token);
        if (!tokenMap.get("status").equals("okay")) {
            return false;
        } else {
            //String username = tokenMap.get("username");
            return draftServ.changeDraftStatus(username, caseId, "1");
        }
    }

    @PostMapping(value = "deSubmitDraft")
    public boolean deSubmitDraft(@RequestHeader Map<String, String> headers, String caseId, String username) {
        String token = headers.get("authorization").split("Bearer ")[1];
        Map<String, String> tokenMap = tokenServ.validToken(token);
        if (!tokenMap.get("status").equals("okay")) {
            return false;
        } else {
            //String username = tokenMap.get("username");
            return draftServ.changeDraftStatus(username, caseId, "0");
        }
    }

    @PostMapping(value = "isReadonly")
    public String isReadOnly(@RequestHeader Map<String, String> headers, String caseId, String username) {
        HashMap<String, String> retMap = new HashMap<>();
        retMap.put("readonly", "true");
        if (!headers.containsKey("authorization")) {
            return new Gson().toJson(retMap);
        }
        if (username.equals("bm_sys") || username.equals("origin")) {
            return new Gson().toJson(retMap);
        }
        String token = headers.get("authorization").split("Bearer ")[1];
        Map<String, String> tokenMap = tokenServ.validToken(token);
        if (tokenMap.get("status").equals("failed")) {
            return new Gson().toJson(retMap);
        }
        if (!tokenMap.get("username").equals(username)) {
            return new Gson().toJson(retMap);
        } else {
            String status = draftServ.getDraftStatus(tokenMap.get("username"), caseId);
            if (status != null) {
                retMap.put("readonly", "false");
                retMap.put("status", status);
                return new Gson().toJson(retMap);
            } else {
                return new Gson().toJson(retMap);
            }
        }
    }

    @PostMapping(value = "getMyAnnos")
    public String getMyAnnos(@RequestHeader Map<String, String> headers, String status) {
        Map<String, Object> retMap = new HashMap<>();
        String token = headers.get("authorization").split("Bearer ")[1];
        Map<String, String> tokenMap = tokenServ.validToken(token);

        if (tokenMap.get("status").equals("okay")) {
            String username = tokenMap.get("username");
            System.out.println(status);
            ArrayList<Object> allDrafts = draftServ.getDraftsForUsername(username, status);
            retMap.put("status", "okay");
            retMap.put("allDrafts", allDrafts);
            return new Gson().toJson(retMap);
        } else {
            retMap.put("status", "failed");
            return new Gson().toJson(retMap);
        }
    }

    @PostMapping("getLobeInfo")
    public String getLobeInfo(String caseId){
        Map<String, Object> retMap = new HashMap<>();
        JsonArray lobes = draftServ.getLobeInfo(caseId);
        if (lobes == null)
            return null;
        List<Object> lst = lobes.toList();
        retMap.put("lobes",lst);
        return new Gson().toJson(retMap);
    }

    // 数据库的centerline查询
    @PostMapping("getCenterLine")
    public String getCenterLine(String caseId){
        Map<String, Object> retMap = new HashMap<>();
        JsonArray centerline = draftServ.getCenterLine(caseId);
        if (centerline == null)
            return null;
        List<Object> lst = centerline.toList();
        retMap.put("centerline",lst);
        return new Gson().toJson(retMap);
    }

    @PostMapping(value = "getRectsForFollowUp")
    public String getRectsForFollowUp(String earlierCaseId,String earlierUsername,String laterCaseId,String laterUsername){
        if(earlierUsername == null||laterUsername == null){
            return "";
        }
        JsonObject rects = draftServ.getRectsForFollowUp(earlierCaseId,earlierUsername, laterCaseId,laterUsername);
        if (rects == null)
            return null;
        Map<String, Object> returnMap = rects.toMap();
        return new Gson().toJson(returnMap);
    }

    // 用于前端数据校验
    @PostMapping("dataValidation")
    public String dataValidation(String caseId){

        // debug
//        System.out.println("caseId:"+caseId);

        Map<String,Object> result = new HashMap<>();
        if(!recordService.isRecordExisted(caseId)){
            result.put("status","failed");
            result.put("message","caseId not found");
        }else if(!draftServ.isPreprocessOk(caseId)){
            result.put("status","failed");
            result.put("message","Errors occur during preprocess");
        }else if(!recordService.validateMd5_dcm(caseId)){
            result.put("status","failed");
            result.put("message","Files been manipulated");
        }else {
            result.put("status","ok");
            result.put("message","success");
        }
        ArrayList<String> annoResults = draftServ.getAnnoResults(caseId);
        result.put("annoResults", annoResults);

        ArrayList<String> modelResults = draftServ.getModelResults(caseId);
        result.put("modelResults", modelResults);

        return new Gson().toJson(result);
    }

    @PostMapping("getLymph")
    public String getLymph(String caseId, String username){
        JsonArray rects = draftServ.getLymph(caseId, username);
        if (rects == null)
            return null;
        List<Object> lst = rects.toList();
        return new Gson().toJson(lst);
    }

    // 用于postman测试
    @PostMapping(value = "getDiameterTest")
    public String getDiameterTest(@RequestHeader Map<String, String> headers, String caseId) {
        System.out.println("getDiameterTestIn");
        ArrayList<HashMap<String, Object>> modelUsernames = draftServ.getDiameterTest();
        Map<String, Object> retMap = new HashMap<>();
        retMap.put("status", "okay");
        retMap.put("dataList", modelUsernames);
        return new Gson().toJson(retMap);
    }

    // 用于postman测试
    @PostMapping(value = "postTest2")
    public String postTest2(@RequestHeader Map<String, String> headers, String caseId) {
        System.out.println("postTest2");
        ArrayList<JsonObject> modelUsernames = draftServ.postTest2();
        Map<String, Object> retMap = new HashMap<>();
        retMap.put("status", "okay");
        retMap.put("dataList", modelUsernames);
        return new Gson().toJson(retMap);
    }

}
