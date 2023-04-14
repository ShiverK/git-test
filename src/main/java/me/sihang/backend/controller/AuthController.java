package me.sihang.backend.controller;



import com.google.gson.Gson;
import me.sihang.backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/auth")
@CrossOrigin
public class AuthController {

    private final AuthService authService;

    @Autowired
    AuthController(AuthService authService){
        this.authService = authService;
    }

    @PostMapping("insertAuth")
    public String insertAuth(String authName){
        return new Gson().toJson(this.authService.insertAuth(authName));
    }

    @PostMapping("delAuth")
    public String delAuth(String authName){
        return new Gson().toJson(this.authService.delAuth(authName));
    }

    @GetMapping("getAllAuth")
    public String getAllAuth(){
        return new Gson().toJson(this.authService.getAllAuth());
    }

    @PostMapping("getAuthsForRoles")
    public String getAuthsForRole(String roles){
        String[] roleLst = roles.split("#");
        return new Gson().toJson(this.authService.getAuthsForRole(roleLst));
    }
}
