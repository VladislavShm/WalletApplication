package com.kuehnenageldemo.wallet.controller;

import com.kuehnenageldemo.wallet.config.WebSecurityConfig;
import com.kuehnenageldemo.wallet.service.WalletOperationService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.NumberFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(WebSecurityConfig.API_PREFIX + "/wallet/top-up")
public class TopUpController {
    private final WalletOperationService walletOperationService;

    @PostMapping
    public void topUp(@RequestBody @Valid TopUpRequest topUpRequest) {
        log.debug("Received top-up request: {}", topUpRequest);
        walletOperationService.topUp(topUpRequest.walletId, topUpRequest.amount);
        log.debug("Top-up request has been successfully processed.");
    }

    @Data
    public static class TopUpRequest {
        @NotNull
        private Long walletId;
        @NotNull
        private BigDecimal amount;
    }
}
