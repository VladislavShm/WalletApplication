package com.kuehnenageldemo.wallet.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kuehnenageldemo.wallet.config.WebSecurityConfig;
import com.kuehnenageldemo.wallet.repository.UserRepository;
import com.kuehnenageldemo.wallet.repository.wallet.WalletRepository;
import com.kuehnenageldemo.wallet.service.WalletOperationService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.NumberFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(WebSecurityConfig.API_PREFIX + "/wallet/transfer")
public class TransferController {
    private final WalletOperationService walletOperationService;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    @GetMapping("/users")
    public List<UserDto> getUsers() {
        return userRepository.findAll().stream()
                .map(u -> new UserDto(u.getId(), u.getUsername()))
                .collect(Collectors.toList());
    }

    @Getter
    @AllArgsConstructor
    public static class UserDto {
        private final Long id;
        private final String username;
    }

    @GetMapping("/wallets")
    public List<WalletDto> getWallets(@RequestParam("userId") Long userId, @RequestParam("excludeWallet") Long excludeWallet) {
        return walletRepository.queryWalletsByOwnerIdAndIdIsNot(userId, excludeWallet)
                .stream().map(a -> new WalletDto(a.getId(), a.getCode()))
                .collect(Collectors.toList());
    }

    @Data
    @AllArgsConstructor
    public static class WalletDto {
        private final Long id;
        private final String code;
    }

    @PostMapping
    public void transfer(@Valid @RequestBody TransferRequest transferRequest) {
        log.debug("Received transfer request: {}", transferRequest);
        walletOperationService.transfer(transferRequest.fromWalletId, transferRequest.toWalletId, transferRequest.amount);
        log.debug("Transfer request has been successfully processed.");
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TransferRequest {
        @NotNull
        private Long fromWalletId;
        @NotNull
        private Long toWalletId;
        @NotNull
        @NumberFormat(style = NumberFormat.Style.CURRENCY)
        private BigDecimal amount;
    }
}
