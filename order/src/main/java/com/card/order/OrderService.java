package com.card.order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("ALL")
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final WebClient webClient;

    public OrderService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public Mono<Map<String, String>> order(String status, String rollBack) {
        Map<String, String> response = new HashMap<>();
        log.info("Order process started: status={}, rollBack={}", status, rollBack);
        // TODO : getUser ******************************************************************
        return getUserDetailsRequest(status)
                .flatMap(user -> {
                    String userId = user.getOrDefault("userId", "").toString();
                    if (userId.isEmpty()) {
                        response.put("failure", "No user found!");
                        response.put("code", "00001");
                        return Mono.just(response);
                    }
                    // TODO : product reserve **********************************************
                    return reserveProductRequest(status, rollBack, userId)
                            .flatMap(product -> {
                                log.info("getUserDetailsRequest user ::{}", user);
                                if ("true".equals(product.get("productReserved"))) {
                                    log.info("Product reserved :: success ::{}", product);
                                    // TODO : payment **************************************
                                    return processPayment(status, product, rollBack, userId)
                                            .flatMap(notification -> sendNotificationRequest((Map<String, String>) notification)
                                                    .thenReturn(notification));
                                } else {
                                    log.info("Product reserved :: failure ::{}", product);
                                    // TODO : failure notification  ********************************
                                    sendNotificationRequest((Map<String, String>) product);
                                    return Mono.just(product);
                                }
                            });
                })
                .onErrorResume(e -> {
                    log.error("Error occurred during order processing: {}", e);
                    response.put("failure", "Error in processing");
                    return Mono.just(response);
                });
    }

    private Mono<Map<String, String>> processPayment(String status, Map<String, String> product, String rollBack, String userId) {
        Map<String, String> notification = new HashMap<>();
        log.info("Processing payment started  :: status ::{}, product ::{}, rollBack ::{}, userId ::{}", status, product, rollBack, userId);
        // TODO : payment **********************************************************************
        return paymentRequest(status, product.get("productReserved"), rollBack)
                .flatMap(paymentStatus -> {
                    var paymentResponse = (Map<String, String>)paymentStatus;
                    if ("00000".equals(paymentResponse.get("code"))) {
                        log.info("payment :: success ::{}",paymentResponse);
                        paymentResponse.put("status",status);
                        // TODO : order confirm *************************************************
                        orderConfirm(paymentStatus).flatMap(confirm->{
                            var confirmResponse = (Map<String, String>)confirm;
                            if("00000".equals(confirmResponse.get("code"))){
                                notification.put("paymentId", paymentStatus.getOrDefault("paymentId", "").toString());
                                notification.put("success", "Order successful, thanks for choosing us. You will receive shortly.");
                                notification.put("code", paymentStatus.getOrDefault("code", "").toString());
                                log.info("Order confirm success :: confirm ::{}", confirm);
                            }else{
                                log.info("Order confirm failure :: confirm={}", confirm);
                                // TODO : reserve product revert ***********************************
                                reserveProductRequest(status, userId,"true").subscribe();
                                log.info("Product reserve revert :*****success*****");
                                // TODO : payment revert *******************************************
                                paymentRequest(status, userId,"true").subscribe();
                                log.info("Payment revert :*****success*****");
                                notification.put("failure", "Order not successful, please try again!");
                            }
                            log.info("Processing payment Ended  :: paymentStatus ::{}", notification);
                            // TODO : notification *************************************************
                            sendNotificationRequest((Map<String, String>) notification).subscribe();
                            return Mono.just(notification);
                        }).subscribe();
                        return Mono.just(notification);
                    } else {
                        log.info("payment :: failure ::{}",paymentResponse);
                        log.info("Product resevere revert started::");
                        // TODO : reserve product revert *******************************************
                        return reserveProductRequest(status, userId,"true")
                                .then(Mono.fromCallable(() -> {
                                    notification.put("failure", "Order not successful, please try again!");
                                    return notification;
                                }));
                    }
                }).thenReturn(notification);
    }
    // TODO : get user *******************************************************************************
    public Mono<HashMap> getUserDetailsRequest(String status) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.scheme("http")
                        .host("localhost")
                        .port(8081)
                        .path("/api/user")
                        .queryParam("status", status)
                        .build())
                .retrieve()
                .bodyToMono(HashMap.class)
                .onErrorResume(e -> {
                    log.error("Error occurred while calling user details API: {}", e.getMessage(), e);
                    return Mono.empty();
                });
    }
    // TODO : reserve product ***********************************************************************
    public Mono<HashMap> reserveProductRequest(String status,String userId, String rollBack) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.scheme("http")
                        .host("localhost")
                        .port(8082)
                        .path("/api/product")
                        .queryParam("status", status)
                        .queryParam("userId", userId)
                        .queryParam("rollBack", rollBack)
                        .build())
                .retrieve()
                .bodyToMono(HashMap.class)
                .onErrorResume(e -> {
                    log.error("Error occurred while calling product reservation API: {}", e.getMessage(), e);
                    return Mono.empty();
                });
    }
    // TODO : payment ******************************************************************************
    public Mono<HashMap> paymentRequest(String status, String productReserved, String rollBack) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.scheme("http")
                        .host("localhost")
                        .port(8083)
                        .path("/api/payment")
                        .queryParam("status", status)
                        .queryParam("productReserved", productReserved)
                        .queryParam("rollBack", rollBack)
                        .build())
                .retrieve()
                .bodyToMono(HashMap.class)
                .onErrorResume(e -> {
                    log.error("Error occurred while calling payment API: {}", e.getMessage(), e);
                    return Mono.empty();
                });
    }
    // TODO : order confirm ***********************************************************************
    public Mono<HashMap> orderConfirm(Map<String, String> orderCon) {
        log.info("orderConfirm started :: {}",orderCon);
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.scheme("http")
                        .host("localhost")
                        .port(8082)
                        .path("/api/product-confirm")
                        .build())
                .bodyValue(orderCon)
                .retrieve()
                .bodyToMono(HashMap.class)
                .onErrorResume(e -> {
                    log.error("Error occurred while calling notification API: {}", e.getMessage(), e);
                    return Mono.empty();
                });
    }

    // TODO : notification ***********************************************************************
    public Mono<HashMap> sendNotificationRequest(Map<String, String> message) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.scheme("http")
                        .host("localhost")
                        .port(8085)
                        .path("/api/notification")
                        .build())
                .bodyValue(message)
                .retrieve()
                .bodyToMono(HashMap.class)
                .onErrorResume(e -> {
                    log.error("Error occurred while calling notification API: {}", e.getMessage(), e);
                    return Mono.empty();
                });
    }
}






