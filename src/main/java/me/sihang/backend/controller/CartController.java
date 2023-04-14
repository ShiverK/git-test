package me.sihang.backend.controller;

import com.google.gson.Gson;
import me.sihang.backend.domain.Token;
import me.sihang.backend.service.CartService;
import me.sihang.backend.service.FileStorageService;
import me.sihang.backend.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@RestController
@RequestMapping("/cart")
@CrossOrigin
public class CartController {
    private final CartService cartServ;
    private final TokenService tokenServ;
    private final FileStorageService fileStorageService;

    @Autowired
    public CartController(CartService cartServ, TokenService tokenServ, FileStorageService fileStorageService) {
        this.cartServ = cartServ;
        this.tokenServ = tokenServ;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(value = "saveCart")
    public String saveCart(@RequestHeader Map<String, String> headers, String cart) throws IOException {
        Map<String, String> retMap = new HashMap<>();
        String token = headers.get("authorization").split("Bearer ")[1];
        Map<String, String> tokenMap = tokenServ.validToken(token);
        if (tokenMap.get("status").equals("okay")) {
            String username = tokenMap.get("username");
            boolean isSaved = cartServ.saveList(username, cart);
            if (isSaved) {
                retMap.put("status", "saved");
            } else {
                retMap.put("status", "errorsaving");
            }
        } else if (tokenMap.get("status").equals("expired")) {
            retMap.put("status", "expired");
        } else {
            retMap.put("status", "invalid");
        }
        return new Gson().toJson(retMap);
    }

    @GetMapping(value = "getCart")
    public String getCart(@RequestHeader Map<String, String> headers) {
        Map<String, Object> retMap = new HashMap<>();
        String token = headers.get("authorization").split("Bearer ")[1];
        Map<String, String> tokenMap = tokenServ.validToken(token);
        if (tokenMap.get("status").equals("okay")) {
            String username = tokenMap.get("username");

            ArrayList<Map<String,Object>> cart = cartServ.getList(username);
//            System.out.println(cart);
            if (cart == null)
                retMap.put("status", "nothing");
            else
                retMap.put("status", "okay");
                retMap.put("cart", cart);
        } else if (tokenMap.get("status").equals("expired")){
            retMap.put("status", "expired");
        } else {
            retMap.put("status", "failed");
        }

        return new Gson().toJson(retMap);
    }

    @GetMapping(value = "downloadCart")
    public String downloadCart(@RequestHeader Map<String, String> headers) throws IOException {
        Map<String, String> retMap = new HashMap<>();
        String token = headers.get("authorization").split("Bearer ")[1];
        Map<String, String> tokenMap = tokenServ.validToken(token);
        if (tokenMap.get("status").equals("okay")) {
            String username = tokenMap.get("username");
            String zipFileName = cartServ.createZip(username);
            if (zipFileName == null)
            {
                return null;
            }
            cartServ.emptyList(username);
            return username + ".zip";

        } else {
            return null;
        }
    }

}
