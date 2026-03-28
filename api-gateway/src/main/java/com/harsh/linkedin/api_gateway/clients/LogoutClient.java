package com.harsh.linkedin.api_gateway.clients;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "UserService", path = "/users")
public interface LogoutClient {

    @GetMapping("/auth/validate-token")
    public Boolean isTokenBlacklisted(@RequestParam String token);
}
