package com.MicroserviceProduct.Product.ProductController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProductController {
    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    @PostMapping("/product")
    Map<String, String> product(@RequestParam("status") String status, @RequestParam("userId") String userId,
                                @RequestParam("rollBack") String rollBack) {
        Map<String, String> product = new HashMap<>();
        if ("productFailure".equalsIgnoreCase(status) && !rollBack.equals("true")) {
            product.put("failure", "Out of stack--testing!");
            product.put("code", "00003");
            log.info("get-product ::failure::{}", product);
            return product;
        }
        if (rollBack != null && rollBack.equals("true")) {
            product.put("failure", "Product reserved removed successful!");
            product.put("code", "00004"); // roll back
            log.info("Product rserve revert :*****success*****");
        } else if (userId != null) {
            product.put("name", "product_1");
            product.put("productId", "33333");
            product.put("count", "4");
            product.put("productReserved", "true");
            product.put("success", "success");
            product.put("code", "00000");
            log.info("Product reserved ::success::{}", product);
        } else {
            product.put("failure", "product reserve failed! or no product found");
            product.put("code", "00003");
            log.info("Product reserved :: failure ::{}", product);
        }
        return product;
    }


    @PostMapping("/product-confirm")
    Map<String, String> productConfirm(@RequestBody Map<String, String> paymentDetails) {
        Map<String, String> orderConfirm = new HashMap<>();
        if ("productConfirmFailure".equalsIgnoreCase(paymentDetails.get("status"))) {
            orderConfirm.put("failure", "product-confirm failed---testing!");
            orderConfirm.put("code", "00006");
            log.info("productConfirm ::failure::{}", orderConfirm);
            return orderConfirm;
        }
        if (paymentDetails != null && !paymentDetails.isEmpty()) {
            orderConfirm.put("name", "product_1");
            orderConfirm.put("productId", "33333");
            orderConfirm.put("count", "4");
            orderConfirm.put("productConfirmed", "true");
            orderConfirm.put("success", "success");
            orderConfirm.put("code", "00000");
            log.info("productConfirm ::success::{}", orderConfirm);
        } else {
            orderConfirm.put("failure", "product-confirm failed");
            orderConfirm.put("code", "00006");
        }
        return orderConfirm;
    }

}
