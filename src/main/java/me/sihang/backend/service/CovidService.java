package me.sihang.backend.service;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;
import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class CovidService {

    private Bucket bucket;

    @Autowired
    public CovidService(Bucket bucket) {
        this.bucket = bucket;
    }


    public ArrayList<String> getCovidList(){

        ArrayList<String> ret = new ArrayList<>();
        N1qlQueryResult result = bucket.query(
                N1qlQuery.simple("select caseId from `bm_sys` where type = 'covid' and status = '1' ")
        );
        if (result.allRows().size() == 0)
            return null;
        for (N1qlQueryRow row: result) {
            ret.add((String) row.value().get("caseId"));
        }
        return ret;
    }

    public Map<String,Object> getHist(String caseId){

        Map<String,Object> ret = new HashMap<>();
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select covid_hist,lung_hist from `bm_sys` where type = 'covid' and status = '1' and caseId = $1",JsonArray.from(caseId))
        );
        if (result.allRows().size() == 0)
            return null;

        JsonObject r =  result.allRows().get(0).value();
        ret.put("covid_hist",r.get("covid_hist"));
        ret.put("lung_hist",r.get("lung_hist"));

        return ret;


    }


}
