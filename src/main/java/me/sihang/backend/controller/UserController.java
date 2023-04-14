package me.sihang.backend.controller;

import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.google.gson.Gson;
import me.sihang.backend.domain.Token;
import me.sihang.backend.service.*;
import me.sihang.backend.util.MD5;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
@CrossOrigin
public class UserController {

    private final UserService userServ;
    private final RecordService recordServ;
    private final TokenService tokenServ;
    private final RoleService roleService;
    private final AuthService authService;

    @Autowired
    public UserController(UserService userServ, RecordService recordServ, TokenService tokenServ, RoleService roleService, AuthService authService) {
        this.userServ = userServ;
        this.recordServ = recordServ;
        this.tokenServ = tokenServ;
        this.roleService = roleService;
        this.authService = authService;
    }

//    @PostMapping(value="registerAction")
//    public String register(String username, String realname, String password, String privilege) {
//        String key = MD5.parse(username + realname + password + privilege);
//        boolean result = userServ.saveUserToDB(key, username, realname, password, privilege);
//        if (result == true)
//            return "1";
//        else
//            return "0";
//
//    }


    @PostMapping(value = "loginAction")
    public String loginUser(String username, String password) {
//        Map<String, Object> retObj = new HashMap<>();
//        JsonObject validUser = userServ.isValidUser(username, password);
//        if (validUser == null) {
//            retObj.put("status", "failed");
//            return new Gson().toJson(retObj);
//        }
//        long currentTimestamp = new Date().getTime();
//        long expiredTimestamp = (long) currentTimestamp / 1000 + 3600 * 24;
//
//        String expires = String.valueOf(expiredTimestamp);
//        String retUsername = (String) validUser.get("username");
//        String retRealname = (String) validUser.get("realname");
//        String retPrivilege = (String) validUser.get("privilege");
//
//        Token tokenValue = new Token(retUsername, retPrivilege, expires, "");
//        String beforeToken = new Gson().toJson(tokenValue);
//        String token = MD5.parse(beforeToken);
//
//        boolean suc = tokenServ.saveToken(token, beforeToken);
//
//        if (suc) {
//            retObj.put("status", "okay");
//            retObj.put("token", token);
//            retObj.put("username", retUsername);
//            retObj.put("privilege", retPrivilege);
//            retObj.put("realname", retRealname);
//            int totalPatients = recordServ.getAllPatientsCount();
//            int totalRecords = recordServ.getAllRecordsCount();
//            String progress = recordServ.getModelProgress();
//            int totalPages = (totalPatients % 10 == 0) ? totalPatients / 10 : (int) totalPatients / 10 + 1;
//            retObj.put("totalPatients", totalPatients);
//            retObj.put("totalRecords", totalRecords);
//            retObj.put("allPatientsPages", totalPages);
//            retObj.put("modelProgress", progress);
//        } else {
//            retObj.put("status", "failed");
//        }
//        return new Gson().toJson(retObj);
        Map<String, Object> retObj = new HashMap<>();
        JsonObject validUser = userServ.isValidUser(username, password);
        if (validUser == null) {
            retObj.put("status", "failed");
            return new Gson().toJson(retObj);
        }
        long currentTimestamp = new Date().getTime();
        long expiredTimestamp = (long) currentTimestamp / 1000 + 3600 * 24;

        String expires = String.valueOf(expiredTimestamp);
        String retUsername = (String) validUser.get("username");
        String retRealname = (String) validUser.get("realname");
        String retPrivilege = (String) validUser.get("privilege");

        Token tokenValue = new Token(retUsername, retPrivilege, expires, "");
        String beforeToken = new Gson().toJson(tokenValue);
        String token = MD5.parse(beforeToken);

        boolean suc = tokenServ.saveToken(token, beforeToken);

        if (suc) {
            retObj.put("status", "okay");
            retObj.put("token", token);
            retObj.put("username", retUsername);
            retObj.put("privilege", retPrivilege);
            retObj.put("realname", retRealname);
            int totalPatients = recordServ.getAllPatientsCount();
            int totalRecords = recordServ.getAllRecordsCount();
            int BCRecords = recordServ.getBCRecordsCount();
            int HCRecords = totalRecords - BCRecords;
            int finishedDraft = recordServ.getModelProgress();
            DecimalFormat formatter = new DecimalFormat("0.00");
            float res = (float) finishedDraft / totalRecords * 100;
            String progress = formatter.format(res) + " %";
            retObj.put("totalPatients", totalPatients);
            retObj.put("totalRecords", totalRecords);
            retObj.put("BCRecords", BCRecords);
            retObj.put("HCRecords", HCRecords);
            retObj.put("modelProgress", progress);
        } else {
            retObj.put("status", "failed");
        }
        return new Gson().toJson(retObj);
    }

    @PostMapping(value = "loginAction_app")
    public String loginUser_app(String username, String password){
        Map<String, Object> retObj = new HashMap<>();
        JsonObject validUser = userServ.isValidUser_app(username, password);
        if (validUser == null) {
            retObj.put("status", "failed");
            return new Gson().toJson(retObj);
        }

        String userId = (String) validUser.get("userId");
        String type = (String) validUser.get("type");
        String phonenumber = (String) validUser.get("phonenumber");

        retObj.put("status", "okay");
        retObj.put("userId", userId);
        retObj.put("phonenumber", phonenumber);
        retObj.put("type", type);

        return new Gson().toJson(retObj);
    }


    @GetMapping(value = "getSessionUser")
    public String getSessionUser(@RequestHeader Map<String, String> headers) {
        String token = headers.get("authorization").split("Bearer ")[1];
        Map<String, String> map = tokenServ.validToken(token);
        return new Gson().toJson(map);
    }

    @GetMapping(value = "signoutUser")
    public String signoutUser(@RequestHeader Map<String, String> headers) {
        Map<String, String> status = new HashMap<>();
        String token = headers.get("authorization").split("Bearer ")[1];
        boolean removeTokenSuc = tokenServ.removeToken(token);
        if (removeTokenSuc) {
            status.put("status", "okay");
        } else {
            status.put("status", "failed");
        }
        return new Gson().toJson(status);
    }



    @GetMapping(value = "myDrafts")
    public String myDrafts(@RequestHeader Map<String, String> headers) {
        Map<String, String> retMap = new HashMap<>();
        retMap.put("status", "failed");
        if (!headers.containsKey("authorization")) {
            return new Gson().toJson(retMap);
        }
        String token = headers.get("authorization").split("Bearer ")[1];
        Map<String, String> tokenValid = tokenServ.validToken(token);
        if (tokenValid.get("status").equals("failed"))
            return new Gson().toJson(retMap);
        String username = tokenValid.get("username");
        ArrayList<HashMap<String, String>> ja = userServ.getMyDrafts(username);
        return new Gson().toJson(ja);
    }

    @GetMapping(value = "getStatistics")
    public String getStatistics() {

//        Map<String, Object> retObj = new HashMap<>();
//        int totalPatients = recordServ.getAllPatientsCount();
//        int totalRecords = recordServ.getAllRecordsCount();
//        String progress = recordServ.getModelProgress();
//        retObj.put("totalPatients", totalPatients);
//        retObj.put("totalRecords", totalRecords);
//        retObj.put("modelProgress", progress);
//        return new Gson().toJson(retObj);
        Map<String, Object> retObj = new HashMap<>();
        int totalPatients = recordServ.getAllPatientsCount();
        int totalRecords = recordServ.getAllRecordsCount();
        int finishedDraft = recordServ.getModelProgress();
        DecimalFormat formatter = new DecimalFormat("0.00");
        float res = (float) finishedDraft / totalRecords * 100;
        String progress = formatter.format(res) + " %";
        retObj.put("totalPatients", totalPatients);
        retObj.put("totalRecords", totalRecords);
        retObj.put("modelProgress", progress);
        return new Gson().toJson(retObj);

    }

    @PostMapping(value = "insertUserInfo")
    public String insertUserInfo(String username){
        return new Gson().toJson(this.userServ.insertUserInfo(username));
    }

    @PostMapping(value = "insertUserInfoForAdmin")
    public String insertUserInfoForAdmin(String createUsername, String createPassword, String roles){
        String[] roleLst = roles.split("#");
//        Map<String,String> res = new HashMap<>();
//        String[] roleLst = roles.split("_");
//        if(this.userServ.isAdmin(username,password)){
//            res.put("status","failed");
//            res.put("msg","Not Admin");
//        }
        return new Gson().toJson(this.userServ.insertUserInfo(createUsername,createPassword,roleLst));
    }

    @PostMapping(value = "updateUserInfoForAdmin")
    public String updateUserInfoForAdmin(String username, String newPassword, String newRoles){
        String[] roleLst = newRoles.split("#");
        return new Gson().toJson(this.userServ.updateUserInfo(username,newPassword,roleLst));
    }

    @PostMapping(value = "updateRolesForUser")
    public String updateRolesForUser( String changeUsername, String roles){
        String[] roleLst = roles.split("#");
//        Map<String,String> res = new HashMap<>();
//        String[] roleLst = roles.split("_");
//        if(this.userServ.isAdmin(username,password)){
//            res.put("status","failed");
//            res.put("msg","Not Admin");
//            return new Gson().toJson(res);
//        }

        return new Gson().toJson(this.userServ.updateRolesForUser(changeUsername,roleLst));
    }

    @PostMapping(value = "getRolesForUser")
    public String getRolesForUser( String username){

        return new Gson().toJson(this.userServ.getRolesForUser(username));
    }

    @PostMapping(value = "getAuthsForUser")
    public String getAuthsForUser( String username){
        ArrayList<String> roles =  this.userServ.getRolesForUser(username);
        String[] rolesString = roles.toArray(new String[roles.size()]) ;

        return new Gson().toJson(this.authService.getAuthsForRole(rolesString));
    }

    @GetMapping(value = "getTotalUserPages")
    public String getTotalUserPages( ){
        return new Gson().toJson(this.userServ.getTotalUserPages());
    }

    @PostMapping(value = "getUserAtpage")
    public String getUserAtpage(String page, String orderBy){
        return new Gson().toJson(this.userServ.getUserAtpage(page,orderBy));
    }

    @PostMapping("delUser")
    public String delUser(String username){return new Gson().toJson(this.userServ.delUser(username));}

    @PostMapping(value = "getRemoteAddr")
    public String getRemoteAddr(){return new Gson().toJson(this.userServ.getRemoteAddr());}

    @PostMapping("addUserUsageDuration")
    public String addUserUsageDuration(String username,String studyId,String caseId,String duration){
        Map<String, String> res = userServ.addUserUsageDuration(username, studyId, caseId, duration);
        return new Gson().toJson(res);
    }

    @PostMapping("saveCustomConfig")
    public String saveCustomConfig(String username,String filterAndSorter){
        Map<String, String> res = userServ.saveCustomConfig(username, filterAndSorter);
        return new Gson().toJson(res);
    }

    @PostMapping("getCustomConfig")
    public String getCustomConfig(String username){
        JsonObject customConfig = userServ.getCustomConfig(username);
        if(customConfig == null){
            return "";
        }
        return new Gson().toJson(customConfig.toMap());
    }


//    @GetMapping(value = "getPatientSexRatio")
//    public String getPatientSexRatio() {
//        return recordServ.getPatientSexRatio();
//    }

}
