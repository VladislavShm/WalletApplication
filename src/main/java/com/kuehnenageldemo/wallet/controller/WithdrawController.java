package com.kuehnenageldemo.wallet.controller;

import com.kuehnenageldemo.wallet.config.WebSecurityConfig;
import com.kuehnenageldemo.wallet.service.WalletOperationService;
import lombok.Data;
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
@RequestMapping(WebSecurityConfig.API_PREFIX + "/wallet/withdraw")
public class WithdrawController {
    private final WalletOperationService walletOperationService;

    @PostMapping
    public void withdraw(@RequestBody @Valid WithdrawRequest withdrawRequest) {
        log.debug("Received withdraw request: {}", withdrawRequest);
        walletOperationService.withdraw(withdrawRequest.walletId, withdrawRequest.amount);
        log.debug("Withdraw request has been successfully processed.");
    }

    @Data
    public static class WithdrawRequest {
        @NotNull
        private Long walletId;
        @NotNull
        @NumberFormat(style = NumberFormat.Style.CURRENCY)
        private BigDecimal amount;
    }
}
