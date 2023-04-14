package me.sihang.backend.controller;

import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.google.gson.Gson;
import me.sihang.backend.service.NoduleService;
import me.sihang.backend.service.SubsetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/nodule")
@CrossOrigin
public class NoduleController {

    private static final Logger logger = LoggerFactory.getLogger(NoduleController.class);
    private final NoduleService noduleService;
    private final SubsetService subsetService;
    private final int NUM_OF_NODULES_FOR_EACH_PAGE = 10;

    @Autowired
    public NoduleController(NoduleService noduleService,SubsetService subsetService) {
        this.noduleService = noduleService;
        this.subsetService = subsetService;
    }

    @PostMapping(value = "filterNodules")
    public String filterNodules(int malignancy, int calcification, int spiculation,
                                int lobulation, int texture, int pin , int cav, int vss, int bea,int bro, String volumeStart, String volumeEnd,
                                String diameterStart, String diameterEnd) {
        HashMap<String, Integer> result = noduleService.filterNodules(malignancy, calcification, spiculation,
        lobulation, texture, pin, cav, vss, bea, bro,volumeStart, volumeEnd, diameterStart, diameterEnd);
        int pages = result.get("nodules") % NUM_OF_NODULES_FOR_EACH_PAGE == 0 ?
                result.get("nodules") / NUM_OF_NODULES_FOR_EACH_PAGE : (int) result.get("nodules") / NUM_OF_NODULES_FOR_EACH_PAGE + 1;
        result.put("pages", pages);
        return new Gson().toJson(result);
    }

//    @PostMapping(value = "filterNodulesMulti")
//    public String filterNodulesMulti(int malignancy, int calcification, int spiculation,
//                                int lobulation, int texture, int pin , int cav, int vss, int bea,int bro,String volumeStart, String volumeEnd,
//                                String diameters) {
//
//        List<List<String>> diameterArray = new ArrayList<>();
//        String[] split_1 = diameters.split("@");
//
//        for(String str : split_1){
//            List<String> res1 = new ArrayList<>();
//            res1.add(str.split("_")[0]);
//            res1.add(str.split("_")[1]);
//            diameterArray.add(res1);
//        }
//
//        //debug
//        System.out.println("filterNodulesMulti_split_1:"+split_1);
//        System.out.println("filterNodulesMulti_res1:"+diameterArray);
//        System.out.println("params:"+malignancy+","+calcification+","+spiculation+","+lobulation+","+texture+","+pin+","+cav+","+vss+","+bea+","+bro+","+volumeStart+","+volumeEnd+","+diameters);
//
//        HashMap<String, Integer> result = noduleService.filterNodules_multi(malignancy, calcification, spiculation,
//                lobulation, texture, pin, cav, vss, bea, bro,volumeStart, volumeEnd, diameterArray,null);
//        int pages = result.get("nodules") % NUM_OF_NODULES_FOR_EACH_PAGE == 0 ?
//                result.get("nodules") / NUM_OF_NODULES_FOR_EACH_PAGE : (int) result.get("nodules") / NUM_OF_NODULES_FOR_EACH_PAGE + 1;
//        result.put("pages", pages);
//
//        //debug
//        System.out.println("filterNodulesMulti_pages:"+pages);
//        return new Gson().toJson(result);
//    }

    @PostMapping(value = "filterNodulesMulti")
    public String filterNodulesMulti(int malignancy, int calcification, int spiculation,
                                     int lobulation, int texture, int pin , int cav, int vss, int bea,int bro,String volumeStart, String volumeEnd,
                                     String diameters) {

        List<List<String>> diametersArray = new ArrayList<>();
        String[] split_1 = diameters.split("@");

        for(String str : split_1){
            List<String> res1 = new ArrayList<>();
            res1.add(str.split("_")[0]);
            res1.add(str.split("_")[1]);
            diametersArray.add(res1);
        }

//        //debug
//        System.out.println("filterNodulesMulti_res1:"+diametersArray);
//        System.out.println("params:"+malignancy+","+calcification+","+spiculation+","+lobulation+","+texture+","+pin+","+cav+","+vss+","+bea+","+bro+","+volumeStart+","+volumeEnd+","+diametersArray);

        HashMap<String, Integer> result = noduleService.filterNodules_multi(malignancy, calcification, spiculation,
                lobulation, texture, pin, cav, vss, bea, bro,volumeStart, volumeEnd, diametersArray,null);
        int pages = result.get("nodules") % NUM_OF_NODULES_FOR_EACH_PAGE == 0 ?
                result.get("nodules") / NUM_OF_NODULES_FOR_EACH_PAGE : (int) result.get("nodules") / NUM_OF_NODULES_FOR_EACH_PAGE + 1;
        result.put("pages", pages);

        //debug
        System.out.println("filterNodulesMulti_pages:"+pages);
        System.out.println("result:"+result);
        return new Gson().toJson(result);
    }



    @PostMapping(value = "filterNodulesMultiForSubset")
    public String filterNodulesMultiForSubset(int malignancy, int calcification, int spiculation,
                                     int lobulation, int texture, int pin , int cav, int vss, int bea,int bro,String volumeStart, String volumeEnd,
                                     String diameters, String username, String subsetName) {

        List<List<String>> diameterArray = new ArrayList<>();
        String[] split_1 = diameters.split("@");

        for(String str : split_1){
            List<String> res1 = new ArrayList<>();
            res1.add(str.split("_")[0]);
            res1.add(str.split("_")[1]);
            diameterArray.add(res1);
        }

        JsonArray patientIds = this.subsetService.getPatientIdsfromSubset(username,subsetName);
        String docKeyString = this.noduleService.getDocKeysStringForPatienIds(patientIds.toList());
        //System.out.println(docKeyString);

        HashMap<String, Integer> result = noduleService.filterNodules_multi(malignancy, calcification, spiculation,
                lobulation, texture, pin, cav, vss, bea, bro,volumeStart, volumeEnd, diameterArray,docKeyString);
        int pages = result.get("nodules") % NUM_OF_NODULES_FOR_EACH_PAGE == 0 ?
                result.get("nodules") / NUM_OF_NODULES_FOR_EACH_PAGE : (int) result.get("nodules") / NUM_OF_NODULES_FOR_EACH_PAGE + 1;
        result.put("pages", pages);
        System.out.println("result："+result);
        return new Gson().toJson(result);
    }

    @PostMapping(value = "getNodulesAtPage")
    public String getNodulesAtPage(int malignancy, int calcification, int spiculation,
                                   int lobulation, int texture,  int pin , int cav, int vss, int bea,int bro,String volumeStart, String volumeEnd,
                                   String diameterStart, String diameterEnd, String page) {
        ArrayList<HashMap<String, Object>> ary = noduleService.getNodulesAtPage(malignancy, calcification, spiculation, lobulation, texture,pin, cav, vss, bea, bro, volumeStart, volumeEnd, diameterStart, diameterEnd, page, NUM_OF_NODULES_FOR_EACH_PAGE);
        return new Gson().toJson(ary);
    }

//    @PostMapping(value = "getNodulesAtPageMulti")
//    public String getNodulesAtPageMulti(int malignancy, int calcification, int spiculation,
//                                        int lobulation, int texture, int pin , int cav, int vss, int bea,int bro,String volumeStart, String volumeEnd,
//                                        String diameters,String page) {
//
//        List<List<String>> diameterArray = new ArrayList<>();
//        String[] split_1 = diameters.split("@");
//
//        for(String str : split_1){
//            List<String> res1 = new ArrayList<>();
//            res1.add(str.split("_")[0]);
//            res1.add(str.split("_")[1]);
//            diameterArray.add(res1);
//        }
//
//        ArrayList<HashMap<String, Object>> ary = noduleService.getNodulesAtPage_multi(malignancy, calcification, spiculation, lobulation, texture, pin, cav, vss, bea, bro,volumeStart, volumeEnd,diameterArray, page, NUM_OF_NODULES_FOR_EACH_PAGE,null);
//        return new Gson().toJson(ary);
//    }


    @PostMapping(value = "getNodulesAtPageMulti")
    public String getNodulesAtPageMulti(int malignancy, int calcification, int spiculation,
                                        int lobulation, int texture, int pin , int cav, int vss, int bea,int bro,String volumeStart, String volumeEnd,
                                        String diameters,String page) {

        List<List<String>> diameterArray = new ArrayList<>();
        String[] split_1 = diameters.split("@");

        for(String str : split_1){
            List<String> res1 = new ArrayList<>();
            res1.add(str.split("_")[0]);
            res1.add(str.split("_")[1]);
            diameterArray.add(res1);
        }

//        ArrayList<HashMap<String, Object>> ary = noduleService.getNodulesAtPage_multi(malignancy, calcification, spiculation, lobulation, texture, pin, cav, vss, bea, bro,volumeStart, volumeEnd,diameterArray, page, NUM_OF_NODULES_FOR_EACH_PAGE,null);
        ArrayList<HashMap<String, Object>> ary = noduleService.getNodulesAtPage_multi2(malignancy, calcification, spiculation, lobulation, texture, pin, cav, vss, bea, bro,volumeStart, volumeEnd,diameterArray, page, NUM_OF_NODULES_FOR_EACH_PAGE,null);
        System.out.println("getNodulesAtPageMulti_ary:"+ary);
        return new Gson().toJson(ary);
    }

    @PostMapping(value = "getNodulesAtPageMultiForSubset")
    public String getNodulesAtPageMultiForSubset(int malignancy, int calcification, int spiculation,
                                        int lobulation, int texture, int pin , int cav, int vss, int bea,int bro,String volumeStart, String volumeEnd,
                                        String diameters,String page, String username, String subsetName) {

        List<List<String>> diameterArray = new ArrayList<>();
        String[] split_1 = diameters.split("@");

        for(String str : split_1){
            List<String> res1 = new ArrayList<>();
            res1.add(str.split("_")[0]);
            res1.add(str.split("_")[1]);
            diameterArray.add(res1);
        }

        JsonArray patientIds = this.subsetService.getPatientIdsfromSubset(username,subsetName);
        String docKeyString = this.noduleService.getDocKeysStringForPatienIds(patientIds.toList());

        ArrayList<HashMap<String, Object>> ary = noduleService.getNodulesAtPage_multi(malignancy, calcification, spiculation, lobulation, texture, pin, cav, vss, bea, bro,volumeStart, volumeEnd,diameterArray, page, NUM_OF_NODULES_FOR_EACH_PAGE,docKeyString);
        return new Gson().toJson(ary);
    }

    @PostMapping("noduleMatch")
    public String noduleMatch(String patientId,String firstDocumentId,String secondDocumentId){
        if(!noduleService.isNoduleExist(firstDocumentId)){
            JsonObject returnObject = JsonObject.create();
            returnObject.put("status","failed");
            returnObject.put("errorCode","Match-0003");
            returnObject.put("errorMessage",String.format("Nodule [%s] does not exist.",firstDocumentId));
            return new Gson().toJson(returnObject.toMap());
        }
        if(!noduleService.isNoduleExist(secondDocumentId)){
            JsonObject returnObject = JsonObject.create();
            returnObject.put("status","failed");
            returnObject.put("errorCode","Match-0003");
            returnObject.put("errorMessage",String.format("Nodule [%s] does not exist.",secondDocumentId));
            return new Gson().toJson(returnObject.toMap());
        }
        if(!noduleService.isNoduleFromSaveCase(firstDocumentId, secondDocumentId)){
            JsonObject returnObject = JsonObject.create();
            returnObject.put("status","failed");
            returnObject.put("errorCode","Match-0004");
            returnObject.put("errorMessage","Nodules from the same caseId.");
            return new Gson().toJson(returnObject.toMap());
        }

        JsonObject result = noduleService.noduleMatch(patientId, firstDocumentId, secondDocumentId);
        return new Gson().toJson(result.toMap());
    }

    @PostMapping("deleteNoduleMatch")
    public String deleteNoduleMatch(String patientId,String noduleDocumentId){
        if(!noduleService.isNoduleExist(noduleDocumentId)){
            JsonObject returnObject = JsonObject.create();
            returnObject.put("status","failed");
            returnObject.put("errorCode","Match-0003");
            returnObject.put("errorMessage",String.format("Nodule [%s] does not exist.",noduleDocumentId));
            return new Gson().toJson(returnObject.toMap());
        }
        JsonObject result = noduleService.deleteNoduleMatch(patientId, noduleDocumentId);
        return new Gson().toJson(result.toMap());
    }

    @GetMapping(value = "totalMalDist")
    public String totalMalDist() {
        return noduleService.getMalignancyData(-1);
    }

    @GetMapping(value = "totalDiameterDist")
    public String totalDiameterDist() {
        return noduleService.getTotalDiameterData();
    }

    @GetMapping(value = "characterDiameterDist")
    public String characterDiameterDist() {
        return noduleService.getDiameterCharacterData();
    }

    @GetMapping(value = "nonSpiculationDiameterDist")
    public String nonSpiculationDiameterDist() {
        return noduleService.getNonSpiculationDiameterData();
    }

    @GetMapping(value = "nonCalcificationDiameterDist")
    public String nonCalcificationDiameterDist() {
        return noduleService.getNonCalcificationDiameterData();
    }

    @GetMapping(value = "nonLobulationDiameterDist")
    public String nonLobulationDiameterDist() {
        return noduleService.getNonLobulationDiameterData();
    }

    @GetMapping(value = "nonTextureDiameterDist")
    public String nonTextureDiameterDist() {
        return noduleService.getNonTextureDiameterData();
    }


    @GetMapping(value = "spiculationMalDist")
    public String spiculationMalDist() {
         return noduleService.getMalignancyData(0);
    }

    @GetMapping(value = "calcificationMalDist")
    public String calcificationMalDist() {
         return noduleService.getMalignancyData(1);
    }

    @GetMapping(value = "lobulationMalDist")
    public String lobulationMalDist() {
        return noduleService.getMalignancyData(2);
    }

    @GetMapping(value = "textureMalDist")
    public String textureMalDist() {
        return noduleService.getMalignancyData(3);
    }

    @GetMapping(value = "pinMalDist")
    public String pinMalDist() {
        return noduleService.getMalignancyData(4);
    }

    @GetMapping(value = "cavMalDist")
    public String cavMalDist() {
        return noduleService.getMalignancyData(5);
    }
    @GetMapping(value = "vssMalDist")
    public String vssMalDist() {
        return noduleService.getMalignancyData(6);
    }
    @GetMapping(value = "beaMalDist")
    public String beaMalDist() {
        return noduleService.getMalignancyData(7);
    }
    @GetMapping(value = "broMalDist")
    public String broMalDist() {
        return noduleService.getMalignancyData(8);
    }

    @GetMapping(value = "CBPDdiameterDist")
    public String CBPDdiameterDist() {
        return noduleService.CBPDdiameterDist();
    }
    @GetMapping(value = "TCDdiameterDist")
    public String TCDdiameterDist() {
        return noduleService.TCDdiameterDist();
    }
    @GetMapping(value = "SWdiameterDist")
    public String SWdiameterDist() {
        return noduleService.SWdiameterDist();
    }
    @GetMapping(value = "SHdiameterDist")
    public String SHdiameterDist() {
        return noduleService.SHdiameterDist();
    }
    @GetMapping(value = "TAWdiameterDist")
    public String TAWdiameterDist() {
        return noduleService.TAWdiameterDist();
    }
    @GetMapping(value = "LADdiameterDist")
    public String LADdiameterDist() {
        return noduleService.LADdiameterDist();
    }
    @GetMapping(value = "RADdiameterDist")
    public String RADdiameterDist() { return noduleService.RADdiameterDist(); }

    @GetMapping(value = "LHVvolume")
    public String LHVvolume() {
        return noduleService.LHVvolume();
    }

    @GetMapping(value = "RHVvolume")
    public String RHVvolume() { return noduleService.RHVvolume(); }

    @GetMapping(value = "TAarea")
    public String TAarea() {
        return noduleService.TAarea();
    }

}