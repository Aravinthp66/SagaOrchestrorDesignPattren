package com.MicroserviceProject.Payment.PaymentController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PaymentController {
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
        @PostMapping("/payment")
        Map<String,String> payment(@RequestBody Map<String, String> request){
            Map<String,String> payment = new HashMap<>();
            String status = request.get("status");
            String rollBack = request.getOrDefault("rollBack",null);
            String productReserved = request.get("productReserved");

            if(status!=null &&  "paymentFailure".equalsIgnoreCase(status)&&
                    rollBack ==null) {
                payment.put("failure", "Payment failure---testing");
                payment.put("code","00005"); // payment failed
                log.info("payment ::failure::{}", payment);
                return payment;
            }
            if(rollBack != null && rollBack.equals("true")){
                payment.put("failure", "Payment revert successful!");
                payment.put("code","00004"); // roll back
                log.info("Payment revert :*****success*****");
            }else if(productReserved !=null && "true".equals(productReserved)){
                payment.put("paymentId","PID1000001");
                payment.put("success", "success");
                payment.put("code","00000"); // success
                log.info("payment ::success::{}",payment);
            }else{
                payment.put("failure", "Payment failure");
                payment.put("code","00005"); // payment failed
                log.info("payment ::failure::{}",payment);
            }
            return payment;
        }
    }