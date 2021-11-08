package com.kuehnenageldemo.wallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
@SpringBootTest(classes = {WithdrawController.class}, properties = {"spring.liquibase.enabled=false"})
@EnableAutoConfiguration(exclude= SecurityAutoConfiguration.class)
public class WithdrawControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WalletOperationService walletOperationService;

    @Test
    @SneakyThrows
    public void okTest() {
        WithdrawController.WithdrawRequest topUpRequest = new WithdrawController.WithdrawRequest();
        topUpRequest.setWalletId(1L);
        topUpRequest.setAmount(new BigDecimal("123.45"));

        mockMvc.perform(
                MockMvcRequestBuilders.post("/api/wallet/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(topUpRequest))
        )
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        Mockito.verify(
                walletOperationService,
                Mockito.times(1)
        ).withdraw(anyLong(), any(BigDecimal.class));
    }

    @Test
    @SneakyThrows
    public void emptyWalletIdTest() {
        invalidBodyTest("{\"amount\":123.45}");
    }

    @Test
    @SneakyThrows
    public void emptyAmountTest() {
        invalidBodyTest("{\"walletId\":1}");
    }

    @Test
    @SneakyThrows
    public void emptyBodyTest() {
        invalidBodyTest("");
    }

    private void invalidBodyTest(String request) throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.post("/api/wallet/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().isBadRequest())
                .andExpect(content().string(""));

        Mockito.verify(
                walletOperationService,
                Mockito.times(0)
        ).withdraw(anyLong(), any(BigDecimal.class));
    }
}
