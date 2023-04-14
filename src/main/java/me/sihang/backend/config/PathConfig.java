package me.sihang.backend.config;

import com.google.gson.JsonObject;
import me.sihang.backend.domain.ConfigPath;
import me.sihang.backend.util.JsonReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Map;

@Configuration
public class PathConfig {



    public @Bean
    ConfigPath getPathConfig(){
        JsonReader jsonReader = new JsonReader();
        Map config = jsonReader.readJsonFile();

        String server_url = config.get("server_url").toString();
        String datapath;
        try{
            ArrayList<String> dataPathArray = (ArrayList<String>)config.get("datapath");
            datapath = dataPathArray.get(0);
        }catch (Exception e){
            datapath = config.get("datapath").toString();
        }

        return new ConfigPath(server_url, datapath);

    }
}
