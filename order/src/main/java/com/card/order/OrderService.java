package com.card.order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("ALL")
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private final RestTemplate restTemplate;

    public OrderService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, String> order(String status) {
        Map<String, String> response = new HashMap<>();
        Map<String, String> request = new HashMap<>();
        request.put("status",status); // for test with manually set failure cases  userFailure, productReserveFailure, paymentFailure, productConfirmFailure

        log.info("Order process started: status::{}", status);

        // Get User Details
        Map<String, String> user = getUserDetailsRequest(request);
        if (user == null || user.getOrDefault("userId", "").isEmpty()) {
            response.put("failure", "No user found!");
            response.put("code", "00001");
            return response;
        }
        log.info("User fetch successfully and product reserve request initiated: {}", response);
        request.put("userId", user.get("userId"));

        // Reserve Product
        response = reserveProductRequest(request);
        if (response == null || !"true".equals(response.get("productReserved"))) {
            log.info("Product reservation failed: {}", response);
            sendNotificationRequest(response);
            return  response;
        }
        log.info("Product reserved successfully and pamument request initiate started: {}", response);

        request.put("productReserved",response.get("productReserved"));
        // Process Payment
        response = processPayment(request);
        if (response != null && "00000".equals(response.get("code"))) {
            sendNotificationRequest(response);
            return response;
        } else {
            log.info("Payment processing failed: {}", response);
            return response;
        }
    }

    private Map<String, String> processPayment( Map<String, String> request) {
        log.info("Processing payment started: request={}", request);
        Map<String, String> response = new HashMap<>();
        response = paymentRequest(request);
        if (response != null && "00000".equals(response.get("code"))) {
            log.info("Payment successful: {}", response);
            request.put("paymentId", response.get("paymentId"));
            response.clear();
            response = orderConfirm(request);
            if (response != null && !"00000".equals(response.get("code"))) {
                log.info("Order confirmation failed: {}", response);
                request.put("rollBack", "true");  // for Compensation
                reserveProductRequest(request); //--> revert product reserve Compensation call
                paymentRequest(request);        //--> revert payment Compensation call
                response.put("failure", "Order not successful, please try again!");
            }else{
                log.info("Order confirmed successfully: {}", response);
            }
        } else {
            log.info("Payment failed: {}", response);
            request.put("rollBack", "true"); // for Compensation
            reserveProductRequest(request); //--> revert product reserve Compensation call
        }
        sendNotificationRequest(response);
        return response;
    }

    private Map<String, String> getUserDetailsRequest(Map<String,String> request) {
        try {
            ResponseEntity<HashMap> response = restTemplate.postForEntity(
                    "http://localhost:8081/api/user",
                    request,
                    HashMap.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error occurred while calling user details API: {}", e.getMessage(), e);
            return null;
        }
    }

    private Map<String, String> reserveProductRequest( Map<String, String> paymentRequest) {
        try {
            ResponseEntity<HashMap> response = restTemplate.postForEntity(
                    "http://localhost:8082/api/product",
                    paymentRequest,
                    HashMap.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error occurred while calling product reservation API: {}", e.getMessage(), e);
            return null;
        }
    }

    private Map<String, String> paymentRequest( Map<String, String> paymentRequest) {
        try {
            ResponseEntity<HashMap> response = restTemplate.postForEntity(
                    "http://localhost:8083/api/payment",
                    paymentRequest,
                    HashMap.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error occurred while calling payment API: {}", e.getMessage(), e);
            return null;
        }
    }

    private Map<String, String> orderConfirm(Map<String, String> orderCon) {
        try {
            ResponseEntity<HashMap> response = restTemplate.postForEntity(
                    "http://localhost:8082/api/product-confirm",
                    orderCon,
                    HashMap.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error occurred while calling order confirm API: {}", e.getMessage(), e);
            return null;
        }
    }

    private void sendNotificationRequest(Map<String, String> message) {
        try {
            restTemplate.postForEntity("http://localhost:8085/api/notification", message, HashMap.class);
        } catch (Exception e) {
            log.error("Error occurred while calling notification API: {}", e.getMessage(), e);
        }
    }
}





