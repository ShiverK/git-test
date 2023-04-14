package me.sihang.backend.controller;


import com.couchbase.client.java.document.json.JsonArray;
import com.google.gson.Gson;
import me.sihang.backend.service.SubsetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/subset")
@CrossOrigin
public class SubsetController {
    private static final Logger logger = LoggerFactory.getLogger(NoduleController.class);
    private final SubsetService subsetService;

    @Autowired
    public SubsetController(SubsetService subsetService){
        this.subsetService = subsetService;
    }

    @PostMapping("create")
    public String creatSubset(String username, String patientIds, String subsetName){
        Map<String,String> res = new HashMap<>();

        if(subsetService.isSubSetExisted(subsetName)){
            res.put("status","existed");
            return new Gson().toJson(res);
        }

        if(patientIds.isEmpty()){
            res.put("status","no patient");
            return new Gson().toJson(res);
        }

        boolean isCreated = this.subsetService.createSubset(username,patientIds,subsetName);

        if(isCreated){
            res.put("status","ok");
        }else{
            res.put("status","failed");
        }

        return new Gson().toJson(res);
    }

    @PostMapping("update")
    public String creatSubset(String username, String patientIds, String subsetName, String newSubsetName){
        Map<String,String> res = new HashMap<>();

        boolean isUpdated = this.subsetService.updateSubset(username,patientIds,subsetName,newSubsetName);

        if(isUpdated){
            res.put("status","ok");
        }else{
            res.put("status","failed");
        }

        return new Gson().toJson(res);
    }

    @PostMapping("get")
    public String getSubset(String username){
        List<String> res = this.subsetService.getSubsetforUser(username);

        return new Gson().toJson(res);
    }

    @PostMapping("delete")
    public String deleteSubset(String username, String subsetName){

        Map<String,String> res = new HashMap<>();

        boolean isCreated = this.subsetService.deleteSubset(username,subsetName);

        if(isCreated){
            res.put("status","ok");
        }else{
            res.put("status","failed");
        }

        return new Gson().toJson(res);
    }

    @PostMapping("getPatienIds")
    public String getPatientIdsfromSubset(String username, String subsetName){

        JsonArray res = this.subsetService.getPatientIdsfromSubset(username,subsetName);

        return new Gson().toJson(res.toList());
    }
}
