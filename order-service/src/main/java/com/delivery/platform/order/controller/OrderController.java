package com.delivery.platform.order.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    @GetMapping("/api/v1/orders/health")
    public String health() {
        return "Order Service is running";
    }
}