package me.sihang.backend.controller;

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.google.gson.Gson;
import me.sihang.backend.service.RecordService;
import me.sihang.backend.service.SubsetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@RestController
@RequestMapping("/record")
@CrossOrigin
public class RecordController {

    private final RecordService recordServ;
    private final SubsetService subsetService;

    @Autowired
    public RecordController(RecordService recordServ,SubsetService subsetService) {
        this.recordServ = recordServ;
        this.subsetService = subsetService;
    }

//    @PostMapping(value = "getMainListForPage")
//    public String getMainListForPage(String page, String type, String pidKeyword, String dateKeyword) {
//        Map<String, Object> retMap = new HashMap<>();
//
//        if (pidKeyword.length() == 0 && dateKeyword.length() == 0 && !page.equals("all")){
//
//            if (type.equals("pid")){
//                ArrayList<Map<String, Object>> mainList = new ArrayList<>();
//                mainList = recordServ.getMainListAtPageByPid(page);
//                retMap.put("mainList", mainList);
//            } else if (type.equals("date")) {
//                ArrayList<String> mainList = new ArrayList<>();
//                mainList = recordServ.getMainListAtPageByDate(page, pidKeyword, dateKeyword);
////                mainList = recordServ.getMainListAtPageByDate(page);
//                retMap.put("mainList", mainList);
//            }else {
//                retMap.put("mainList", "");
//            }
//
//        }
//        else if (pidKeyword.length() == 0 && dateKeyword.length() == 0 && page.equals("all")){
//            ArrayList<Map<String, Object>> mainList = new ArrayList<>();
//            mainList = recordServ.getAllList();
//            retMap.put("mainList", mainList);
//        }
//        else{
//            if (type.equals("pid")){
//                ArrayList<Map<String, Object>> mainList = new ArrayList<>();
//                mainList = recordServ.getMainListAtPageByPid(page, pidKeyword, dateKeyword);
//                retMap.put("mainList", mainList);
//            } else if (type.equals("date")) {
//                ArrayList<String> mainList = new ArrayList<>();
//                mainList = recordServ.getMainListAtPageByDate(page, pidKeyword, dateKeyword);
//                retMap.put("mainList", mainList);
//            }else {
//                retMap.put("mainList", "");
//            }
//        }
//
//        retMap.put("status", "okay");
//
//        return new Gson().toJson(retMap);
//    }

@PostMapping(value = "getMainListForPage")
public String getMainListForPage(String page, String type, String pidKeyword, String dateKeyword) {
    Map<String, Object> retMap = new HashMap<>();
    //debug
    System.out.println("pidKeyword: "+pidKeyword);
    System.out.println("dateKeyword: "+dateKeyword);
    System.out.println("page: "+page);
    System.out.println("type: "+type);

    if (pidKeyword.length() == 0 && dateKeyword.length() == 0 && !page.equals("all")){

        if (type.equals("pid")){
            ArrayList<Map<String, Object>> mainList = new ArrayList<>();
            mainList = recordServ.getMainListAtPageByPid(page);
            retMap.put("mainList", mainList);
        } else if (type.equals("date")) {
            ArrayList<String> mainList = new ArrayList<>();
            mainList = recordServ.getMainListAtPageByDate(page, pidKeyword, dateKeyword);
//                mainList = recordServ.getMainListAtPageByDate(page);
            retMap.put("mainList", mainList);
        }else {
            retMap.put("mainList", "");
        }

    }
    else if (pidKeyword.length() == 0 && dateKeyword.length() == 0 && page.equals("all")){
        ArrayList<Map<String, Object>> mainList = new ArrayList<>();
        mainList = recordServ.getAllList();
        retMap.put("mainList", mainList);
    }
    else{
        if (type.equals("pid")){
            ArrayList<Map<String, Object>> mainList = new ArrayList<>();
            mainList = recordServ.getMainListAtPageByPid(page, pidKeyword, dateKeyword);
            retMap.put("mainList", mainList);
        } else if (type.equals("date")) {
            ArrayList<String> mainList = new ArrayList<>();
            mainList = recordServ.getMainListAtPageByDate(page, pidKeyword, dateKeyword);
            retMap.put("mainList", mainList);
        }else {
            retMap.put("mainList", "");
        }
    }

    retMap.put("status", "okay");

    return new Gson().toJson(retMap);
}


    @PostMapping(value = "getMainListForSubset")
    public String getMainListForSubset(String username, String subsetName, String page,String type, String pidKeyword, String dateKeyword){
        Map<String, Object> retMap = new HashMap<>();

        JsonArray patienIds = this.subsetService.getPatientIdsfromSubset(username,subsetName);

        if (pidKeyword.length() == 0 && dateKeyword.length() == 0 && !page.equals("all")&& type.equals("pid")){
            ArrayList<Map<String, Object>> mainList = recordServ.getMainListForSubset(patienIds.toList(), page);
            retMap.put("mainList", mainList);
        }else if (page.equals("all")){
            ArrayList<Map<String, Object>> mainList = recordServ.getAllListForSubset(patienIds.toList());
            retMap.put("mainList", mainList);
        }else{
            if (type.equals("pid")){
                ArrayList<Map<String, Object>> mainList  = recordServ.getMainListForSubsetByPid(patienIds.toList(),page, pidKeyword, dateKeyword);
                retMap.put("mainList", mainList);
            } else if (type.equals("date")) {
                ArrayList<String> mainList = recordServ.getMainListForSubsetByDate(patienIds.toList(), page, pidKeyword, dateKeyword);
                retMap.put("mainList", mainList);
            }else {
                retMap.put("mainList", "");
            }
        }

        retMap.put("status", "okay");

        return new Gson().toJson(retMap);
    }


    @PostMapping(value = "getSubListForMainItem")
    public String getSubListForMainItem(@RequestHeader Map<String, String> headers, String type, String mainItem, String otherKeyword) {
        HashMap<String, ArrayList<String>> ret = recordServ.getStudyForMainItem(type, mainItem, otherKeyword);
        Map<String, Object> retMap = new HashMap<>();
        retMap.put("status", "okay");
        retMap.put("subList", ret);
        return new Gson().toJson(retMap);
    }

    @PostMapping(value = "getSubListForMainItem_front")
    public String getSubListForMainItem_front(@RequestHeader Map<String, String> headers, String type, String mainItem, String otherKeyword) {
        TreeMap<String, ArrayList<Map<String, Object>>> ret = recordServ.getStudyForMainItem_front(type, mainItem, otherKeyword);
        Map<String, Object> retMap = new HashMap<>();
        retMap.put("status", "okay");
        retMap.put("subList", ret);
        return new Gson().toJson(retMap);
    }

    @PostMapping(value = "getPatientListForCond")
    public String getPatientListForCond(@RequestHeader Map<String, String> headers, String pidKeyword, String dateKeyword) {
        ArrayList<String> dates = recordServ.getAllRelatedDates(dateKeyword + '%');
        return new Gson().toJson(dates);
    }

//    @PostMapping(value = "getTotalPages")
//    public String getTotalPages(@RequestHeader Map<String, String> headers, String type, String pidKeyword, String dateKeyword) {
//        int total = 0;
//        if (pidKeyword.length() == 0 && dateKeyword.length() == 0)
//            total = recordServ.getRecordsCount(type);
//        else
//            total = recordServ.getRecordsCount(type, pidKeyword, dateKeyword);
//        int pages = total % 10 == 0 ? (int) total / 10 : (int) total / 10 + 1;
//        Map<String, Object> retMap = new HashMap<>();
//        retMap.put("status", "okay");
//        retMap.put("count", pages);
//        return new Gson().toJson(retMap);
//    }
    @PostMapping(value = "getTotalPages")
    public String getTotalPages(@RequestHeader Map<String, String> headers, String type, String pidKeyword, String dateKeyword) {
        int total = 0;
        if (pidKeyword.length() == 0 && dateKeyword.length() == 0)
            total = recordServ.getRecordsCount(type);
        else
            total = recordServ.getRecordsCount(type, pidKeyword, dateKeyword);
        int pages = total % 10 == 0 ? (int) total / 10 : (int) total / 10 + 1;
        Map<String, Object> retMap = new HashMap<>();
        retMap.put("status", "okay");
        retMap.put("count", pages);
        return new Gson().toJson(retMap);
    }

    @PostMapping(value = "getTotalPagesForSubset")
    public String getTotalPagesForSubset(String username, String subsetName,String type, String pidKeyword, String dateKeyword){
        int total = 0;
        JsonArray patienIds = this.subsetService.getPatientIdsfromSubset(username, subsetName);
        if (type.equals("pid")){
            if (pidKeyword.length() == 0 && dateKeyword.length() == 0){
                total = patienIds.size();
            }else{
                total = recordServ.getRecordsCount(patienIds.toList(),type,pidKeyword,dateKeyword);
            }
        }else if(type.equals("date")){
            total = recordServ.getRecordsCount(patienIds.toList(),type,pidKeyword,dateKeyword);
        }

        int pages = total % 10 == 0 ? (int) total / 10 : (int) total / 10 + 1;
        Map<String, Object> retMap = new HashMap<>();
        retMap.put("status", "okay");
        retMap.put("count", pages);
        return new Gson().toJson(retMap);
    }

    @PostMapping(value = "getPatientInfo")
    public String getPatientInfo(@RequestHeader Map<String, String> headers, String patientId) {
        return recordServ.getPatientInfo(patientId);
    }

    @GetMapping(value = "getAllInfo_APP")
    public String getAllInfo_APP() {
        return new Gson().toJson(recordServ.getAllInfo_APP());
    }

    @PostMapping(value = "getInfoForPatient_APP")
    public String getAllInfo_APP(String patientId) {
        return new Gson().toJson(recordServ.getInfoForPatient_APP(patientId));
    }

    @PostMapping(value = "getInfoForPatientByIdNumber_APP")
    public String getInfoForPatientByIdNumber_APP(String idNumber) {
        return new Gson().toJson(recordServ.getInfoForPatientByIdNumber_APP(idNumber));
    }

    @PostMapping(value = "getCaseIdForPlugin")
    public String getCaseIdForPlugin(String pid){
        ArrayList<Map<String, Object>> ret = recordServ.getCaseIdForPlugin(pid);
        Map<String, Object> retMap = new HashMap<>();
        retMap.put("status", "okay");
        retMap.put("caseIdList", ret);
        return new Gson().toJson(retMap);
    }

}
