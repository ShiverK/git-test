package me.sihang.backend.controller;


import com.google.gson.Gson;
import me.sihang.backend.service.CovidService;
import me.sihang.backend.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RequestMapping("/covid")
@RestController
@CrossOrigin
public class CovidController {

    private  final CovidService covidService;



    @Autowired
    public CovidController(CovidService covidService){
        this.covidService = covidService;
    };

    @RequestMapping("/getCovidlist")
    public String getCovidlist(){
        ArrayList<String> covidlist = this.covidService.getCovidList();
        Map<String,Object> retmap = new HashMap<>();

        retmap.put("status","ok");
        retmap.put("covidlist",covidlist);

        return new Gson().toJson(retmap);


    }

    @RequestMapping("/getHist")
    public String getHist(String caseId){
        Map<String,Object> hist = this.covidService.getHist(caseId);

        hist.put("status","ok");

        return new Gson().toJson(hist);

    }

}
