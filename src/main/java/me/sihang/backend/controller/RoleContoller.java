package me.sihang.backend.controller;


import com.google.gson.Gson;
import me.sihang.backend.service.AuthService;
import me.sihang.backend.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;


@RestController
@RequestMapping("/role")
@CrossOrigin
public class RoleContoller {

    private final RoleService roleService;

    @Autowired
    RoleContoller(RoleService roleService){
        this.roleService = roleService;
    }

    @PostMapping("insertRole")
    public String insertAuth(String roleName, String auths){
        String[] authlst = auths.split("#");

        return new Gson().toJson(this.roleService.upsertRole(roleName,authlst));
    }

    @PostMapping("delRole")
    public String delRole(String roleName){
        return new Gson().toJson(this.roleService.delRole(roleName));
    }

    @GetMapping("getAllRole")
    public String getAllRole(){
        return new Gson().toJson(this.roleService.getAllRole());
    }
    @PostMapping("getAuthsByRole")
    public String getAuthsByRole(String roleName){
        return new Gson().toJson(this.roleService.getAuthsByRole(roleName));
    }
}
