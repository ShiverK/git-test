package me.sihang.backend.service;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SubsetService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    private Bucket bucket;

    @Autowired
    public SubsetService(Bucket bucket){
        this.bucket = bucket;
    }

    public boolean createSubset(String username, String patientIds, String subsetName){

        int no = getSubsetCountforUser(username);

        String subsetId = "bm_sys#" + username + "_" + no + "@subset";

        String[] patientId_array = patientIds.split("_");
        JsonArray patientIds_jsonarray = JsonArray.create();
        for(String s:patientId_array){
            patientIds_jsonarray.add(s);
        }


        JsonObject subsetObj =  JsonObject.create();
        subsetObj.put("subsetName",subsetName);
        subsetObj.put("username",username);
        subsetObj.put("patientIds",patientIds_jsonarray);
        subsetObj.put("type","subset");


        JsonDocument doc = JsonDocument.create(subsetId, subsetObj);

        try{
            bucket.upsert(doc);
            return true;
        }catch (Exception e){
            return false;
        }


    }

    public boolean updateSubset(String username,String patientIds,String subsetName, String newSubsetName){
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select meta(bm_sys).id as dockey from `bm_sys` where type = 'subset' and username = $1 and subsetName = $2 ", JsonArray.from(username,subsetName))
        );
        String subsetId = result.allRows().get(0).value().getString("dockey");
        String[] patientId_array = patientIds.split("_");
        JsonArray patientIds_jsonarray = JsonArray.create();
        for(String s:patientId_array){
            patientIds_jsonarray.add(s);
        }

        JsonObject subsetObj =  JsonObject.create();
        subsetObj.put("subsetName",newSubsetName);
        subsetObj.put("username",username);
        subsetObj.put("patientIds",patientIds_jsonarray);
        subsetObj.put("type","subset");


        JsonDocument doc = JsonDocument.create(subsetId, subsetObj);
        try{
            bucket.upsert(doc);
            return true;
        }catch (Exception e){
            return false;
        }

    }

    private int getSubsetCountforUser(String username){
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select meta(bm_sys).id from `bm_sys` where type = 'subset' and username = $1 ", JsonArray.from(username))
        );

//        if(result.allRows().size() > 0){
//            int no = (int) result.allRows().get(0).value().get("count");
//            return no;
//        }else{
//            return 0;
//        }
        int maxnum = 0;
        for(N1qlQueryRow row : result){
            String key = row.value().getString("id");
            String s1 = key.split("#")[1];
            String s2 = s1.split("@")[0];
            int num = Integer.valueOf(s2.split("_")[1]);

            if(num >= maxnum){
                maxnum = num + 1;
            }
        }
        return maxnum;



    }

    public ArrayList<String> getSubsetforUser(String username){

        ArrayList<String> res = new ArrayList<>();
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select subsetName from `bm_sys` where type = 'subset' and username = $1", JsonArray.from(username))
        );
        for(N1qlQueryRow row : result){
            res.add(row.value().getString("subsetName"));
        }

        return res;

    }

    public boolean deleteSubset(String username, String subsetName){
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("delete from `bm_sys` where type = 'subset' and username = $1 and subsetName = $2", JsonArray.from(username, subsetName))
        );
        String status = result.status();
        if (status.equals("success"))
            return true;
        else
            return false;
    }

    public JsonArray getPatientIdsfromSubset(String username, String subsetName){
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select patientIds from `bm_sys` where type = 'subset' and username = $1 and subsetName = $2", JsonArray.from(username,subsetName))
        );

        JsonArray patientIds = JsonArray.create();
        if(result.allRows().size() > 0){
            patientIds  = result.allRows().get(0).value().getArray("patientIds");
        }
        return patientIds;

    }

    public boolean isSubSetExisted(String subsetName){
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select subsetName from `bm_sys` where type = 'subset' and subsetName = $1", JsonArray.from(subsetName))
        );

        if(result.allRows().size() > 0){
            return true;
        }else {
            return false;
        }
    }

}
