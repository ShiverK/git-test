package me.sihang.backend.service;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;
import com.google.gson.Gson;
import me.sihang.backend.util.RedisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class NoduleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NoduleService.class);
    private Bucket bucket;

    @Autowired
    private RedisUtils ru;


    @Autowired
    public NoduleService(Bucket bucket) {
        this.bucket = bucket;
    }

    public String constructN1QL(int malignancy, int calcification, int spiculation,
                                int lobulation, int texture, int pin , int cav, int vss, int bea,int bro,
                                String volumeStart, String volumeEnd, String diameterStart, String diameterEnd, String page, int num_of_nodules_for_page) {
        String n1qlQuery = "";
        if (page == null)
            n1qlQuery = "select meta(bm_sys).id as docKey from `bm_sys` where ";
        else
            n1qlQuery = "select meta(bm_sys).id as docKey, nodule_no, malignancy, calcification, spiculation, lobulation, texture, pin, cav, vss, bea,bro,volume, diameter, nodule_hist, status from `bm_sys` where ";
        if (malignancy != -1)
            n1qlQuery += "malignancy = " + String.valueOf(malignancy) + " and ";
        if (calcification != -1)
            n1qlQuery += "calcification = " + String.valueOf(calcification) + " and ";
        if (spiculation != -1)
            n1qlQuery += "spiculation = " + String.valueOf(spiculation) + " and ";
        if (lobulation != -1)
            n1qlQuery += "lobulation = " + String.valueOf(lobulation) + " and ";
        if (texture != -1)
            n1qlQuery += "texture = " + String.valueOf(texture) + " and ";
        if (pin != -1)
            n1qlQuery += "pin = " + String.valueOf(pin) + " and ";
        if (cav != -1)
            n1qlQuery += "cav = " + String.valueOf(cav) + " and ";
        if (vss != -1)
            n1qlQuery += "vss = " + String.valueOf(vss) + " and ";
        if (bea != -1)
            n1qlQuery += "bea = " + String.valueOf(bea) + " and ";
        if (bro != -1)
            n1qlQuery += "bro = " + String.valueOf(bro) + " and ";
        if (volumeStart != null)
            n1qlQuery += "volume > " + volumeStart + " and ";
        if (volumeEnd != null)
            n1qlQuery += "volume < " + volumeEnd + " and ";
        if (diameterStart != null)
            n1qlQuery += "diameter > " + String.valueOf(Double.parseDouble(diameterStart) * 10) + " and ";
        if (diameterEnd != null)
            n1qlQuery += "diameter < " + String.valueOf(Double.parseDouble(diameterEnd) * 10) + " and ";
        n1qlQuery += "(status = 1 or status = 2) and type = 'nodule'";
        if (page != null) {
            // do something
            if(!page.equals("all")){
                int pageNum = Integer.parseInt(page) - 1;
                int offset = pageNum * num_of_nodules_for_page;
                n1qlQuery += " limit " + num_of_nodules_for_page + " offset " + offset;
            }
        }
        return n1qlQuery;
    }

    public String constructN1QL_multi(int malignancy, int calcification, int spiculation,
                                      int lobulation, int texture, int pin , int cav, int vss, int bea, int bro,String volumeStart, String volumeEnd,
                                      List<List<String>> weekArray, String page, int num_of_nodules_for_page, String docKeyStr) {
        String n1qlQuery = "";
        if (page == null)
            n1qlQuery = "select meta(bm_sys).id as docKey from `bm_sys` where ";
        else
            n1qlQuery = "select meta(bm_sys).id as docKey, nodule_no, malignancy, calcification, spiculation, lobulation, texture, pin, cav, vss, bea,bro,volume, diameter, nodule_hist, status from `bm_sys` where ";

        if (docKeyStr !=null)
            n1qlQuery += "meta(bm_sys).id in " + docKeyStr + " and ";
        if (malignancy != -1)
            n1qlQuery += "malignancy = " + String.valueOf(malignancy) + " and ";
        if (calcification != -1)
            n1qlQuery += "calcification = " + String.valueOf(calcification) + " and ";
        if (spiculation != -1)
            n1qlQuery += "spiculation = " + String.valueOf(spiculation) + " and ";
        if (lobulation != -1)
            n1qlQuery += "lobulation = " + String.valueOf(lobulation) + " and ";
        if (texture != -1)
            n1qlQuery += "texture = " + String.valueOf(texture) + " and ";
        if (pin != -1)
            n1qlQuery += "pin = " + String.valueOf(pin) + " and ";
        if (cav != -1)
            n1qlQuery += "cav = " + String.valueOf(cav) + " and ";
        if (vss != -1)
            n1qlQuery += "vss = " + String.valueOf(vss) + " and ";
        if (bea != -1)
            n1qlQuery += "bea = " + String.valueOf(bea) + " and ";
        if (bro != -1)
            n1qlQuery += "bro = " + String.valueOf(bro) + " and ";
        if (volumeStart != null)
            n1qlQuery += "volume > " + volumeStart + " and ";
        if (volumeEnd != null)
            n1qlQuery += "volume < " + volumeEnd + " and ";
        if (weekArray != null)

            n1qlQuery += " ( ";

        for(int i=0; i<weekArray.size(); i++){

            List<String> diameters = (List<String>) weekArray.get(i);

            String diameterStart = String.valueOf(Double.parseDouble(diameters.get(0)) * 10);
            String diameterEnd = String.valueOf(Double.parseDouble(diameters.get(1)) * 10);

            n1qlQuery += " ( diameter > " + diameterStart + " and diameter < " + diameterEnd + ") " ;
            if( i < weekArray.size() -1 ){
                n1qlQuery += " or ";
            }
        }

        n1qlQuery += " ) and  ";


        n1qlQuery += "(status = 1 or status = 2) and type = 'nodule'";
        if (page != null) {
            // do something
            if(!page.equals("all")){
                int pageNum = Integer.parseInt(page) - 1;
                int offset = pageNum * num_of_nodules_for_page;
                n1qlQuery += " limit " + num_of_nodules_for_page + " offset " + offset;
            }

        }
        System.out.println("n1qlQuery:"+n1qlQuery);
        return n1qlQuery;
    }


    public String getDocKeysStringForPatienIds(List<Object> patientIds){

        List<String> res = new ArrayList<>();
        for(Object patientId : patientIds){
            N1qlQueryResult result = bucket.query(
                    N1qlQuery.parameterized("select rects from `bm_sys` where type = 'draft' and caseId like $1", JsonArray.from(patientId.toString() + "%"))
            );
            for(N1qlQueryRow row : result){
                JsonArray rects = row.value().getArray("rects");
                for(Object docKey : rects){
                    res.add(docKey.toString());
                }
            }
        }

        ArrayList<String> allDocKeysList = new ArrayList<>();
        for(Object dockey: res){
            allDocKeysList.add("'" + dockey + "'");
        }
        String docKeyStr = "[" + String.join(",", allDocKeysList) + "]";
        return docKeyStr;
    }

    public HashMap<String, Integer> filterNodules(int malignancy, int calcification, int spiculation,
                                                  int lobulation, int texture, int pin, int cav, int vss, int bea, int bro, String volumeStart, String volumeEnd,
                                                  String diameterStart, String diameterEnd) {

        String n1qlQuery = this.constructN1QL(malignancy, calcification, spiculation, lobulation, texture, pin, cav, vss, bea,bro, volumeStart, volumeEnd,
                diameterStart, diameterEnd, null, 0);

        String countN1QLQuery = n1qlQuery.replace("meta(bm_sys).id", "count(1)");
        System.out.println(countN1QLQuery);

        N1qlQueryResult result = bucket.query(
                N1qlQuery.simple(countN1QLQuery)
        );
//        int noduleCount = result.allRows().size();
//        HashSet<String> caseIds = new HashSet<>();
//        HashSet<String> patientIds = new HashSet<>();
//        for (N1qlQueryRow row : result) {
//            String docKey = (String) row.value().get("docKey");
//            String caseId = docKey.split("#")[1];
//            String patientId = caseId.split("_")[0];
//            caseIds.add(caseId);
//            patientIds.add(patientId);
//        }
//
//        HashMap<String, Integer> retMap = new HashMap<>();
//        retMap.put("nodules", noduleCount);
//        retMap.put("caseIds", caseIds.size());
//        retMap.put("patientIds", patientIds.size());
        int noduleCount = (int) result.allRows().get(0).value().get("docKey");

        HashMap<String, Integer> retMap = new HashMap<>();
        retMap.put("nodules", noduleCount);


        return retMap;

    }

    public HashMap<String, Integer> filterNodules_multi(int malignancy, int calcification, int spiculation,
                                                        int lobulation, int texture, int pin, int cav, int vss, int bea, int bro,String volumeStart,String volumeEnd,List<List<String>> diametersArray, String docKeyStr) {
//        String n1qlQuery = this.constructN1QL_multi(malignancy, calcification, spiculation, lobulation, texture, pin, cav, vss, bea, bro, volumeStart, volumeEnd,
//                diametersArray, null, 0,docKeyStr);

//        String countN1QLQuery = n1qlQuery.replaceFirst("meta\\(bm_sys\\)\\.id", "count(1)");

        String countN1QLQuery = "select count(1) as docKey from `fetal_sys` where (status = '1' or status = '2') and type = 'draft';";
        System.out.println(countN1QLQuery);

        N1qlQueryResult result = bucket.query(
                N1qlQuery.simple(countN1QLQuery)
        );
        // debug
        System.out.println("filterNodules_multi_result:"+result);

        int noduleCount = (int) result.allRows().get(0).value().get("docKey");
        HashMap<String, Integer> retMap = new HashMap<>();
        retMap.put("nodules", noduleCount);

        return retMap;
    }

    public ArrayList<HashMap<String, Object>> getNodulesAtPage(int malignancy, int calcification, int spiculation,
                                                               int lobulation, int texture, int pin, int cav, int vss, int bea, int bro, String volumeStart, String volumeEnd,
                                                               String diameterStart, String diameterEnd, String page, int num_of_pages_for_page) {
        String n1qlQuery = this.constructN1QL(malignancy, calcification, spiculation, lobulation, texture, pin, cav, vss, bea, bro, volumeStart, volumeEnd,
                diameterStart, diameterEnd, page, num_of_pages_for_page);
        N1qlQueryResult result = bucket.query(N1qlQuery.simple(n1qlQuery));
        ArrayList<HashMap<String, Object>> ary = new ArrayList<>();
        for (N1qlQueryRow row : result) {
            JsonObject value = (JsonObject) row.value();
            HashMap<String, Object> tmp = new HashMap<>();
            // malignancy, calcification, spiculation, lobulation, texture, volume, diamter, status
            String docKey = value.getString("docKey");
            int caseIdStartIndex = docKey.indexOf("#") + 1;
            int caseIdEndIndex = docKey.lastIndexOf("#");
            String caseId = docKey.substring(caseIdStartIndex, caseIdEndIndex);
            String username = value.getString("docKey").split("#")[0];

            tmp.put("malignancy", value.getInt("malignancy"));
            tmp.put("calcification", value.getInt("calcification"));
            tmp.put("spiculation", value.getInt("spiculation"));
            tmp.put("lobulation", value.getInt("lobulation"));
            tmp.put("texture", value.getInt("texture"));
            tmp.put("volume", value.getDouble("volume"));
            tmp.put("diameter", value.getDouble("diameter") / 10);
            tmp.put("status", value.getInt("status"));
            tmp.put("noduleNo", value.getString("nodule_no"));
            if(value.containsKey("diameter")){
                tmp.put("diameter", value.getDouble("diameter") / 10);
            }
            if(value.containsKey("nodule_hist")){
                tmp.put("nodule_hist", value.get("nodule_hist"));
            }
            if(value.containsKey("pin")){
                tmp.put("pin", value.getInt("pin"));
            }
            if(value.containsKey("cav")){
                tmp.put("cav", value.getInt("cav"));
            }
            if(value.containsKey("vss")){
                tmp.put("vss", value.getInt("vss"));
            }
            if(value.containsKey("bea")){
                tmp.put("bea", value.getInt("bea"));
            }
            if(value.containsKey("bro")){
                tmp.put("bro", value.getInt("bro"));
            }
            tmp.put("caseId", caseId);

            tmp.put("username", username);

            JsonDocument recordDocument = bucket.get(caseId + "@record");
            String patientId = recordDocument.content().getString("patientId");
            tmp.put("patienId", patientId);

            N1qlQueryResult result2 = bucket.query(
                    N1qlQuery.parameterized("select * from `bm_sys` where type = 'info' and patientId = $1", JsonArray.from(patientId))
            );
            if(result2.allRows().size()>0){
                JsonObject content = result2.allRows().get(0).value().getObject("bm_sys");
                tmp.put("patientSex",content.getString("patientSex"));
                tmp.put("patientBirth",content.getString("patientBirth"));
            }


            ary.add(tmp);
        }
        return ary;
    }

    public ArrayList<HashMap<String, Object>> getNodulesAtPage_multi(int malignancy, int calcification, int spiculation,
                                                                     int lobulation, int texture, int pin, int cav, int vss, int bea, int bro, String volumeStart, String volumeEnd,
                                                                     List<List<String>> diameterArray, String page, int num_of_pages_for_page, String docKetStr) {
        String n1qlQuery = this.constructN1QL_multi(malignancy, calcification, spiculation, lobulation, texture, pin, cav, vss, bea, bro, volumeStart, volumeEnd,
                diameterArray, page, num_of_pages_for_page,docKetStr);
        N1qlQueryResult result = bucket.query(N1qlQuery.simple(n1qlQuery));
        ArrayList<HashMap<String, Object>> ary = new ArrayList<>();
        for (N1qlQueryRow row : result) {
            JsonObject value = (JsonObject) row.value();
            HashMap<String, Object> tmp = new HashMap<>();
            // malignancy, calcification, spiculation, lobulation, texture, volume, diamter, status
            String docKey = value.getString("docKey");
            int caseIdStartIndex = docKey.indexOf("#") + 1;
            int caseIdEndIndex = docKey.lastIndexOf("#");
            String caseId = docKey.substring(caseIdStartIndex, caseIdEndIndex);
            String username = value.getString("docKey").split("#")[0];

            tmp.put("malignancy", value.getInt("malignancy"));
            tmp.put("calcification", value.getInt("calcification"));
            tmp.put("spiculation", value.getInt("spiculation"));
            tmp.put("lobulation", value.getInt("lobulation"));
            tmp.put("texture", value.getInt("texture"));
            tmp.put("volume", value.getDouble("volume"));
            tmp.put("status", value.getInt("status"));
            tmp.put("noduleNo", value.getString("nodule_no"));
            if(value.containsKey("diameter")){
                tmp.put("diameter", value.getDouble("diameter") / 10);
            }
            if(value.containsKey("nodule_hist")){
                tmp.put("nodule_hist", value.get("nodule_hist"));
            }
            if(value.containsKey("pin")){
                tmp.put("pin", value.getInt("pin"));
            }
            if(value.containsKey("cav")){
                tmp.put("cav", value.getInt("cav"));
            }
            if(value.containsKey("vss")){
                tmp.put("vss", value.getInt("vss"));
            }
            if(value.containsKey("bea")){
                tmp.put("bea", value.getInt("bea"));
            }
            if(value.containsKey("bro")){
                tmp.put("bro", value.getInt("bro"));
            }
            tmp.put("caseId", caseId);

            tmp.put("username", username);
            System.out.println("caseid = "+caseId);
            JsonDocument recordDocument = bucket.get(caseId + "@record");
            String patientId = recordDocument.content().getString("patientId");
            tmp.put("patienId", patientId);

            N1qlQueryResult result2 = bucket.query(
                    N1qlQuery.parameterized("select * from `bm_sys` where type = 'info' and patientId = $1", JsonArray.from(patientId))
            );
            if(result2.allRows().size()>0){
                JsonObject content = result2.allRows().get(0).value().getObject("bm_sys");
                tmp.put("patientSex",content.getString("patientSex"));
                tmp.put("patientBirth",content.getString("patientBirth"));
            }

            ary.add(tmp);
        }
        return ary;
    }

    // NEW
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

    // NEW
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

    // 左半脑体积(cm³)    右半脑体积(cm³)    小脑最大横截面面积(cm)    脑双顶径    小脑最大横径    左侧脑室宽度    右侧脑室宽度    第三脑室宽度    透明隔腔高度    透明隔腔宽度
    public ArrayList<HashMap<String, Object>> getNodulesAtPage_multi2(int malignancy, int calcification, int spiculation, int lobulation, int texture, int pin, int cav, int vss, int bea, int bro, String volumeStart, String volumeEnd, List<List<String>> diameterArray, String page, int num_of_nodules_for_page, String docKetStr) {
        ArrayList<JsonArray> ret = new ArrayList<>();
        String n1qlQuery = "select meta(fetal_sys).id as docKey, diameter, volume, area from fetal_sys where type = 'draft' and (status = '1' or status = '2')";
        if (page != null) {
            // do something
            if (!page.equals("all")) {
                int pageNum = Integer.parseInt(page) - 1;
                int offset = pageNum * num_of_nodules_for_page;
                n1qlQuery += " limit " + num_of_nodules_for_page + " offset " + offset + ";";
            }
        }else {
            n1qlQuery += ";";
        }

        N1qlQueryResult result = bucket.query(N1qlQuery.simple(n1qlQuery));
        ArrayList<HashMap<String, Object>> ary = new ArrayList<>();

        for (N1qlQueryRow row: result) {
            HashMap<String, Object> tmp = new HashMap<>();
            JsonArray diameter = row.value().getArray("diameter");
            JsonArray volume = row.value().getArray("volume");
            JsonArray area = row.value().getArray("area");
            for(int i = 0; i < diameter.size(); i++){
                JsonObject rowJSON = diameter.getObject(i);

                if (Objects.equals(rowJSON.getString("name"), "CBPD")){
//                    double CBPD = rowJSON.getDouble("value");
                    tmp.put("CBPD", rowJSON.getDouble("value").floatValue());
                }else if (Objects.equals(rowJSON.getString("name"), "TCD")){
                    tmp.put("TCD", rowJSON.getDouble("value").floatValue());
                }else if (Objects.equals(rowJSON.getString("name"), "SW")){
                    tmp.put("SW", rowJSON.getDouble("value").floatValue());
                }else if (Objects.equals(rowJSON.getString("name"), "SW")) {
                    tmp.put("SW", rowJSON.getDouble("value").floatValue());
                }else if (Objects.equals(rowJSON.getString("name"), "SH")) {
                    tmp.put("SH", rowJSON.getDouble("value").floatValue());
                }else if (Objects.equals(rowJSON.getString("name"), "TAW")) {
                    tmp.put("TAW", rowJSON.getDouble("value").floatValue());
                }else if (Objects.equals(rowJSON.getString("name"), "LAD")) {
                    tmp.put("LAD", rowJSON.getDouble("value").floatValue());
                }else if (Objects.equals(rowJSON.getString("name"), "RAD")) {
                    tmp.put("RAD", rowJSON.getDouble("value").floatValue());
                }
            }
            for(int i = 0; i < volume.size(); i++){
                JsonObject rowJSON = volume.getObject(i);
                if (Objects.equals(rowJSON.getString("name"), "LHV")){
                    tmp.put("LHV", rowJSON.getDouble("value").floatValue());
                }else if (Objects.equals(rowJSON.getString("name"), "RHV")){
                    tmp.put("RHV", rowJSON.getDouble("value").floatValue());
                }
            }

            for(int i = 0; i < area.size(); i++){
                JsonObject rowJSON = area.getObject(i);
                if (Objects.equals(rowJSON.getString("name"), "TA")){
                    tmp.put("TA", rowJSON.getDouble("value").floatValue());
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
//                System.out.println(patienInfo);
                String patientId = patienInfo.getString("patientId");
                tmp.put("patientId", patientId);
                int horb = patienInfo.getInt("horb");
                tmp.put("horb", horb);
                String patientBirth = getBirthByPatientId(patientId);
                tmp.put("patientBirth", patientBirth);
            }else {
                tmp.put("patientId", null);
                tmp.put("horb", null);
                tmp.put("patientBirth", null);
            }
            ary.add(tmp);
            System.out.println("tmp:"+tmp);
        }
        System.out.println("ary:"+ary);
        return ary;
    }

    public String getTotalDiameterDataBackup() {
        String zero2three = "select count(*) as cnt from `bm_sys` where type = 'nodule' and (status = 1 or status = 2) and diameter between 0 and 3;";
        String three2five = "select count(*) as cnt from `bm_sys` where type = 'nodule' and (status = 1 or status = 2) and diameter between 3 and 5;";
        String five2eight = "select count(*) as cnt from `bm_sys` where type = 'nodule' and (status = 1 or status = 2) and diameter between 5 and 8;";
        String eight2thirty = "select count(*) as cnt from `bm_sys` where type = 'nodule' and (status = 1 or status = 2) and diameter between 8 and 30;";
        String aboveThirty = "select count(*) as cnt from `bm_sys` where type = 'nodule' and (status = 1 or status = 2) and diameter > 30;";
        ArrayList<HashMap<String, Object>> ary = new ArrayList<>();
        HashMap<String, Object> retMap = new HashMap<>();

        try {
            int value1 = bucket.query(N1qlQuery.simple(zero2three)).allRows().get(0).value().getInt("cnt");
            int value2 = bucket.query(N1qlQuery.simple(three2five)).allRows().get(0).value().getInt("cnt");
            int value3 = bucket.query(N1qlQuery.simple(five2eight)).allRows().get(0).value().getInt("cnt");
            int value4 = bucket.query(N1qlQuery.simple(eight2thirty)).allRows().get(0).value().getInt("cnt");
            int value5 = bucket.query(N1qlQuery.simple(aboveThirty)).allRows().get(0).value().getInt("cnt");
            retMap.put("status", "okay");
            HashMap<String, Object> first = new HashMap<>();
            HashMap<String, Object> second = new HashMap<>();
            HashMap<String, Object> third = new HashMap<>();
            HashMap<String, Object> fourth = new HashMap<>();
            HashMap<String, Object> fifth = new HashMap<>();

            first.put("type", "0-3 mm");
            first.put("value", value1);

            second.put("type", "3-5 mm");
            second.put("value", value2);

            third.put("type", "5-8 mm");
            third.put("value", value3);

            fourth.put("type", "8-30 mm");
            fourth.put("value", value4);

            fifth.put("type", ">30 mm");
            fifth.put("value", value5);

            ary.add(first);
            ary.add(second);
            ary.add(third);
            ary.add(fourth);
            ary.add(fifth);

            retMap.put("data", ary);
            String retValue = new Gson().toJson(retMap);
            return retValue;
        } catch(Exception e) {
            retMap.put("status", "failed");
            retMap.put("data", ary);
            return new Gson().toJson(retMap);
        }
    }


    public String getIdxToTypename(int idx) {
        switch (idx) {
            case 1: return "0-3 mm";
            case 2: return "3-5 mm";
            case 3: return "5-8 mm";
            case 4: return "8-30 mm";
            case 5: return "30+ mm";
            default: return "";
        }
    }


    public String getDiameterCharacterData() {
        HashMap<String, Object> retMap = new HashMap<>();
        ArrayList<HashMap<String, Object>> ary = new ArrayList<>();
        try {
            JsonObject jo = bucket.get("character_diameter_dist").content();
            JsonArray ja = (JsonArray) jo.get("value");
            for (Object obj : ja) {
                JsonObject jsonObj = (JsonObject) obj;
                String diameter = (String) jsonObj.get("diameter");
                String type = (String) jsonObj.get("type");
                int value = (int) jsonObj.get("value");
                HashMap<String, Object> tmp = new HashMap<>();
                tmp.put("diameter", diameter);
                tmp.put("type", type);
                tmp.put("value", value);
                ary.add(tmp);
            }

            retMap.put("status", "okay");
            retMap.put("data", ary);
            return new Gson().toJson(retMap);
        } catch (Exception e) {
            e.printStackTrace();
            retMap.put("status", "failed");
            retMap.put("data", ary);
            return new Gson().toJson(retMap);
        }
//        return null;
    }

    public String getNonSpiculationDiameterData() {
        HashMap<String, Object> retMap = new HashMap<>();
        ArrayList<HashMap<String, Object>> ary = new ArrayList<>();
        try {
            JsonObject jo = bucket.get("non_spiculation_diameter_dist").content();
            JsonArray ja = (JsonArray) jo.get("value");
            for (Object obj : ja) {
                JsonObject jsonObj = (JsonObject) obj;
                String type = (String) jsonObj.get("type");
                int value = (int) jsonObj.get("value");
                HashMap<String, Object> tmp = new HashMap<>();
                tmp.put("type", type);
                tmp.put("value", value);
                ary.add(tmp);
            }

            retMap.put("status", "okay");
            retMap.put("data", ary);
            return new Gson().toJson(retMap);
        } catch (Exception e) {
            retMap.put("status", "failed");
            retMap.put("data", ary);
            return new Gson().toJson(retMap);
        }
    }

    public String getNonCalcificationDiameterData() {

        HashMap<String, Object> retMap = new HashMap<>();
        ArrayList<HashMap<String, Object>> ary = new ArrayList<>();
        try {
            JsonObject jo = bucket.get("non_calcification_diameter_dist").content();
            JsonArray ja = (JsonArray) jo.get("value");
            for (Object obj : ja) {
                JsonObject jsonObj = (JsonObject) obj;
                String type = (String) jsonObj.get("type");
                int value = (int) jsonObj.get("value");
                HashMap<String, Object> tmp = new HashMap<>();
                tmp.put("type", type);
                tmp.put("value", value);
                ary.add(tmp);
            }

            retMap.put("status", "okay");
            retMap.put("data", ary);
            return new Gson().toJson(retMap);
        } catch (Exception e) {
            retMap.put("status", "failed");
            retMap.put("data", ary);
            return new Gson().toJson(retMap);
        }

    }

    public String getNonLobulationDiameterData() {
        HashMap<String, Object> retMap = new HashMap<>();
        ArrayList<HashMap<String, Object>> ary = new ArrayList<>();
        try {
            JsonObject jo = bucket.get("non_lobulation_diameter_dist").content();

            JsonArray ja = (JsonArray) jo.get("value");
            for (Object obj : ja) {
                JsonObject jsonObj = (JsonObject) obj;
                String type = (String) jsonObj.get("type");
                int value = (int) jsonObj.get("value");
                HashMap<String, Object> tmp = new HashMap<>();
                tmp.put("type", type);
                tmp.put("value", value);
                ary.add(tmp);
            }

            retMap.put("status", "okay");
            retMap.put("data", ary);
            return new Gson().toJson(retMap);
        } catch (Exception e) {
            retMap.put("status", "failed");
            retMap.put("data", ary);
            return new Gson().toJson(retMap);
        }
    }

    public String getNonTextureDiameterData() {
        HashMap<String, Object> retMap = new HashMap<>();
        ArrayList<HashMap<String, Object>> ary = new ArrayList<>();
        try {
            JsonObject jo = bucket.get("non_texture_diameter_dist").content();
            JsonArray ja = (JsonArray) jo.get("value");
            for (Object obj : ja) {
                JsonObject jsonObj = (JsonObject) obj;
                String type = (String) jsonObj.get("type");
                int value = (int) jsonObj.get("value");
                HashMap<String, Object> tmp = new HashMap<>();
                tmp.put("type", type);
                tmp.put("value", value);
                ary.add(tmp);
            }

            retMap.put("status", "okay");
            retMap.put("data", ary);
            return new Gson().toJson(retMap);
        } catch (Exception e) {
            retMap.put("status", "failed");
            retMap.put("data", ary);
            return new Gson().toJson(retMap);
        }
    }

    public String getTotalDiameterData() {
        HashMap<String, Object> retMap = new HashMap<>();
        ArrayList<HashMap<String, Object>> ary = new ArrayList<>();
        try {
            JsonObject jo = bucket.get("total_diameter_dist").content();

            JsonArray ja = (JsonArray) jo.get("value");
            for (Object obj : ja) {
                JsonObject jsonObj = (JsonObject) obj;
                String type = (String) jsonObj.get("type");
                int value = (int) jsonObj.get("value");
                HashMap<String, Object> tmp = new HashMap<>();
                tmp.put("type", type);
                tmp.put("value", value);
                ary.add(tmp);
            }

            retMap.put("status", "okay");
            retMap.put("data", ary);
            return new Gson().toJson(retMap);
        } catch (Exception e) {
            retMap.put("status", "failed");
            retMap.put("data", ary);
            return new Gson().toJson(retMap);
        }
    }

    public String getMalignancyData(int val) {

        HashMap<String, Object> retMap = new HashMap<>();
        ArrayList<HashMap<String, Object>> ary = new ArrayList<>();
        try {
            JsonObject jo;
            if (val == -1)
                jo = bucket.get("total_mal_dist").content();
            else if (val == 0)
                jo = bucket.get("spiculation_mal_dist").content();
            else if (val == 1)
                jo = bucket.get("calcification_mal_dist").content();
            else if (val == 2)
                jo = bucket.get("lobulation_mal_dist").content();
            else if (val == 3)
                jo = bucket.get("texture_mal_dist").content();
            else if (val == 4)
                jo = bucket.get("pin_mal_dist").content();
            else if (val == 5)
                jo = bucket.get("cav_mal_dist").content();
            else if (val == 6)
                jo = bucket.get("vss_mal_dist").content();
            else if (val == 7)
                jo = bucket.get("bea_mal_dist").content();
            else if (val == 8)
                jo = bucket.get("bro_mal_dist").content();
            else
                throw new Exception();

            JsonArray ja = (JsonArray) jo.get("value");
            for (Object obj : ja) {
                JsonObject jsonObj = (JsonObject) obj;
                String type = (String) jsonObj.get("type");
                int value = (int) jsonObj.get("value");
                HashMap<String, Object> tmp = new HashMap<>();
                tmp.put("type", type);
                tmp.put("value", value);
                ary.add(tmp);
            }

            retMap.put("status", "okay");
            retMap.put("data", ary);
            return new Gson().toJson(retMap);
        } catch (Exception e) {
            retMap.put("status", "failed");
            retMap.put("data", ary);
            return new Gson().toJson(retMap);
        }

    }

    public JsonObject noduleMatch(String patientId,String firstDocumentId,String secondDocumentId){
        JsonDocument matchDoc;
        matchDoc = bucket.get(patientId + "@nodule_match");
        if (matchDoc == null){
            JsonObject object = JsonObject.create();
            object.put("type","nodule_match");
            object.put("patientId",patientId);
            object.put("match",JsonArray.create());
            String key = patientId + "@nodule_match";
            matchDoc = JsonDocument.create(key, object);
        }

        JsonArray matchArray =  matchDoc.content().getArray("match");
        JsonArray matchForFirst = null;
        JsonArray matchForSecond = null;
        for (int i = 0; i < matchArray.size(); i++) {
            JsonArray rectArray = matchArray.getArray(i);
            for (int j = 0; j < rectArray.size(); j++) {
                String rectDocId = rectArray.getString(j);
                if (rectDocId.equals(firstDocumentId)){
                    matchForFirst = rectArray;
                }
                if (rectDocId.equals(secondDocumentId)){
                    matchForSecond = rectArray;
                }
            }
        }
        //如果已经和第一个caseId中的这个结节配准的结节中，已经有第二个CaseId中的某个结节，应该报错。
        if(matchForFirst != null){
            for (int i = 0; i < matchForFirst.size(); i++) {
                String rectDocId = matchForFirst.getString(i);
                String usernameAndCaseIdSecond = secondDocumentId.substring(0, secondDocumentId.length() - 9);
                if (rectDocId.startsWith(usernameAndCaseIdSecond)){
                    JsonObject returnObject = JsonObject.create();
                    returnObject.put("status","failed");
                    returnObject.put("errorCode","Match-0001");
                    returnObject.put("errorMessage",String.format("Nodule [%s] in the first caseID already matched with nodule [%s] in the second caseId,can not match with [%s].",firstDocumentId,rectDocId,secondDocumentId));
                    return returnObject;
                }
            }
        }
        //如果已经和第二个caseId中的这个结节配准的结节中，已经有第一个CaseId中的某个结节，应该报错。
        if(matchForSecond != null){
            for (int i = 0; i < matchForSecond.size(); i++) {
                String rectDocId = matchForSecond.getString(i);
                String usernameAndCaseIdFirst = firstDocumentId.substring(0, firstDocumentId.length() - 9);
                if (rectDocId.startsWith(usernameAndCaseIdFirst)){
                    JsonObject returnObject = JsonObject.create();
                    returnObject.put("status","failed");
                    returnObject.put("errorCode","Match-0002");
                    returnObject.put("errorMessage",String.format("Nodule [%s] in the second caseID already assigned with nodule [%s] in the first caseId,can not match with [%s].",secondDocumentId,rectDocId,firstDocumentId));
                    return returnObject;
                }
            }
        }
        if (matchForFirst != null && matchForSecond == null){
            matchForFirst.add(secondDocumentId);
        }
        else if (matchForSecond != null && matchForFirst == null){
            matchForSecond.add(firstDocumentId);
        }else if (matchForSecond == null && matchForFirst == null){
            JsonArray newMatch = JsonArray.create();
            newMatch.add(firstDocumentId);
            newMatch.add(secondDocumentId);
            matchArray.add(newMatch);
        }else if(matchForFirst != null && matchForSecond != null){
            for (int i = 0; i < matchForSecond.size(); i++) {
                matchForFirst.add(matchForSecond.get(i));
            }
            JsonArray newArray = JsonArray.create();
            for (int i = 0; i < matchArray.size(); i++) {
                if (!matchArray.get(i).equals(matchForSecond)){
                    newArray.add(matchArray.get(i));
                }
            }
            matchDoc.content().put("match",newArray);

        }
        try {
            bucket.upsert(matchDoc);
        } catch (Exception e) {
            JsonObject returnObject = JsonObject.create();
            returnObject.put("status","failed");
            return returnObject;
        }
        JsonObject returnObject = JsonObject.create();
        returnObject.put("status","okay");
        return returnObject;
    }

    public JsonObject deleteNoduleMatch(String patientId,String noduleDocumentId){
        JsonDocument matchDoc;
        matchDoc = bucket.get(patientId + "@nodule_match");
        JsonArray matchArray =  matchDoc.content().getArray("match");
        JsonObject returnObject = JsonObject.create();
        if (matchDoc == null){
            returnObject.put("status","okay");
            return returnObject;
        }
        int index_i = -1;
        int index_j = -1;

        for (int i = 0; i < matchArray.size() && index_i == -1; i++) {
            JsonArray rectArray = matchArray.getArray(i);
            for (int j = 0; j < rectArray.size(); j++) {
                String rectDocId = rectArray.getString(j);
                if (rectDocId.equals(noduleDocumentId)){
                    index_i = i;
                    index_j = j;
                    break;
                }
            }
        }
        if (index_i != -1 && index_j != -1){
            JsonArray newRectArray = JsonArray.create();
            for (int j = 0; j < matchArray.getArray(index_i).size(); j++) {
                if (j != index_j){
                    newRectArray.add(matchArray.getArray(index_i).get(j));
                }
            }

            JsonArray newMatchArray = JsonArray.create();
            for (int i = 0; i < matchArray.size(); i++) {
                if(i != index_i){
                    newMatchArray.add(matchArray.get(i));
                }else if (i == index_i && newRectArray.size() > 1){
                    newMatchArray.add(newRectArray);
                }
            }
            matchDoc.content().put("match",newMatchArray);
        }
        try {
            if(matchDoc.content().getArray("match").size() > 0){
                bucket.upsert(matchDoc);
            }else {
                bucket.remove(matchDoc);
            }

        } catch (Exception e) {
            returnObject.put("status","failed");
            return returnObject;
        }
        returnObject.put("status","okay");
        return returnObject;
    }

    public boolean isNoduleExist(String noduleDocumentId){
        try{
            JsonDocument noduleDocument = bucket.get(noduleDocumentId);
            if(noduleDocument==null){
                return false;
            }else {
                return true;
            }
        }catch (Exception e){
            return false;
        }
    }

    public  boolean isNoduleFromSaveCase(String firstDocumentId,String secondDocumentId){
        N1qlQueryResult firstResult = bucket.query(
                N1qlQuery.parameterized("select caseId from bm_sys where type = 'draft' and any rect in bm_sys.rects satisfies rect = $1 end", JsonArray.from(firstDocumentId))
        );
        N1qlQueryResult secondResult = bucket.query(
                N1qlQuery.parameterized("select caseId from bm_sys where type = 'draft' and any rect in bm_sys.rects satisfies rect = $1 end", JsonArray.from(secondDocumentId))
        );
        try {
            String firstCaseId = firstResult.allRows().get(0).value().getString("caseId");
            String secondCaseId = secondResult.allRows().get(0).value().getString("caseId");

            if (!firstCaseId.equals(secondCaseId))
            {
                return true;
            }else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 用于postman的单个器官测试
    public String CBPDdiameterDist() {
        HashMap<String, Object> retMap = new HashMap<>();
        ArrayList<HashMap<String, Object>> ary = new ArrayList<>();
        HashMap<String, Object> tmp1 = new HashMap<>();
        HashMap<String, Object> tmp2 = new HashMap<>();
        HashMap<String, Object> tmp3 = new HashMap<>();
        HashMap<String, Object> tmp4 = new HashMap<>();
        int Count0To30 = 0, Count30To50 = 0, Count50To80 = 0, CountOver80 = 0;
        try {
            String n1qlQuery = "select distinct raw m from fetal_sys as b unnest b.diameter as m where b.type = 'draft' and b.status = '1' and m.name = 'CBPD'; ";
            N1qlQueryResult result = bucket.query(N1qlQuery.simple(n1qlQuery));

            for (N1qlQueryRow row: result) {
                // debug
//                System.out.println("CBPDdiameterDist:"+row.value());

                Double value = row.value().getDouble("value");
                if (value < 30) {
                    Count0To30++;
//                    tmp1.put("name", "0-3cm");
//                    tmp1.put("value", Count0To30);
                } else if (value >= 30 && value < 50) {
                    Count30To50++;
//                    tmp2.put("name", "3-5cm");
//                    tmp2.put("value", Count30To50);
                } else if (value >= 50 && value < 80) {
                    Count50To80++;
//                    tmp3.put("name", "5-8cm");
//                    tmp3.put("value", Count50To80);
                } else if (value >= 80) {
                    CountOver80++;
//                    tmp4.put("name", "8cm+");
//                    tmp4.put("value", CountOver80);
                }
            }
            tmp1.put("name", "0-3cm");
            tmp1.put("value", Count0To30);
            tmp2.put("name", "3-5cm");
            tmp2.put("value", Count30To50);
            tmp3.put("name", "5-8cm");
            tmp3.put("value", Count50To80);
            tmp4.put("name", "8cm+");
            tmp4.put("value", CountOver80);

            ary.add(tmp1);
            ary.add(tmp2);
            ary.add(tmp3);
            ary.add(tmp4);

            retMap.put("status", "okay");
            retMap.put("data", ary);

            return new Gson().toJson(retMap);
        } catch (Exception e) {
            retMap.put("status", "failed");
            retMap.put("data", ary);
            return new Gson().toJson(retMap);
        }
    }

    // 用于postman的单个器官测试
    public String TCDdiameterDist() {
        HashMap<String, Object> retMap = new HashMap<>();
        ArrayList<HashMap<String, Object>> ary = new ArrayList<>();
        HashMap<String, Object> tmp1 = new HashMap<>();
        HashMap<String, Object> tmp2 = new HashMap<>();
        HashMap<String, Object> tmp3 = new HashMap<>();
        HashMap<String, Object> tmp4 = new HashMap<>();
        // debug
//        System.out.println("CBPDdiameterDist");
        int Count0To10 = 0, Count10To15 = 0, Count15To20 = 0, CountOver20 = 0;
        try {
            String n1qlQuery = "select distinct raw m from fetal_sys as b unnest b.diameter as m where b.type = 'draft' and b.status = '1' and m.name = 'TCD'; ";
            N1qlQueryResult result = bucket.query(N1qlQuery.simple(n1qlQuery));

            for (N1qlQueryRow row: result) {
                // debug
//                System.out.println("CBPDdiameterDist:"+row.value());
                Double value = row.value().getDouble("value");
                if (value < 10) {
                    Count0To10++;
//                    retMap.put("0-10mm", Count0To10);
                } else if (value >= 10 && value < 15) {
                    Count10To15++;
//                    retMap.put("10-15mm", Count10To15);
                } else if (value >= 15 && value < 20) {
                    Count15To20++;
//                    retMap.put("15-20mm", Count15To20);
                } else if (value >= 20) {
                    CountOver20++;
//                    retMap.put("20mm+", CountOver20);
                }
            }

            tmp1.put("name", "0-10mm");
            tmp1.put("value", Count0To10);
            tmp2.put("name", "10-15mm");
            tmp2.put("value", Count10To15);
            tmp3.put("name", "15-20mm");
            tmp3.put("value", Count15To20);
            tmp4.put("name", "20mm+");
            tmp4.put("value", CountOver20);

            ary.add(tmp1);
            ary.add(tmp2);
            ary.add(tmp3);
            ary.add(tmp4);

            retMap.put("status", "okay");
            retMap.put("data", ary);

            return new Gson().toJson(retMap);
        } catch (Exception e) {
            retMap.put("status", "failed");
            retMap.put("data", retMap);
            return new Gson().toJson(retMap);
        }
    }

    // 用于postman的单个器官测试
    public String SWdiameterDist() {
        HashMap<String, Object> retMap = new HashMap<>();
        ArrayList<HashMap<String, Object>> ary = new ArrayList<>();
        HashMap<String, Object> tmp1 = new HashMap<>();
        HashMap<String, Object> tmp2 = new HashMap<>();
        HashMap<String, Object> tmp3 = new HashMap<>();
        HashMap<String, Object> tmp4 = new HashMap<>();
        int Count0To3 = 0, Count3To5= 0, Count5To8 = 0, CountOver8 = 0;
        try {
            String n1qlQuery = "select distinct raw m from fetal_sys as b unnest b.diameter as m where b.type = 'draft' and b.status = '1' and m.name = 'SW'; ";
            N1qlQueryResult result = bucket.query(N1qlQuery.simple(n1qlQuery));

            for (N1qlQueryRow row: result) {
                // debug
//                System.out.println("CBPDdiameterDist:"+row.value());
                Double value = row.value().getDouble("value");
                if (value < 3) {
                    Count0To3++;
//                    retMap.put("0-3cm", Count0To30);
                } else if (value >= 3 && value < 5) {
                    Count3To5++;
//                    retMap.put("3-5cm", Count30To50);
                } else if (value >= 5 && value < 8) {
                    Count5To8++;
//                    retMap.put("5-8cm", Count50To80);
                } else if (value >= 8) {
                    CountOver8++;
//                    retMap.put("8cm+", CountOver80);
                }
            }
            tmp1.put("name", "0-3mm");
            tmp1.put("value", Count0To3);
            tmp2.put("name", "3-5mm");
            tmp2.put("value", Count3To5);
            tmp3.put("name", "5-8mm");
            tmp3.put("value", Count5To8);
            tmp4.put("name", "8mm+");
            tmp4.put("value", CountOver8);

            ary.add(tmp1);
            ary.add(tmp2);
            ary.add(tmp3);
            ary.add(tmp4);

            retMap.put("status", "okay");
            retMap.put("data", ary);
            return new Gson().toJson(retMap);
        } catch (Exception e) {
            retMap.put("status", "failed");
            retMap.put("data", retMap);
            return new Gson().toJson(retMap);
        }
    }
    // 用于postman的单个器官测试
    public String SHdiameterDist() {
        HashMap<String, Object> retMap = new HashMap<>();
        ArrayList<HashMap<String, Object>> ary = new ArrayList<>();
        HashMap<String, Object> tmp1 = new HashMap<>();
        HashMap<String, Object> tmp2 = new HashMap<>();
        HashMap<String, Object> tmp3 = new HashMap<>();
        HashMap<String, Object> tmp4 = new HashMap<>();
        int Count0To3 = 0, Count3To5= 0, Count5To8 = 0, CountOver8 = 0;
        try {
            String n1qlQuery = "select distinct raw m from fetal_sys as b unnest b.diameter as m where b.type = 'draft' and b.status = '1' and m.name = 'SH'; ";
            N1qlQueryResult result = bucket.query(N1qlQuery.simple(n1qlQuery));

            for (N1qlQueryRow row: result) {
                // debug
//                System.out.println("CBPDdiameterDist:"+row.value());

                Double value = row.value().getDouble("value");
                if (value < 3) {
                    Count0To3++;
//                    retMap.put("0-3cm", Count0To30);
                } else if (value >= 3 && value < 5) {
                    Count3To5++;
//                    retMap.put("3-5cm", Count30To50);
                } else if (value >= 5 && value < 8) {
                    Count5To8++;
//                    retMap.put("5-8cm", Count50To80);
                } else if (value >= 8) {
                    CountOver8++;
//                    retMap.put("8cm+", CountOver80);
                }
            }
            tmp1.put("name", "0-3mm");
            tmp1.put("value", Count0To3);
            tmp2.put("name", "3-5mm");
            tmp2.put("value", Count3To5);
            tmp3.put("name", "5-8mm");
            tmp3.put("value", Count5To8);
            tmp4.put("name", "8mm+");
            tmp4.put("value", CountOver8);

            ary.add(tmp1);
            ary.add(tmp2);
            ary.add(tmp3);
            ary.add(tmp4);

            retMap.put("status", "okay");
            retMap.put("data", ary);

            return new Gson().toJson(retMap);
        } catch (Exception e) {
            retMap.put("status", "failed");
            retMap.put("data", retMap);
            return new Gson().toJson(retMap);
        }
    }
    // 用于postman的单个器官测试
    public String TAWdiameterDist() {
        HashMap<String, Object> retMap = new HashMap<>();
        ArrayList<HashMap<String, Object>> ary = new ArrayList<>();
        HashMap<String, Object> tmp1 = new HashMap<>();
        HashMap<String, Object> tmp2 = new HashMap<>();
        HashMap<String, Object> tmp3 = new HashMap<>();
        HashMap<String, Object> tmp4 = new HashMap<>();
        int Count0To3 = 0, Count3To5= 0, Count5To8 = 0, CountOver8 = 0;
        try {
            String n1qlQuery = "select distinct raw m from fetal_sys as b unnest b.diameter as m where b.type = 'draft' and b.status = '1' and m.name = 'TAW'; ";
            N1qlQueryResult result = bucket.query(N1qlQuery.simple(n1qlQuery));

            for (N1qlQueryRow row: result) {
                // debug
//                System.out.println("CBPDdiameterDist:"+row.value());

                Double value = row.value().getDouble("value");
                if (value < 3) {
                    Count0To3++;
//                    retMap.put("0-3cm", Count0To30);
                } else if (value >= 3 && value < 5) {
                    Count3To5++;
//                    retMap.put("3-5cm", Count30To50);
                } else if (value >= 5 && value < 8) {
                    Count5To8++;
//                    retMap.put("5-8cm", Count50To80);
                } else if (value >= 8) {
                    CountOver8++;
//                    retMap.put("8cm+", CountOver80);
                }
            }
            tmp1.put("name", "0-3mm");
            tmp1.put("value", Count0To3);
            tmp2.put("name", "3-5mm");
            tmp2.put("value", Count3To5);
            tmp3.put("name", "5-8mm");
            tmp3.put("value", Count5To8);
            tmp4.put("name", "8mm+");
            tmp4.put("value", CountOver8);

            ary.add(tmp1);
            ary.add(tmp2);
            ary.add(tmp3);
            ary.add(tmp4);

            retMap.put("status", "okay");
            retMap.put("data", ary);
            return new Gson().toJson(retMap);
        } catch (Exception e) {
            retMap.put("status", "failed");
            retMap.put("data", retMap);
            return new Gson().toJson(retMap);
        }
    }
    // 用于postman的单个器官测试
    public String LADdiameterDist() {
        HashMap<String, Object> retMap = new HashMap<>();
        ArrayList<HashMap<String, Object>> ary = new ArrayList<>();
        HashMap<String, Object> tmp1 = new HashMap<>();
        HashMap<String, Object> tmp2 = new HashMap<>();
        HashMap<String, Object> tmp3 = new HashMap<>();
        HashMap<String, Object> tmp4 = new HashMap<>();
        int Count0To3 = 0, Count3To5= 0, Count5To8 = 0, CountOver8 = 0;
        try {
            String n1qlQuery = "select distinct raw m from fetal_sys as b unnest b.diameter as m where b.type = 'draft' and b.status = '1' and m.name = 'LAD'; ";
            N1qlQueryResult result = bucket.query(N1qlQuery.simple(n1qlQuery));

            for (N1qlQueryRow row: result) {
                // debug
//                System.out.println("CBPDdiameterDist:"+row.value());

                Double value = row.value().getDouble("value");
                if (value < 3) {
                    Count0To3++;
//                    retMap.put("0-3cm", Count0To30);
                } else if (value >= 3 && value < 5) {
                    Count3To5++;
//                    retMap.put("3-5cm", Count30To50);
                } else if (value >= 5 && value < 8) {
                    Count5To8++;
//                    retMap.put("5-8cm", Count50To80);
                } else if (value >= 8) {
                    CountOver8++;
//                    retMap.put("8cm+", CountOver80);
                }
            }
            tmp1.put("name", "0-3mm");
            tmp1.put("value", Count0To3);
            tmp2.put("name", "3-5mm");
            tmp2.put("value", Count3To5);
            tmp3.put("name", "5-8mm");
            tmp3.put("value", Count5To8);
            tmp4.put("name", "8mm+");
            tmp4.put("value", CountOver8);

            ary.add(tmp1);
            ary.add(tmp2);
            ary.add(tmp3);
            ary.add(tmp4);

            retMap.put("status", "okay");
            retMap.put("data", ary);

            return new Gson().toJson(retMap);
        } catch (Exception e) {
            retMap.put("status", "failed");
            retMap.put("data", retMap);
            return new Gson().toJson(retMap);
        }
    }
    // 用于postman的单个器官测试
    public String RADdiameterDist() {
        HashMap<String, Object> retMap = new HashMap<>();
        ArrayList<HashMap<String, Object>> ary = new ArrayList<>();
        HashMap<String, Object> tmp1 = new HashMap<>();
        HashMap<String, Object> tmp2 = new HashMap<>();
        HashMap<String, Object> tmp3 = new HashMap<>();
        HashMap<String, Object> tmp4 = new HashMap<>();
        int Count0To3 = 0, Count3To5= 0, Count5To8 = 0, CountOver8 = 0;
        try {
            String n1qlQuery = "select distinct raw m from fetal_sys as b unnest b.diameter as m where b.type = 'draft' and b.status = '1' and m.name = 'RAD'; ";
            N1qlQueryResult result = bucket.query(N1qlQuery.simple(n1qlQuery));

            for (N1qlQueryRow row: result) {
                // debug
//                System.out.println("CBPDdiameterDist:"+row.value());

                Double value = row.value().getDouble("value");
                if (value < 3) {
                    Count0To3++;
//                    retMap.put("0-3cm", Count0To30);
                } else if (value >= 3 && value < 5) {
                    Count3To5++;
//                    retMap.put("3-5cm", Count30To50);
                } else if (value >= 5 && value < 8) {
                    Count5To8++;
//                    retMap.put("5-8cm", Count50To80);
                } else if (value >= 8) {
                    CountOver8++;
//                    retMap.put("8cm+", CountOver80);
                }
            }
            tmp1.put("name", "0-3mm");
            tmp1.put("value", Count0To3);
            tmp2.put("name", "3-5mm");
            tmp2.put("value", Count3To5);
            tmp3.put("name", "5-8mm");
            tmp3.put("value", Count5To8);
            tmp4.put("name", "8mm+");
            tmp4.put("value", CountOver8);

            ary.add(tmp1);
            ary.add(tmp2);
            ary.add(tmp3);
            ary.add(tmp4);

            retMap.put("status", "okay");
            retMap.put("data", ary);

            return new Gson().toJson(retMap);
        } catch (Exception e) {
            retMap.put("status", "failed");
            retMap.put("data", retMap);
            return new Gson().toJson(retMap);
        }
    }
    // 用于postman的单个器官测试
    public String LHVvolume() {
        HashMap<String, Object> retMap = new HashMap<>();
        ArrayList<HashMap<String, Object>> ary = new ArrayList<>();
        HashMap<String, Object> tmp1 = new HashMap<>();
        HashMap<String, Object> tmp2 = new HashMap<>();
        HashMap<String, Object> tmp3 = new HashMap<>();
        HashMap<String, Object> tmp4 = new HashMap<>();
        int Count0To50 = 0, Count50To100 = 0, Count100To150 = 0, CountOver150 = 0;
        try {
            String n1qlQuery = "select distinct raw m from fetal_sys as b unnest b.volume as m where b.type = 'draft' and b.status = '1' and m.name = 'LHV'; ";
            N1qlQueryResult result = bucket.query(N1qlQuery.simple(n1qlQuery));

            for (N1qlQueryRow row: result) {
                // debug
//                System.out.println("CBPDdiameterDist:"+row.value());

                Double value = row.value().getDouble("value");
                if (value < 50000) {
                    Count0To50++;
//                    retMap.put("0-5cm³", Count0To50);
                } else if (value >= 50000 && value < 100000) {
                    Count50To100++;
//                    retMap.put("5-10cm³", Count50To100);
                } else if (value >= 100000 && value < 150000) {
                    Count100To150++;
//                    retMap.put("10-15cm³", Count100To150);
                } else if (value >= 150000) {
                    CountOver150++;
//                    retMap.put("15cm³+", CountOver150);
                }
            }
            tmp1.put("name", "0-5cm³");
            tmp1.put("value", Count0To50);
            tmp2.put("name", "5-10cm³");
            tmp2.put("value", Count50To100);
            tmp3.put("name", "10-15cm³");
            tmp3.put("value", Count100To150);
            tmp4.put("name", "15cm³+");
            tmp4.put("value", CountOver150);

            ary.add(tmp1);
            ary.add(tmp2);
            ary.add(tmp3);
            ary.add(tmp4);

            retMap.put("status", "okay");
            retMap.put("data", ary);

            return new Gson().toJson(retMap);
        } catch (Exception e) {
            retMap.put("status", "failed");
            retMap.put("data", retMap);
            return new Gson().toJson(retMap);
        }
    }
    // 用于postman的单个器官测试
    public String RHVvolume() {
        HashMap<String, Object> retMap = new HashMap<>();
        ArrayList<HashMap<String, Object>> ary = new ArrayList<>();
        HashMap<String, Object> tmp1 = new HashMap<>();
        HashMap<String, Object> tmp2 = new HashMap<>();
        HashMap<String, Object> tmp3 = new HashMap<>();
        HashMap<String, Object> tmp4 = new HashMap<>();
        int Count0To50 = 0, Count50To100 = 0, Count100To150 = 0, CountOver150 = 0;
        try {
            String n1qlQuery = "select distinct raw m from fetal_sys as b unnest b.volume as m where b.type = 'draft' and b.status = '1' and m.name = 'RHV'; ";
            N1qlQueryResult result = bucket.query(N1qlQuery.simple(n1qlQuery));

            for (N1qlQueryRow row: result) {
                // debug
//                System.out.println("CBPDdiameterDist:"+row.value());

                Double value = row.value().getDouble("value");
                if (value < 50000) {
                    Count0To50++;
//                    retMap.put("0-5cm³", Count0To50);
                } else if (value >= 50000 && value < 100000) {
                    Count50To100++;
//                    retMap.put("5-10cm³", Count50To100);
                } else if (value >= 100000 && value < 150000) {
                    Count100To150++;
//                    retMap.put("10-15cm³", Count100To150);
                } else if (value >= 150000) {
                    CountOver150++;
//                    retMap.put("15cm³+", CountOver150);
                }
            }
            tmp1.put("name", "0-5cm³");
            tmp1.put("value", Count0To50);
            tmp2.put("name", "5-10cm³");
            tmp2.put("value", Count50To100);
            tmp3.put("name", "10-15cm³");
            tmp3.put("value", Count100To150);
            tmp4.put("name", "15cm³+");
            tmp4.put("value", CountOver150);

            ary.add(tmp1);
            ary.add(tmp2);
            ary.add(tmp3);
            ary.add(tmp4);

            retMap.put("status", "okay");
            retMap.put("data", ary);

            return new Gson().toJson(retMap);
        } catch (Exception e) {
            retMap.put("status", "failed");
            retMap.put("data", retMap);
            return new Gson().toJson(retMap);
        }
    }
    // 用于postman的单个器官测试
    public String TAarea() {
        HashMap<String, Object> retMap = new HashMap<>();
        ArrayList<HashMap<String, Object>> ary = new ArrayList<>();
        HashMap<String, Object> tmp1 = new HashMap<>();
        HashMap<String, Object> tmp2 = new HashMap<>();
        HashMap<String, Object> tmp3 = new HashMap<>();
        HashMap<String, Object> tmp4 = new HashMap<>();
        int Count0To300 = 0, Count30To500 = 0, Count50To800 = 0, CountOver800 = 0;
        try {
            String n1qlQuery = "select distinct raw m from fetal_sys as b unnest b.area as m where b.type = 'draft' and b.status = '1' and m.name = 'TA'; ";
            N1qlQueryResult result = bucket.query(N1qlQuery.simple(n1qlQuery));

            for (N1qlQueryRow row: result) {
                // debug
//                System.out.println("CBPDdiameterDist:"+row.value());

                Double value = row.value().getDouble("value");
                if (value < 300) {
                    Count0To300++;
//                    retMap.put("0-3cm²", Count0To300);
                } else if (value >= 300 && value < 500) {
                    Count30To500++;
//                    retMap.put("3-5cm²", Count30To500);
                } else if (value >= 500 && value < 800) {
                    Count50To800++;
//                    retMap.put("5-8cm²", Count50To800);
                } else if (value >= 800) {
                    CountOver800++;
//                    retMap.put("8cm²+", CountOver800);
                }
            }
            tmp1.put("name", "0-3cm²");
            tmp1.put("value", Count0To300);
            tmp2.put("name", "3-5cm²");
            tmp2.put("value", Count30To500);
            tmp3.put("name", "5-8cm²");
            tmp3.put("value", Count50To800);
            tmp4.put("name", "8cm²+");

            ary.add(tmp1);
            ary.add(tmp2);
            ary.add(tmp3);
            ary.add(tmp4);

            retMap.put("status", "okay");
            retMap.put("data", ary);
            return new Gson().toJson(retMap);
        } catch (Exception e) {
            retMap.put("status", "failed");
            retMap.put("data", retMap);
            return new Gson().toJson(retMap);
        }
    }

}
