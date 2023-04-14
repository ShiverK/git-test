package me.sihang.backend.service;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class ReviewService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    private Bucket bucket;

    @Autowired
    public ReviewService(Bucket bucket) {
        this.bucket = bucket;
    }

    public ArrayList<String> getReviewResults(String caseId) {
        ArrayList<String> ret = new ArrayList<>();
        N1qlQueryResult result = bucket.query(
                N1qlQuery.parameterized("select reviewerName from `bm_sys` where type = 'review' and caseId = $1 and status = '1'", JsonArray.from(caseId))
        );
        for (N1qlQueryRow row: result) {
            ret.add((String) row.value().get("username"));
        }
        return ret;
    }
}
