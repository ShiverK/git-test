package me.sihang.backend.controller;


import com.google.gson.Gson;
import me.sihang.backend.domain.ConfigPath;
import me.sihang.backend.service.FileStorageService;
import me.sihang.backend.util.RedisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

@RestController
@RequestMapping("/preprocess")
@CrossOrigin
public class PreprocessController {

    //@Value("${script_path}")
    //private String script_path;
    @Autowired
    private RedisUtils ru;



    private static final Logger logger = LoggerFactory.getLogger(NoduleController.class);
    private final FileStorageService fileStorageService;


    @Autowired
    public PreprocessController(FileStorageService fileStorageService,ConfigPath configPath) {
        this.fileStorageService = fileStorageService;

    }
//
//    @PostMapping("/exec")
//    public String preProcess(String filepath){
//
//        HashMap<String,String> res = new HashMap<>();
//
//        if(fileStorageService.isFileExsit(filepath)){
//            //exec the python script
//            try {
//
//                String command = "python3 " + script_path + "format_original_new.py -s  "+ filepath;
//                System.out.println(command);
//                Process proc = Runtime.getRuntime().exec(command);// 执行py文件
//                res.put("status","ok");
//
//
//            } catch (IOException e) {
//                e.printStackTrace();
//                res.put("status", "failed");
//                res.put("msg", "IO error.");
//            }
//        }else{
//            res.put("status","failed");
//            res.put("msg","Folder doesn't exist.");
//        }
//
//        return new Gson().toJson(res);
//    }

    @PostMapping("/exec")
    public String preProcess(String filepath){

        HashMap<String,String> res = new HashMap<>();

        if(ru.lSet("data_path",filepath)){
            res.put("status","ok");
        }
        else{
            res.put("status", "failed");
        }

        return new Gson().toJson(res);
    }
}
