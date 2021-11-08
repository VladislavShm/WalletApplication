package com.kuehnenageldemo.wallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuehnenageldemo.wallet.repository.UserRepository;
import com.kuehnenageldemo.wallet.repository.wallet.WalletRepository;
import com.kuehnenageldemo.wallet.service.WalletOperationService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnableWebMvc
@AutoConfigureMockMvc
@SpringBootTest(classes = {TransferController.class}, properties = {"spring.liquibase.enabled=false"})
@EnableAutoConfiguration(exclude= SecurityAutoConfiguration.class)
public class TransferControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;
    @MockBean
    private WalletRepository walletRepository;
    @MockBean
    private WalletOperationService walletOperationService;

    @Test
    @SneakyThrows
    public void okTest() {
        TransferController.TransferRequest transferRequest = new TransferController.TransferRequest();
        transferRequest.setToWalletId(1L);
        transferRequest.setFromWalletId(4L);
        transferRequest.setAmount(new BigDecimal("123.45"));

        mockMvc.perform(
                MockMvcRequestBuilders.post("/api/wallet/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(transferRequest))
        )
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        Mockito.verify(
                walletOperationService,
                Mockito.times(1)
        ).transfer(anyLong(), anyLong(), any(BigDecimal.class));
    }

    @Test
    @SneakyThrows
    public void emptyFromWalletIdTest() {
        invalidBodyTest("{\"amount\":123.45, \"toWalletId\":4}");
    }

    @Test
    @SneakyThrows
    public void emptyToWalletIdTest() {
        invalidBodyTest("{\"amount\":123.45, \"fromWalletId\":1}");
    }

    @Test
    @SneakyThrows
    public void emptyAmountTest() {
        invalidBodyTest("{\"fromWalletId\":1, \"toWalletId\":4}");
    }

    @Test
    @SneakyThrows
    public void emptyBodyTest() {
        invalidBodyTest("");
    }

    private void invalidBodyTest(String request) throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.post("/api/wallet/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().isBadRequest())
                .andExpect(content().string(""));

        Mockito.verify(
                walletOperationService,
                Mockito.times(0)
        ).transfer(anyLong(), anyLong(), any(BigDecimal.class));
    }
}
