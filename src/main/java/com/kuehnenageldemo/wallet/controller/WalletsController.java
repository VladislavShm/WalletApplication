package com.kuehnenageldemo.wallet.controller;

import com.kuehnenageldemo.wallet.config.WebSecurityConfig;
import com.kuehnenageldemo.wallet.repository.wallet.WalletRepository;
import com.kuehnenageldemo.wallet.service.UserService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(WebSecurityConfig.API_PREFIX + "/wallets")
public class WalletsController {
    private final WalletRepository walletRepository;

    @GetMapping
    public List<WalletDto> getWallets() {
        return walletRepository.queryWalletsByOwnerId(UserService.getCurrentUser().getId()).stream()
                .map(a -> new WalletDto(a.getId(), a.getCode(), a.getBalance()))
                .collect(Collectors.toList());
    }

    @Getter
    @AllArgsConstructor
    public static class WalletDto {
        private final Long id;
        private final String code;
        private final BigDecimal balance;
    }
}
