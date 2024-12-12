package com.card.user;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@AllArgsConstructor
@RestController
@RequestMapping("/api")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    @PostMapping("/user")
    Map<String,String> user(@RequestParam("status") String status){
        Map<String,String> user = new HashMap<>();
        if("userFailure".equalsIgnoreCase(status)) {
            user.put("failure", "No user found!---testing");
            log.info("user ::failure::{}", user);
        }else{
            user.put("name","Rana");
            user.put("userId","1000001");
            user.put("email","abc@gmail.com");
            log.info("user ::success::{}",user);
        }
        return user;
    }
}
