package me.sihang.backend.service;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;
import com.google.gson.Gson;
import me.sihang.backend.config.PathConfig;
import me.sihang.backend.util.ConstantMap;
import me.sihang.backend.util.MD5;
import me.sihang.backend.util.RedisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.*;

@Service
public class RecordService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    private Bucket bucket;

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ConstantMap constantMap;

    @Autowired
    private RedisUtils ru;
    @Autowired
    public RecordService(Bucket bucket) {
        this.bucket = bucket;
    }

    public int getAllPatientsCount() {
        N1qlQueryResult result = bucket.query(
                N1qlQuery.simple("select count(patientId) as count from `bm_sys` where type='info'")
        );

        int no = (int) result.allRows().get(0).value().get("count");
        return no;
    }

    public int getAllRecordsCount() {
        N1qlQueryResult result = bucket.query(
                N1qlQuery.simple("select count(caseId) as count from `bm_sys` where type = 'record'")
        );
        int no = (int) result.allRows().get(0).value().get("count");
        return no;
    }

    public int getBCRecordsCount() {
        N1qlQueryResult result = bucket.query(
                N1qlQuery.simple("select count(caseId) as count from `bm_sys` where type='record' and caseId like '%BC%' ")
        );

        int no = (int) result.allRows().get(0).value().get("count");
        return no;
    }

    public int getModelProgress() {
        N1qlQueryResult result1 = bucket.query(
                N1qlQuery.simple("select count(distinct(caseId)) as count from `bm_sys` where type='draft' and status = '1'")
        );

//        N1qlQueryResult result2 = bucket.query(
//                N1qlQuery.simple("select count(distinct(caseId)) as count from `bm_sys` where type = 'record'")
//        );
//        int totalDraft = (int) result2.allRows().get(0).value().get("count");
//        int finishedDraft = (int) result1.allRows().get(0).value().get("count");
//        DecimalFormat formatter = new DecimalFormat("0.00");
//        float res = (float) finishedDraft / totalDraft * 100;
//        return formatter.format(res) + " %";
        int finishedDraft = (int) result1.allRows().get(0).value().get("count");

        return finishedDraft;
    }

    /*
    没有任何输入的情况下进行所有数据的展示
     */

    public ArrayList<Map<String, Object>> getMainListAtPageByPid(String pagenum) {

        ArrayList<Map<String, Object>> ret = new ArrayList<>();
        int offset = (Integer.parseInt(pagenum) - 1) * 10;

        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select patientId, patientSex,patientName from `fetal_sys` where type = 'info' limit 10 offset $1", JsonArray.from(offset))
        );
        for (N1qlQueryRow row : result) {
            Map<String, Object> patientInfo = new HashMap<>();
            patientInfo.put("patientId",(String) row.value().get("patientId"));
            patientInfo.put("gender",(String) row.value().get("patientSex"));
            patientInfo.put("patientName",(String) row.value().get("patientName"));
            ret.add(patientInfo);
        }

        return ret;
    }

    public ArrayList<String> getMainListAtPageByDate(String pagenum) {

        ArrayList<String> ret = new ArrayList<>();
        int offset = (Integer.parseInt(pagenum) - 1) * 10;
        int start = (Integer.parseInt(pagenum) - 1) * 10;
        int end = ((Integer.parseInt(pagenum) - 1) + 1) * 10 - 1;
        Set<Object> results = ru.zsRGet("dates", start, end);
        for (Object res : results) {
            ret.add(String.valueOf(res));
        }

        return ret;
    }


    public ArrayList<Map<String, Object>> getAllList(){
        ArrayList<Map<String, Object>> ret = new ArrayList<>();
        N1qlQueryResult result = bucket.query(
                N1qlQuery.simple("select patientId, patientSex,patientName from `fetal_sys` where type = 'info'")
        );
        for (N1qlQueryRow row : result) {
            Map<String, Object> patientInfo = new HashMap<>();
            patientInfo.put("patientId",(String) row.value().get("patientId"));
            patientInfo.put("gender",(String) row.value().get("patientSex"));
            patientInfo.put("patientName",(String) row.value().get("patientName"));
            ret.add(patientInfo);
        }

        return ret;
    }

    public ArrayList<Map<String, Object>> getAllListForSubset(List<Object> patientIds){

        ArrayList<Map<String, Object>> ret = new ArrayList<>();

        ArrayList<String> allPatientIdList = new ArrayList<>();
        for(Object patientId: patientIds){
            allPatientIdList.add("'" + patientId.toString() + "'");
        }
        String pidStrs = "[" + String.join(",", allPatientIdList) + "]";

        N1qlQueryResult result = bucket.query(
                N1qlQuery.simple("select patientId, patientSex,patientName from `bm_sys` where type = 'info' and patientId in " + pidStrs)
        );
        for (N1qlQueryRow row : result) {
            Map<String, Object> patientInfo = new HashMap<>();
            patientInfo.put("patientId",(String) row.value().get("patientId"));
            patientInfo.put("gender",(String) row.value().get("patientSex"));
            patientInfo.put("patientName",(String) row.value().get("patientName"));
            ret.add(patientInfo);
        }

        return ret;
    }

    public ArrayList<Map<String, Object>> getMainListForSubset(List<Object> patientIds, String pagenum){

        int offset = (Integer.parseInt(pagenum) - 1) * 10;
        ArrayList<Map<String, Object>> ret = new ArrayList<>();

        ArrayList<String> allPatientIdList = new ArrayList<>();
        for(Object patientId: patientIds){
            allPatientIdList.add("'" + patientId.toString() + "'");
        }
        String pidStrs = "[" + String.join(",", allPatientIdList) + "]";

        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select patientId, patientSex,patientName from `bm_sys` where type = 'info' and patientId in " + pidStrs +  " limit 10 offset $1", JsonArray.from(offset))
        );
        for (N1qlQueryRow row : result) {
            Map<String, Object> patientInfo = new HashMap<>();
            patientInfo.put("patientId",(String) row.value().get("patientId"));
            patientInfo.put("gender",(String) row.value().get("patientSex"));
            patientInfo.put("patientName",(String) row.value().get("patientName"));
            ret.add(patientInfo);
        }


        return ret;
    }

    public ArrayList<Map<String, Object>> getMainListForSubsetByPid(List<Object> patientIds, String pagenum, String pidKeyword, String dateKeyword){

        N1qlQueryResult result;
        int offset = (Integer.parseInt(pagenum) - 1) * 10;
        ArrayList<Map<String, Object>> ret = new ArrayList<>();

        ArrayList<String> allPatientIdList = new ArrayList<>();
        for(Object patientId: patientIds){
            allPatientIdList.add("'" + patientId.toString() + "'");
        }
        String pidStrs = "[" + String.join(",", allPatientIdList) + "]";


        ArrayList<String> allPatientIdList_2 = new ArrayList<>();
        result = bucket.query(N1qlQuery.parameterized("select distinct(patientId) as v from `bm_sys` where type='record' and patientId in " + pidStrs + "and any pid_element in suffixes(patientId) satisfies pid_element like $1 end and any date_element in suffixes(date) satisfies date_element like $2 end order by date desc limit 10 offset $3", JsonArray.from(pidKeyword + "%", dateKeyword + "%", offset)));
        for (N1qlQueryRow row : result) {
            String patientId = (String) row.value().get("v");
            allPatientIdList_2.add("'" + patientId + "'");
        }
        String pidStrs2 = "[" + String.join(",", allPatientIdList_2) + "]";
        String n1qlString = "select patientId, patientSex,patientName from `bm_sys` where type = 'info' and patientId in " + pidStrs2 ;
        N1qlQueryResult result2 = bucket.query(
                N1qlQuery.simple(n1qlString)
        );
        for (N1qlQueryRow row : result2) {
            Map<String, Object> patientInfo = new HashMap<>();
            patientInfo.put("patientId",(String) row.value().get("patientId"));
            patientInfo.put("gender",(String) row.value().get("patientSex"));
            patientInfo.put("patientName",(String) row.value().get("patientName"));
            ret.add(patientInfo);
        }

        return ret;
    }


    public ArrayList<String> getMainListForSubsetByDate(List<Object> patientIds, String pagenum, String pidKeyword, String dateKeyword){

        N1qlQueryResult result;
        int offset = (Integer.parseInt(pagenum) - 1) * 10;
        ArrayList<String> ret = new ArrayList<>();

        ArrayList<String> allPatientIdList = new ArrayList<>();
        for(Object patientId: patientIds){
            allPatientIdList.add("'" + patientId.toString() + "'");
        }
        String pidStrs = "[" + String.join(",", allPatientIdList) + "]";


        result = bucket.query(N1qlQuery.parameterized("select distinct(date) as v from `bm_sys` where type='record' and patientId in" + pidStrs + " and  any pid_element in suffixes(patientId) satisfies pid_element like $1 end and any date_element in suffixes(date) satisfies date_element like $2 end order by date desc limit 10 offset $3", JsonArray.from(pidKeyword + "%", dateKeyword + "%", offset)));
        for (N1qlQueryRow row : result) {
            String value = (String) row.value().get("v");
            ret.add(value);
        }

        return ret;
    }

    public ArrayList<Map<String, Object>> getMainListAtPageByPid(String pageNum, String pidKeyword, String dateKeyword) {

        N1qlQueryResult result;
        ArrayList<Map<String, Object>> ret = new ArrayList<>();

        N1qlQueryResult result1 = null;
        if(!pageNum.equals("all")){
            int offset = (Integer.parseInt(pageNum) - 1) * 10;
            result1 = bucket.query(N1qlQuery.parameterized("select distinct(patientId) as v from `fetal_sys` where type='record' and any pid_element in suffixes(patientId) satisfies pid_element like $1 end and any date_element in suffixes(date) satisfies date_element like $2 end order by date asc limit 10 offset $3", JsonArray.from(pidKeyword + "%", dateKeyword + "%", offset)));
        }
        else{
            result1 = bucket.query(N1qlQuery.parameterized("select distinct(patientId) as v from `fetal_sys` where type='record' and any pid_element in suffixes(patientId) satisfies pid_element like $1 end and any date_element in suffixes(date) satisfies date_element like $2 end order by date asc", JsonArray.from(pidKeyword + "%", dateKeyword + "%")));

        }

        ArrayList<String> allPatientIdList = new ArrayList<>();
        for (N1qlQueryRow row : result1) {
            String patientId = (String) row.value().get("v");
            allPatientIdList.add("'" + patientId + "'");
        }
        String pidStrs = "[" + String.join(",", allPatientIdList) + "]";
        String n1qlString = "select patientId, patientSex,patientName from `bm_sys` where type = 'info' and patientId in " + pidStrs ;
        N1qlQueryResult result2 = bucket.query(
                N1qlQuery.simple(n1qlString)
                // N1qlQuery.parameterized("select distinct(patientId || \"_\" || patientName || \"_\" || patientSex) from `bm_sys` where type = 'info' and patientId in $1", JsonArray.from(allPatientIdList))
        );
        for (N1qlQueryRow row : result2) {
            Map<String, Object> patientInfo = new HashMap<>();
            patientInfo.put("patientId",(String) row.value().get("patientId"));
            patientInfo.put("gender",(String) row.value().get("patientSex"));
            patientInfo.put("patientName",(String) row.value().get("patientName"));
            ret.add(patientInfo);
        }

        return ret;
    }


    public ArrayList<String> getMainListAtPageByDate(String pageNum, String pidKeyword, String dateKeyword) {

        N1qlQueryResult result;
        ArrayList<String> ret = new ArrayList<>();

        int offset = (Integer.parseInt(pageNum) - 1) * 10;
        if (dateKeyword.length() == 0 && pidKeyword.length() == 0 && !pageNum.equals("all")) {
            try {
                int start = offset * 10;
                int end = (offset + 1) * 10 - 1;
                Set<Object> results = ru.zsRGet("dates", start, end);
                if (results.size() == 0) {
                    throw new Exception("No date in redis");
                } else {
                    for (Object value : results) {
                        ret.add(String.valueOf(value));
                    }
                }

            } catch (Exception e) {
//                    e.printStackTrace();
                result = bucket.query(N1qlQuery.parameterized("select distinct(date) as v from `fetal_sys` where type='record' and any pid_element in suffixes(patientId) satisfies pid_element like $1 end and any date_element in suffixes(date) satisfies date_element like $2 end order by date desc limit 10 offset $3", JsonArray.from(pidKeyword + "%", dateKeyword + "%", offset)));
                for (N1qlQueryRow row : result) {
                    String value = (String) row.value().get("v");
                    ret.add(String.valueOf(value));
                }
            }
        }else if (dateKeyword.length() == 4 && pidKeyword.length() == 0 && !pageNum.equals("all")) {
            try {

                int dateTime = Integer.parseInt(dateKeyword);
                if (dateTime >= 1979 && dateTime <= 2099) {
                    int start = offset * 10;
                    int end = (offset + 1) * 10 - 1;
                    Set<Object> results = ru.zsRGet("dates_" + dateKeyword, start, end);

                    if (results.size() == 0) {
                        throw new Exception("No valid year in redis");
                    } else {
                        for (Object value : results) {
                            ret.add(String.valueOf(value));
                        }
                    }
                } else {
                    throw new Exception("input keyword not years");
                }
            } catch (Exception e) {
//                    e.printStackTrace();
                result = bucket.query(N1qlQuery.parameterized("select distinct(date) as v from `fetal_sys` where type='record' and any pid_element in suffixes(patientId) satisfies pid_element like $1 end and any date_element in suffixes(date) satisfies date_element like $2 end order by date desc limit 10 offset $3", JsonArray.from(pidKeyword + "%", dateKeyword + "%", offset)));
                for (N1qlQueryRow row : result) {
                    String value = (String) row.value().get("v");
                    ret.add(String.valueOf(value));
                }
            }
        } else
            if(!pageNum.equals("all")){
            result = bucket.query(N1qlQuery.parameterized("select distinct(date) as v from `fetal_sys` where type='record' and any pid_element in suffixes(patientId) satisfies pid_element like $1 end and any date_element in suffixes(date) satisfies date_element like $2 end order by date desc limit 10 offset $3", JsonArray.from(pidKeyword + "%", dateKeyword + "%", offset)));
            for (N1qlQueryRow row : result) {
                String value = (String) row.value().get("v");
                ret.add(String.valueOf(value));
            }
        } else {
            result = bucket.query(N1qlQuery.parameterized("select distinct(date) as v from `fetal_sys` where type='record' and any pid_element in suffixes(patientId) satisfies pid_element like $1 end and any date_element in suffixes(date) satisfies date_element like $2 end order by date desc ", JsonArray.from(pidKeyword + "%", dateKeyword + "%")));
            for (N1qlQueryRow row : result) {
                String value = (String) row.value().get("v");
                ret.add(String.valueOf(value));
            }
        }


        return ret;
    }

    public ArrayList<String> getAllRelatedDates(String roughDate) {
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select distinct(date) from `bm_sys` where any ae in suffixes(date) satisfies ae like $1 end and type='record'", JsonArray.from(roughDate))
        );
        ArrayList<String> ret = new ArrayList<>();
        for (N1qlQueryRow row : result) {
            String patientId = (String) row.value().get("date");
            ret.add(patientId);
        }
        return ret;
    }

    public int getRecordsCount(String type, String pidKeyword, String dateKeyword) {
        N1qlQueryResult result;
        int no = 0;
        if (type.equals("pid")) {
            result = bucket.query(N1qlQuery.parameterized("select count(distinct(patientId)) as count from `fetal_sys` where type='record' and any pid_element in suffixes(patientId) satisfies pid_element like $1 end and any date_element in suffixes(date) satisfies date_element like $2 end", JsonArray.from(pidKeyword + "%", dateKeyword + "%")));
            no = (int) result.allRows().get(0).value().get("count");
        } else if (type.equals("date")) {
            result = bucket.query(N1qlQuery.parameterized("select count(distinct(date)) as count from `fetal_sys` where type='record' and any pid_element in suffixes(patientId) satisfies pid_element like $1 end and any date_element in suffixes(date) satisfies date_element like $2 end", JsonArray.from(pidKeyword + "%", dateKeyword + "%")));
            no = (int) result.allRows().get(0).value().get("count");
        }
        return no;
    }

    public int getRecordsCount(List<Object> patientIds,  String type, String pidKeyword, String dateKeyword) {
        N1qlQueryResult result;
        int no = 0;

        ArrayList<String> allPatientIdList = new ArrayList<>();
        for(Object patientId: patientIds){
            allPatientIdList.add("'" + patientId.toString() + "'");
        }
        String pidStrs = "[" + String.join(",", allPatientIdList) + "]";

        if (type.equals("pid")) {
            result = bucket.query(N1qlQuery.parameterized("select count(distinct(patientId)) as count from `fetal_sys` where type='record' and patientId in  " + pidStrs + " and any pid_element in suffixes(patientId) satisfies pid_element like $1 end and any date_element in suffixes(date) satisfies date_element like $2 end", JsonArray.from(pidKeyword + "%", dateKeyword + "%")));
            no = (int) result.allRows().get(0).value().get("count");
        } else if (type.equals("date")) {
            result = bucket.query(N1qlQuery.parameterized("select count(distinct(date)) as count from `fetal_sys` where type='record' and patientId in  " + pidStrs + " and any pid_element in suffixes(patientId) satisfies pid_element like $1 end and any date_element in suffixes(date) satisfies date_element like $2 end", JsonArray.from(pidKeyword + "%", dateKeyword + "%")));
            no = (int) result.allRows().get(0).value().get("count");
        }
        return no;
    }

    public int getRecordsCount(String type) {
        N1qlQueryResult result;
        int no = 0;
        if (type.equals("pid")) {
            result = bucket.query(
                    N1qlQuery.simple("select count(distinct(patientId)) as count from `fetal_sys` where type = 'info'")
            );
            no = (int) result.allRows().get(0).value().get("count");
        } else if (type.equals("date")) {
            no = Math.toIntExact(ru.zsSize("dates"));
            System.out.println();
        }

//        int no = (int) result.allRows().get(0).value().get("count");
        return no;
    }

    public String getPatientInfo(String patientId) {
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select * from `fetal_sys` where type = 'info' and patientId = $1", JsonArray.from(patientId))
        );
        JsonObject content = result.allRows().get(0).value().getObject("fetal_sys");
        String patientSex = content.getString("patientSex");
        String patientName = content.getString("patientName");
        String patientBirth = content.getString("patientBirth");
        N1qlQueryResult noduleResult = bucket.query(
//                N1qlQuery.parameterized("select caseId, username, rects, spacing from `bm_sys` as s where type = 'draft' and status = '1' and username = 'bm_sys' and caseId in (select raw(caseId) from `bm_sys` where type = 'record' and patientId = $1)", JsonArray.from(patientId))
//                N1qlQuery.parameterized("select d.caseId,d.username,d.rects,d.spacing,r.date from fetal_sys d inner join fetal_sys r on (d.caseId = r.caseId) where r.patientId = $1 and d.type = 'draft' and r.type = 'record' and d.username = 'bm_sys'", JsonArray.from(patientId))
                N1qlQuery.parameterized("select d.caseId,d.username,d.spacing,r.date from fetal_sys d inner join fetal_sys r on (d.caseId = r.caseId) where r.patientId = $1 and d.type = 'draft' and r.type = 'record' and d.username = 'bm_sys'", JsonArray.from(patientId))
        );
        HashMap<String, HashMap<String, Double>> caseHashMap = new HashMap<>();
        for (N1qlQueryRow row : noduleResult) {
            JsonObject record = row.value();
            String caseId = record.getString("caseId");
//            String date = caseId.split("_")[1];
            String date = record.getString("date");
            String username = record.getString("username");
            double spacing = record.getDouble("spacing");
//            JsonArray rects = record.getArray("rects");

            double largestVolume = 0.0f;
            double largestDiameter = 0.0;

//            if (rects.size() > 0) {
//                for (int i = 0; i < rects.size(); i ++) {
//                    String noduleId = (String) rects.get(i);
//                    JsonObject jo = bucket.get(noduleId).content();
////                    JsonObject jo = rects.getObject(i);
//                    if (jo.isEmpty()) {
//                        continue;
//                    }
//                    double vol = jo.getDouble("volume");
//                    if (vol > largestVolume) {
//                        largestVolume = vol;
//                        double y1 = jo.getDouble("y1");
//                        double y2 = jo.getDouble("y2");
//                        double x1 = jo.getDouble("x1");
//                        double x2 = jo.getDouble("x2");
//                        double xLen = x2 - x1;
//                        double yLen = y2 - y1;
//                        largestDiameter = Math.sqrt(xLen * xLen * spacing + yLen * yLen * spacing);
//                    }
//                }

//                HashMap<String, Double> volumeAndDiamter = new HashMap<>();
//                volumeAndDiamter.put("volume", largestVolume);
//                volumeAndDiamter.put("diameter", largestDiameter);

//                if (!caseHashMap.containsKey(date)) {
//                    caseHashMap.put(date, volumeAndDiamter);
//                } else {
//                    double oldVolume = caseHashMap.get(date).get("volume");
//                    if (largestVolume > oldVolume) {
//                        caseHashMap.replace(date, volumeAndDiamter);
//                    }
//
//                }


//            }

        }
        HashMap<String, Object> retMap = new HashMap<>();
        retMap.put("patientName", patientName);
        retMap.put("patientSex", patientSex);
        retMap.put("patientBirth", patientBirth);
        ArrayList<HashMap<String, Object>> caseList = new ArrayList<>();
        Iterator it = caseHashMap.entrySet().iterator();
        double maxDiameter = Double.MIN_VALUE;
        while(it.hasNext()) {
            Map.Entry<String, HashMap<String, Double>> pair = (Map.Entry) it.next();
            HashMap<String, Object> tmp = new HashMap<>();
            tmp.put("date", pair.getKey());
//            double diameter = pair.getValue().get("diameter");
//            tmp.put("diameter", diameter);
//            if (diameter > maxDiameter) {
//                maxDiameter = diameter;
//            }
//            tmp.put("volume", pair.getValue().get("volume"));
//            caseList.add(tmp);
        }

        retMap.put("stats", caseList);
//        retMap.put("maxDiameter", maxDiameter);
        return new Gson().toJson(retMap);
    }



    public HashMap<String, ArrayList<String>> getStudyForMainItem(String type, String mainItem, String otherKeyword) {
        N1qlQueryResult result = null;
        otherKeyword += '%';
        if (type.equals("pid")) {
            result = bucket.query(N1qlQuery.parameterized("select date, seriesId, caseId from `bm_sys` where type='record' and patientId = $1 and any date_element in suffixes(date) satisfies date_element like $2 end;", JsonArray.from(mainItem, otherKeyword)));
            HashMap<String, ArrayList<String>> dateToStudy = new HashMap<>();
            for (N1qlQueryRow row : result) {
                String date = (String) row.value().get("date");
                String caseId = (String) row.value().get("caseId");
                String seriesId = (String) row.value().get("seriesId");
                String value = caseId + "#" + seriesId;
                if (!dateToStudy.containsKey(date)) {
                    ArrayList<String> lst = new ArrayList<String>();
                    lst.add(value);
                    dateToStudy.put(date, lst);
                } else {
                    ArrayList<String> curLst = dateToStudy.get(date);
                    curLst.add(value);
                    dateToStudy.replace(date, curLst);
                }
            }
            return dateToStudy;
        }
        else if (type.equals("date")) {

            N1qlQuery query = N1qlQuery.parameterized("select caseId, patientId, seriesId from `bm_sys` where type='record' and date = $1 and any pid_element in suffixes(patientId) satisfies pid_element like $2 end;", JsonArray.from(mainItem, otherKeyword)) ;
            result = bucket.query(query);
            HashMap<String, ArrayList<String>> pidToStudy = new HashMap<>();
            for (N1qlQueryRow row : result) {
                String patientId = (String) row.value().get("patientId");
                String caseId = (String) row.value().get("caseId");
                String seriesId = (String) row.value().get("seriesId");
                N1qlQuery infoQuery = N1qlQuery.parameterized("select patientName, patientSex from `bm_sys` where type = 'info' and patientId = $1", JsonArray.from(patientId));
                JsonObject jo = bucket.query(infoQuery).allRows().get(0).value();
                String patientName = (String) jo.get("patientName");
                String patientSex = (String) jo.get("patientSex");
                String key = patientId + "_" + patientName + "_" + patientSex;
//                String key = patientId + "_noname_M";
                String value = caseId + "#" + seriesId;
                if (!pidToStudy.containsKey(key)) {
                    ArrayList<String> lst = new ArrayList<String>();
                    lst.add(value);
                    pidToStudy.put(key, lst);
                } else {
                    ArrayList<String> curLst = pidToStudy.get(key);
                    curLst.add(value);
                    pidToStudy.replace(key, curLst);
                }
            }
            return pidToStudy;
        }

        else return null;
    }


    public TreeMap<String, ArrayList<Map<String, Object>>> getStudyForMainItem_front(String type, String mainItem, String otherKeyword) {
        N1qlQueryResult result = null;
        otherKeyword += '%';

        if (type.equals("pid")) {
            result = bucket.query(N1qlQuery.parameterized("select a.date,a.seriesId, a.description, a.caseId,b.patientId,b.patientName,b.patientSex from fetal_sys a join  fetal_sys b on a.patientId = b.patientId where a.type = 'record' and b.type = 'info' and a.patientId = $1 and any date_element in suffixes(a.date) satisfies date_element like $2 end order by a.date,a.caseId;", JsonArray.from(mainItem,otherKeyword)));
            TreeMap<String, ArrayList<Map<String, Object>>> dateToStudy = new TreeMap<>(new Comparator<String>() {
                public int compare(String obj1, String obj2) {
                    // 降序排序
                    return obj2.compareTo(obj1);
                }
            });
            for (N1qlQueryRow row : result) {
                Map<String, Object> caseObject = new HashMap<>();
                String date = (String) row.value().get("date");
                String caseId = (String) row.value().get("caseId");
                String seriesId = (String) row.value().get("seriesId");
                String patientName = (String) row.value().get("patientName");
                String patientId = (String) row.value().get("patientId");
                String gender = (String) row.value().get("patientSex");
                String description;
                if(row.value().containsKey("description")){
                    description = (String) row.value().get("description");
                }else {
                    description = seriesId;
                }
                caseObject.put("caseId",caseId);
                caseObject.put("patientName",patientName);
                caseObject.put("patientId",patientId);
                caseObject.put("date",date);
                caseObject.put("gender",gender);
                caseObject.put("description",description);


                if (!dateToStudy.containsKey(date)) {
                    ArrayList<Map<String, Object>> lst = new ArrayList<Map<String, Object>>();
                    lst.add(caseObject);
                    dateToStudy.put(date, lst);
                } else {
                    ArrayList<Map<String, Object>> curLst = dateToStudy.get(date);

                    if(caseId.contains("BC")){
                        curLst.add(0,caseObject);
                    }else {
                        curLst.add(caseObject);
                    }
                    dateToStudy.replace(date, curLst);
                }
            }
            return dateToStudy;
        }
        else if (type.equals("date")) {

//            N1qlQuery query = N1qlQuery.parameterized("select a.date,a.seriesId, a.description, a.caseId,b.patientId,b.patientName,b.patientSex from bm_sys a join  bm_sys b on a.patientId = b.patientId where a.type = 'record' and b.type = 'info' and a.date = $1 and any pid_element in suffixes(a.patientId) satisfies pid_element like $2 end;", JsonArray.from(mainItem, otherKeyword)) ;
            N1qlQuery query = N1qlQuery.parameterized("select date,seriesId, description,caseId,patientId from fetal_sys  where type = 'record' and date = $1;", JsonArray.from(mainItem)) ;
            result = bucket.query(query);
            TreeMap<String, ArrayList<Map<String, Object>>> pidToStudy = new TreeMap<>();
            if(!result.rows().hasNext()) {
                return pidToStudy;
            }

            for (N1qlQueryRow row : result){
                Map<String, Object> caseObject = new HashMap<>();
                String date = (String) row.value().get("date");
                String caseId = (String) row.value().get("caseId");
                String seriesId = (String) row.value().get("seriesId");
//                String patientName = (String) row.value().get("patientName");
                String patientId = (String) row.value().get("patientId");
//                String gender = (String) row.value().get("patientSex");
                String description;
                if(row.value().containsKey("description")){
                    description = (String) row.value().get("description");
                }else {
                    description = seriesId;
                }
                caseObject.put("caseId",caseId);
//                caseObject.put("patientName",patientName);
                caseObject.put("patientId",patientId);
                caseObject.put("date",date);
//                caseObject.put("gender",gender);
                caseObject.put("description",description);


                if (!pidToStudy.containsKey(patientId)) {
                    ArrayList<Map<String, Object>> lst = new ArrayList<Map<String, Object>>();
                    lst.add(caseObject);
                    pidToStudy.put(patientId, lst);
                } else {
                    ArrayList<Map<String, Object>> curLst = pidToStudy.get(patientId);

                    if(caseId.contains("BC")){
                        curLst.add(0,caseObject);
                    }else {
                        curLst.add(caseObject);
                    }
                    pidToStudy.replace(patientId, curLst);
                }
            }
            return pidToStudy;
        }
        else return null;
    }

    public HashMap<String,String> getAllInfo_APP(){
        N1qlQueryResult result = bucket.query(N1qlQuery.simple("select a.patientId, a.caseId, b.patientName from  `bm_sys` a join  `bm_sys` b on (a.patientId = b.patientId) where a.type = 'record' and b.type = 'info' "));
        HashMap<String,String> tmp = new HashMap<>();
        for (N1qlQueryRow row : result){
            String patientId = (String)row.value().getString("patientId");
            String caseId = (String)row.value().getString("caseId");


            if(tmp.get(patientId) != null){
                String olddate = tmp.get(patientId).split("_")[1];
                String curdate = caseId.split("_")[1];

                int comp = curdate.compareTo(olddate);
                if(comp > 0 || (comp == 0 && caseId.contains("BC")) ){
                    String patientName = (String)row.value().getString("patientName");
                    if(patientName == null){
                        patientName = "Anonymous";
                    }
                    tmp.put(patientId,patientName + "%" + caseId);
                }

            }else {
                String patientName = (String)row.value().getString("patientName");
                if(patientName == null){
                    patientName = "Anonymous";
                }
                tmp.put(patientId,patientName + "%" + caseId);
            }

        }

        return tmp;
    }

    public HashMap<String,String> getInfoForPatient_APP(String patient_Id){
        N1qlQueryResult result = bucket.query(N1qlQuery.parameterized("select a.patientId, a.caseId, b.patientSex, b.patientName, b.idNumber from  `bm_sys` a join  `bm_sys` b on (a.patientId = b.patientId) where a.type = 'record' and b.type = 'info' and a.patientId = $1 ", JsonArray.from(patient_Id)));
        HashMap<String,String> tmp = new HashMap<>();
        for (N1qlQueryRow row : result){
            String patientId = (String)row.value().getString("patientId");
            String caseId = (String)row.value().getString("caseId");
            String patientSex = (String)row.value().getString("patientSex");
            String idNumber = (String)row.value().getString("idNumber");


            if(tmp.get(patientId) != null){
                String olddate = tmp.get(patientId).split("_")[1];
                String curdate = caseId.split("_")[1];

                int comp = curdate.compareTo(olddate);
                if(comp > 0 || (comp == 0 && caseId.contains("BC")) ){
                    String patientName = (String)row.value().getString("patientName");
                    if(patientName == null){
                        patientName = "Anonymous";
                    }
                    if(idNumber == null){
                        idNumber = "None";
                    }
                    tmp.put(patientId,patientName + "%" + patientSex + "%" + caseId + "%" + idNumber);
                }

            }else {
                String patientName = (String)row.value().getString("patientName");
                if(patientName == null){
                    patientName = "Anonymous";
                }
                if(idNumber == null){
                    idNumber = "None";
                }
                tmp.put(patientId,patientName + "%" +  patientSex + "%" + caseId + "%" + idNumber);
            }

        }

        return tmp;
    }


    public HashMap<String,String> getInfoForPatientByIdNumber_APP(String idNumber){
        N1qlQueryResult result = bucket.query(N1qlQuery.parameterized("select a.patientId, a.caseId, b.patientSex, b.patientName, b.idNumber from  `bm_sys` a join  `bm_sys` b on (a.patientId = b.patientId) where a.type = 'record' and b.type = 'info' and b.idNumber = $1 ", JsonArray.from(idNumber)));
        HashMap<String,String> tmp = new HashMap<>();
        for (N1qlQueryRow row : result){
            String patientId = (String)row.value().getString("patientId");
            String caseId = (String)row.value().getString("caseId");
            String patientSex = (String)row.value().getString("patientSex");



            if(tmp.get(patientId) != null){
                String olddate = tmp.get(patientId).split("_")[1];
                String curdate = caseId.split("_")[1];

                int comp = curdate.compareTo(olddate);
                if(comp > 0 || (comp == 0 && caseId.contains("BC")) ){
                    String patientName = (String)row.value().getString("patientName");
                    if(patientName == null){
                        patientName = "Anonymous";
                    }
                    if(idNumber == null){
                        idNumber = "None";
                    }
                    tmp.put(patientId,patientName + "%" + patientSex + "%" + caseId + "%" + idNumber);
                }

            }else {
                String patientName = (String)row.value().getString("patientName");
                if(patientName == null){
                    patientName = "Anonymous";
                }
                if(idNumber == null){
                    idNumber = "None";
                }
                tmp.put(patientId,patientName + "%" +  patientSex + "%" + caseId + "%" + idNumber);
            }

        }

        return tmp;
    }

    public boolean isRecordExisted( String caseId) {
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select caseId from `fetal_sys` where type = 'record' and caseId = $1", JsonArray.from(caseId))
        );
        if (result.allRows().size() > 0)
            return true;
        else
            return false;
    }


    public boolean validateMd5_dcm(String caseId){
//        如果MD5_validation_flag不为true则跳过验证
        if(!constantMap.getMD5_validation_flag().equals("true")) {
            log.info("skip MD5_validation");
            return true;
        }

        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select md5_dcm from `fetal_sys` where type = 'record' and caseId = $1 ", JsonArray.from(caseId))
        );
        try {
            JsonObject obj = result.allRows().get(0).value();
            String old_md5_dcm = obj.getString("md5_dcm");
            String dir = new PathConfig().getPathConfig().getDatapath() +caseId;
            String dicomDirMd5String = MD5.getDicomDirMd5String(dir);
            LOGGER.info("md5_dcm in db is:"+old_md5_dcm+", current md5_dcm is :"+dicomDirMd5String);
            if (dicomDirMd5String.equals(old_md5_dcm))
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

    public ArrayList<Map<String, Object>> getCaseIdForPlugin(String pid){
        N1qlQueryResult result = null;
        while (pid.length() < 10){
            pid = '0'+ pid;
        }

        result = bucket.query(N1qlQuery.parameterized("select date,horb,seriesId, description, caseId,patientId from bm_sys  where type = 'record' and (patientId = $1 or accessionNu = $1) order by date desc,horb;", JsonArray.from(pid)));
        ArrayList<Map<String, Object>> dateToStudy = new ArrayList<>();
        for (N1qlQueryRow row : result) {
            Map<String, Object> caseObject = new HashMap<>();
            String date = (String) row.value().get("date");
            String horb = Double.toString(row.value().getDouble("horb"));
            String caseId = (String) row.value().get("caseId");
            String username;

            caseObject.put("caseId",caseId);
            caseObject.put("date",date);
            caseObject.put("horb",horb);

            N1qlQueryResult draftResult = bucket.query(
                    N1qlQuery.parameterized("select username from `bm_sys` where type = 'draft' and caseId = $1 and username like 'bm_sys%'", JsonArray.from(caseId))
            );
            if (draftResult.allRows().size() == 0){
                username = "origin";
            }else{
                ArrayList<String> usernameArray = new ArrayList<>();
                for (N1qlQueryRow draftRow: draftResult) {
                    usernameArray.add((String) draftRow.value().get("username"));
                }
                if(usernameArray.contains("bm_sys"))
                    username = "bm_sys"; // return default bm_sys
                else{
                    username = usernameArray.get(0);
                }
            }

            caseObject.put("caseId",caseId);
            caseObject.put("username",username);
            caseObject.put("date",date);
            caseObject.put("horb",horb);
            dateToStudy.add(caseObject);
        }
        return dateToStudy;
    }

}
