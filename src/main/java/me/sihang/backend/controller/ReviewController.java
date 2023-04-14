package me.sihang.backend.controller;

import com.google.gson.Gson;
import me.sihang.backend.service.RecordService;
import me.sihang.backend.service.ReviewService;
import me.sihang.backend.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RequestMapping("/review")
@RestController
@CrossOrigin
public class ReviewController {

    private final ReviewService reviewServ;
    private final TokenService tokenServ;

    @Autowired
    public ReviewController(ReviewService reviewServ, TokenService tokenServ) {
        this.reviewServ = reviewServ;
        this.tokenServ = tokenServ;
    }

    @PostMapping(value = "getReviewResults")
    public String getReviewResults(@RequestHeader Map<String, String> headers, String caseId) {
        ArrayList<String> modelUsernames = reviewServ.getReviewResults(caseId);
        Map<String, Object> retMap = new HashMap<>();
        retMap.put("status", "okay");
        retMap.put("dataList", modelUsernames);
        return new Gson().toJson(retMap);
    }

    @PostMapping(value = "isOkayForReview")
    public boolean isOkayForReview(@RequestHeader Map<String, String> headers, String caseId, String username) {
//        String token = headers.get("authorization").split("Bearer ")[1];
//        Map<String, String> tokenMap = tokenServ.validToken(token);
//        if (!tokenMap.get("privilege").equals("2")) {
//            return false;
//        } else {
//            String reviewerUsername = tokenMap.get("username");
//            return false;
//        }
        return false;
    }
}
