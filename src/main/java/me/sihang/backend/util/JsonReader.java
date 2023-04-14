package me.sihang.backend.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;

public class JsonReader {
    public Map readJsonFile() {
        String jsonStr = "";
        try {
//            File jsonFile = new File("C:\\Users\\zz\\Desktop\\javawork\\fetal_backend(1)\\fetal_backend\\config.json");
            File jsonFile = new File("/root/config.json");
            FileReader fileReader = new FileReader(jsonFile);

            Map object =  new Gson().fromJson(fileReader, Map.class);
            return  object;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
