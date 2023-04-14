package me.sihang.backend.service;


import me.sihang.backend.util.RedisUtils;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class TokenService {

    private Map<String, String> retMap = new HashMap<>();

    @Autowired
    private RedisUtils ru;

    public boolean removeToken(String token) {
        try {
            ru.hdel("token", token);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean saveToken(String token, String tokenValue) {
        try {
            ru.hset("token", token, tokenValue);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    public Map<String, String> validToken(String token) {
        String value = (String) ru.hget("token", token);
        if (value == null) {
            retMap.put("status", "failed");
            return retMap;
        }

        long timestamp = (long) (new Date().getTime() / 1000);

        JSONParser parser = new JSONParser();
        try {
            JSONObject json = (JSONObject) parser.parse(value);
            String username = (String) json.get("username");
            String privilege = (String) json.get("privilege");
            //long expires = Long.parseLong((String) json.get("expires"));
            //if (timestamp > expires) {
//                jedis.hdel("token", token);
             //   ru.hdel("token", token);
//                redisTemplate.opsForHash().delete("token", token);
            //    retMap.put("status", "expired");
            //    return retMap;
            //}
            retMap.put("status", "okay");
            retMap.put("username", username);
            retMap.put("privilege", privilege);
            return retMap;
        } catch (ParseException e) {
            e.printStackTrace();
            retMap.put("status", "failed");
            return retMap;
        }
    }

}
