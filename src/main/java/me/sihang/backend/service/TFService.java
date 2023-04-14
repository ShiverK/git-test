package me.sihang.backend.service;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;
import com.google.gson.Gson;
import me.sihang.backend.util.FileList;
import net.minidev.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class TFService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    private Bucket bucket;

    @Value("${datapath}")
    private String datapath;

    @Autowired
    public TFService(Bucket bucket) {
        this.bucket = bucket;
    }

    public String getDatapath() {
        return this.datapath;
    }

    public String getCaseIdByThreeDomains(String patientID, String patientDate, String seriesID) {
        N1qlQueryResult result = bucket.query(N1qlQuery.parameterized(
                "select caseId from `bm_sys` where type = 'record' and patientId = $1 and date = $2 and seriesId = $3", JsonArray.from(patientID, patientDate, seriesID)
        ));
        String caseId = (String) result.allRows().get(0).value().get("caseId");
        return caseId;
    }

    public String getPatientList(String indexbegin, String indexend, String patientID, String patientDate) {
        int limit = Integer.parseInt(indexend) - Integer.parseInt(indexbegin);
        int offset = Integer.parseInt(indexbegin);
        N1qlQueryResult result;
        if (!patientID.equals("undef")) {
            // query with patientId
            result = bucket.query(N1qlQuery.parameterized(
                    "select patientId, patientName from `bm_sys` where type='info' and patientId = $1 limit $2 offset $3", JsonArray.from(patientID, limit, offset)
            ));
        } else if (!patientDate.equals("undef")) {
            // query with patientDate
            result = bucket.query(N1qlQuery.parameterized(
                    "select patientId, patientName from `bm_sys` where type='info' and date = $1 limit $2 offset $3", JsonArray.from(patientDate, limit, offset)
            ));
        } else {
             result = bucket.query(N1qlQuery.parameterized(
                    "select patientId, patientName from `bm_sys` where type='info' limit $1 offset $2", JsonArray.from(limit, offset)
            ));
        }
        ArrayList<HashMap<String, Object>> theData = new ArrayList<>();
        for (N1qlQueryRow row : result) {
            String patientId = (String) row.value().get("patientId");
            String patientName = (String) row.value().get("patientName");
            N1qlQueryResult subResult = bucket.query(N1qlQuery.parameterized(
                    "select caseId, date, seriesId from `bm_sys` where type='record' and patientId = $1", JsonArray.from(patientId)
            ));
            HashMap<String, ArrayList<HashMap<String, String>>> dateMap = new HashMap<>();
            for (N1qlQueryRow subRow: subResult) {
                String date = (String) subRow.value().get("date");
                String seriesId = (String) subRow.value().get("seriesId");
                String bh = ((String) subRow.value().get("caseId")).split("_")[2];
                HashMap<String, String> datelist = new HashMap<>();
                datelist.put("seriesID", seriesId);
                datelist.put("bh", bh);
                datelist.put("status", "finished");

                if (!dateMap.containsKey(date)) {
                    ArrayList<HashMap<String, String>> lst = new ArrayList<>();
                    lst.add(datelist);
                    dateMap.put(date, lst);
                } else {
                    ArrayList<HashMap<String, String>> theLst = dateMap.get(date);
                    theLst.add(datelist);
                    dateMap.replace(date, theLst);
                }
            }

            Iterator it = dateMap.entrySet().iterator();
            ArrayList<HashMap<String, Object>> finalLst = new ArrayList<>();
            while(it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                HashMap<String, Object> finalObj = new HashMap<>();
                String date = (String) pair.getKey();
                ArrayList<HashMap<String, String>> value = (ArrayList<HashMap<String, String>>) pair.getValue();
                finalObj.put("date", date);
                finalObj.put("datelist", value);
                finalLst.add(finalObj);
            }
            HashMap<String, Object> ini = new HashMap<>();
            ini.put("ID", patientId);
            ini.put("name", patientName);
            ini.put("info", finalLst);
            theData.add(ini);
        }
        HashMap<String, Object> ret = new HashMap<>();
        ret.put("y", "success");
        ret.put("data", theData);
        return new Gson().toJson(ret);
    }

    public String getRectForClient(String caseId) throws IOException {
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select spacing, rects from `bm_sys` where type = 'draft' and status = '1' and username = 'bm_sys' and caseId = $1", JsonArray.from(caseId)
                ));
        if (result.allRows().size() == 0)
            return null;
        JsonArray rects;
        double spacing = 0.0;
        try {
            rects = (JsonArray) result.allRows().get(0).value().get("rects");
            spacing = (double) result.allRows().get(0).value().get("spacing");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        ArrayList<Object> yList = new ArrayList<>();
        ArrayList<Object> noduleList = new ArrayList<>();
        for (int i = 0; i < rects.size(); i ++) {
            String noduleId = (String) rects.get(i);
            JsonObject jo = bucket.get(noduleId).content();
            HashMap<String, Object> noduleObj = new HashMap<>();

            int lobulation =  jo.getInt("lobulation");
            int texture = jo.getInt("texture");
            int spiculation = jo.getInt("spiculation");
            int calcification = jo.getInt("calcification");
            int malignancy = jo.getInt("malignancy");

            double lobProb = jo.getDouble("lobProb");
            double texProb = jo.getDouble("texProb");
            double spiProb = jo.getDouble("spiProb");
            double calProb = jo.getDouble("calProb");
            double malProb = jo.getDouble("malProb");

            ArrayList<Double> coordinate = new ArrayList<>();

            Set<String> imgList = FileList.fromDirectory(datapath + caseId);
            int slicesTotal = FileList.countDCM(datapath + caseId);
            int coords1 = slicesTotal - (int) jo.get("slice_idx") - 1;

//            double coords2 = ((double)jo.get("y1") + (double)jo.get("y2")) / 2;
//            double coords3 = ((double)jo.get("x1") + (double)jo.get("x2")) / 2;
//            double coords4 = ((double)jo.get("y2") - (double)jo.get("y1")) / 2;

            double coords2 = (Double.valueOf(jo.get("y1").toString()) + Double.valueOf(jo.get("y2").toString())) / 2;
            double coords3 = (Double.valueOf(jo.get("x1").toString()) + Double.valueOf(jo.get("x2").toString())) / 2;
            double coords4 = (Double.valueOf(jo.get("y2").toString()) - Double.valueOf(jo.get("y1").toString())) / 2;

            coordinate.add(coords3 / 512);
            coordinate.add(coords2 / 512);
            coordinate.add((double) coords1 / slicesTotal);

            double probability = (double) jo.get("probability");
            noduleObj.put("lobulation", new Double[] {(double) lobulation, lobProb});
            noduleObj.put("texture", new Double[] {(double) texture, texProb});
            noduleObj.put("spiculation", new Double[] {(double) spiculation, spiProb});
            noduleObj.put("calcification", new Double[] {(double) calcification, calProb});
            noduleObj.put("malignancy", new Double[] {(double) malignancy, malProb});
            noduleObj.put("probability", probability);
            noduleObj.put("coordinate", coordinate);
            noduleObj.put("semi_y_axis", 0);
            noduleObj.put("semi_x_axis", 0);
            noduleObj.put("modelorhand", 0);
            noduleObj.put("radius", coords4 * spacing);
            noduleObj.put("nodule_index", i);
            noduleObj.put("hu_min", jo.get("huMin"));
            noduleObj.put("hu_max", jo.get("huMax"));
            noduleObj.put("hu_mean", jo.get("huMean"));
            noduleObj.put("volume", jo.get("volume"));
            noduleList.add(noduleObj);
        }
        HashMap<String, Object> tmp = new HashMap<>();
        tmp.put("nodules", noduleList);
        tmp.put("index", 0);
        HashMap<String, Object> ret = new HashMap<>();
        yList.add(tmp);
        ret.put("y", yList);
        return new Gson().toJson(ret);
    }
}
