package me.sihang.backend.service;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

@Service
public class PatientService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NoduleService.class);
    private Bucket bucket;

    @Autowired
    public PatientService(Bucket bucket) {
        this.bucket = bucket;
    }

    public String getAgeDist() {
        HashMap<String, Object> retMap = new HashMap<>();
        SimpleDateFormat yearFormatter = new SimpleDateFormat("yyyy");
        SimpleDateFormat monthDayFormatter = new SimpleDateFormat("MMdd");
        Date date = new Date();
        int year = Integer.parseInt(yearFormatter.format(date));
        String monthDateString = monthDayFormatter.format(date);
//        int zeroYear = year;
        int fifteenYear = year - 15;
        int thirtyYear = year - 30;
        int fiftyYear = year - 50;
        int sixtyFiveYear = year - 65;
//        String zeroBirth = String.valueOf(zeroYear) + monthDateString;
        String fifteenBirth = String.valueOf(fifteenYear) + monthDateString;
        String thirtyBirth = String.valueOf(thirtyYear) + monthDateString;
        String fiftyBirth = String.valueOf(fiftyYear) + monthDateString;
        String sixtyFiveBirth = String.valueOf(sixtyFiveYear) + monthDateString;
        String n1ql = "select sum(case when patientBirth < '" + sixtyFiveBirth + "' then 1 else 0 end) as r1,\n" +
                "       sum(case when patientBirth > '" + sixtyFiveBirth + "' and patientBirth < '" + fiftyBirth + "' then 1 else 0 end) as r2,\n" +
                "       sum(case when patientBirth > '" + fiftyBirth + "' and patientBirth < '" + thirtyBirth + "' then 1 else 0 end) as r3,\n" +
                "       sum(case when patientBirth > '" + thirtyBirth + "' and patientBirth < '" + fifteenBirth + "' then 1 else 0 end) as r4,\n" +
                "       sum(case when patientBirth > '" + fifteenBirth + "' then 1 else 0 end) as r5\n" +
                "from fetal_sys where type = 'info';";
        N1qlQueryResult result = bucket.query(N1qlQuery.simple(n1ql));
        JsonObject jo = JsonObject.create();
        try {
            jo = result.allRows().get(0).value();
        } catch (Exception e) {
            retMap.put("status", "failed");
            return new Gson().toJson(retMap);
        }

        int ageOver65 = (int) jo.get("r1");
        int age50To64 = (int) jo.get("r2");
        int age30To49 = (int) jo.get("r3");
        int age15To29 = (int) jo.get("r4");
        int age0To14 = (int) jo.get("r5");

        HashMap<String, Object> first = new HashMap<>();
        HashMap<String, Object> second = new HashMap<>();
        HashMap<String, Object> third = new HashMap<>();
        HashMap<String, Object> fourth = new HashMap<>();
        HashMap<String, Object> fifth = new HashMap<>();

        first.put("type", "0-14岁");
        first.put("value", age0To14);

        second.put("type", "15-29岁");
        second.put("value", age15To29);

        third.put("type", "30-49岁");
        third.put("value", age30To49);

        fourth.put("type", "50-64岁");
        fourth.put("value", age50To64);

        fifth.put("type", "65岁以上");
        fifth.put("value", ageOver65);
        ArrayList<HashMap<String, Object>> ary = new ArrayList<>();
        ary.add(first);
        ary.add(second);
        ary.add(third);
        ary.add(fourth);
        ary.add(fifth);

        retMap.put("status", "okay");
        retMap.put("data", ary);
        return new Gson().toJson(retMap);

    }

    public String getPatientSexRatio() {
        N1qlQueryResult resultMale = bucket.query(
                N1qlQuery.simple("select count(patientId) as count from `fetal_sys` where type = 'info' and patientSex = 'M'")
        );
        N1qlQueryResult resultFemale = bucket.query(
                N1qlQuery.simple("select count(patientId) as count from `fetal_sys` where type = 'info' and patientSex = 'F'")
        );
        int maleCount = (int) resultMale.allRows().get(0).value().get("count");
        int femaleCount = (int) resultFemale.allRows().get(0).value().get("count");
        HashMap<String, Integer> retMap = new HashMap<>();
        retMap.put("male", maleCount);
        retMap.put("female", femaleCount);
        return new Gson().toJson(retMap);
    }

    public String MomAge() {
        HashMap<String, Object> retMap = new HashMap<>();
        SimpleDateFormat yearFormatter = new SimpleDateFormat("yyyy");
        SimpleDateFormat monthDayFormatter = new SimpleDateFormat("MMdd");
        Date date = new Date();
        int year = Integer.parseInt(yearFormatter.format(date));
        String monthDateString = monthDayFormatter.format(date);
//        int zeroYear = year;
        int fifteenYear = year - 15;
        int twentyfiveYear = year - 25;
        int thirtyfiveYear = year - 35;
//        int sixtyFiveYear = year - 65;

//        String zeroBirth = String.valueOf(zeroYear) + monthDateString;
        String fifteenBirth = String.valueOf(fifteenYear) + monthDateString;
        String twentyfiveBirth = String.valueOf(twentyfiveYear) + monthDateString;
        String thirtyfiveBirth = String.valueOf(thirtyfiveYear) + monthDateString;
//        String sixtyFiveBirth = String.valueOf(sixtyFiveYear) + monthDateString;
        String n1ql = "select sum(case when patientBirth < '" + thirtyfiveBirth + "' then 1 else 0 end) as r1,\n" +
                " sum(case when patientBirth > '" + thirtyfiveBirth + "' and patientBirth < '" + twentyfiveBirth + "' then 1 else 0 end) as r2,\n" +
                " sum(case when patientBirth > '" + twentyfiveBirth + "' and patientBirth < '" + fifteenBirth + "' then 1 else 0 end) as r3\n" +
                "from fetal_sys where type = 'info';";
        N1qlQueryResult result = bucket.query(N1qlQuery.simple(n1ql));
        System.out.println(result.allRows());

        JsonObject jo = JsonObject.create();
        try {
            jo = result.allRows().get(0).value();
        } catch (Exception e) {
            retMap.put("status", "failed");
            return new Gson().toJson(retMap);
        }

        int ageOver35 = (int) jo.get("r1");
        int age25To35 = (int) jo.get("r2");
        int age18To25 = (int) jo.get("r3");
//        int age15To29 = (int) jo.get("r4");
//        int age0To14 = (int) jo.get("r5");

        HashMap<String, Object> first = new HashMap<>();
        HashMap<String, Object> second = new HashMap<>();
        HashMap<String, Object> third = new HashMap<>();
//        HashMap<String, Object> fourth = new HashMap<>();
//        HashMap<String, Object> fifth = new HashMap<>();

        first.put("name", "18-25岁");
        first.put("value", age18To25);

        second.put("name", "25-35岁");
        second.put("value", age25To35);

        third.put("name", "35岁以上");
        third.put("value", ageOver35);
        ArrayList<HashMap<String, Object>> ary = new ArrayList<>();
        ary.add(first);
        ary.add(second);
        ary.add(third);

        retMap.put("status", "okay");
        retMap.put("data", ary);
        return new Gson().toJson(retMap);

    }




}
