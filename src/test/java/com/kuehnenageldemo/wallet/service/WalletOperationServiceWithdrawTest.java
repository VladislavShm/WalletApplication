package com.kuehnenageldemo.wallet.service;

import com.kuehnenageldemo.wallet.PostgresqlContainer;
import com.kuehnenageldemo.wallet.WalletApplication;
import com.kuehnenageldemo.wallet.entity.Wallet;
import com.kuehnenageldemo.wallet.entity.User;
import com.kuehnenageldemo.wallet.exception.NotEnoughFoundsException;
import com.kuehnenageldemo.wallet.repository.UserRepository;
import com.kuehnenageldemo.wallet.repository.wallet.WalletRepository;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DirtiesContext
@SpringBootTest(
        classes = {
                WalletApplication.class,
                WalletOperationServiceWithdrawTest.WithdrawConcurrentExecutionHelper.class
        },
        properties = {
                "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQL95Dialect",
                "spring.datasource.url=${DB_URL}",
                "spring.datasource.username=${DB_USERNAME}",
                "spring.datasource.password=${DB_PASSWORD}"
        })
public class WalletOperationServiceWithdrawTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private WalletOperationService walletOperationService;
    @Autowired
    private WithdrawConcurrentExecutionHelper withdrawConcurrentExecutionHelper;

    private final static PostgresqlContainer postgreSQLContainer = PostgresqlContainer.getInstance();
    private final static ExecutorService executor = Executors.newSingleThreadExecutor();

    @BeforeAll
    public static void init() {
        postgreSQLContainer.start();
    }

    @AfterAll
    public static void destroy() {
        postgreSQLContainer.stop();
        executor.shutdown();
    }

    @Test
    public void withdrawTest() {
        User user = userRepository.findByUsername("user1");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

        Long walletId = 1L;
        walletOperationService.withdraw(walletId, new BigDecimal("20.45"));

        Wallet wallet = walletRepository.findById(walletId).orElseThrow(() -> new RuntimeException("Wallet was not found."));
        assertEquals(new BigDecimal("29.55"), wallet.getBalance());
    }

    @Test
    @SneakyThrows
    public void concurrentWithdrawTest_commitBothOperations() {
        User user = userRepository.findByUsername("user2");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

        Long walletId = 4L;
        CountDownLatch firstExecutionLatch = new CountDownLatch(1);
        CountDownLatch secondExecutionLatch = new CountDownLatch(1);

        executor.execute(() -> {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
            withdrawConcurrentExecutionHelper.withdraw(walletId, new BigDecimal("1.90"), firstExecutionLatch, secondExecutionLatch);
        });

        Assertions.assertTrue(firstExecutionLatch.await(5, TimeUnit.SECONDS));

        walletOperationService.withdraw(walletId, new BigDecimal("2.87"));

        secondExecutionLatch.countDown();

        Wallet wallet = walletRepository.findById(walletId).orElseThrow(() -> new RuntimeException("Wallet was not found."));

        assertEquals(new BigDecimal("3545.23"), wallet.getBalance());
    }

    @Test
    @SneakyThrows
    public void concurrentWithdrawTest_commitOnlyFirstOperation() {
        User user = userRepository.findByUsername("user3");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

        Long walletId = 7L;
        CountDownLatch firstExecutionLatch = new CountDownLatch(1);
        CountDownLatch secondExecutionLatch = new CountDownLatch(1);

        executor.execute(() -> {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
            withdrawConcurrentExecutionHelper.withdraw(walletId, new BigDecimal("10.20"), firstExecutionLatch, secondExecutionLatch);
        });

        Assertions.assertTrue(firstExecutionLatch.await(5, TimeUnit.SECONDS));

        try {
            walletOperationService.withdraw(walletId, new BigDecimal("123.00"));
            throw new RuntimeException("Unexpected case.");
        } catch (NotEnoughFoundsException e) {
            // expected case
        } finally {
            secondExecutionLatch.countDown();
        }

        Wallet wallet = walletRepository.findById(walletId).orElseThrow(() -> new RuntimeException("Wallet was not found."));

        assertEquals(new BigDecimal("112.92"), wallet.getBalance());
    }

    @Component
    public static class WithdrawConcurrentExecutionHelper {
        @Autowired
        private WalletOperationService walletOperationService;

        @SneakyThrows
        @Transactional
        public void withdraw(Long walletId, BigDecimal amount, CountDownLatch firstExecutionLatch, CountDownLatch secondExecutionLatch) {
            walletOperationService.withdraw(walletId, amount);
            firstExecutionLatch.countDown();
            assertFalse(secondExecutionLatch.await(1, TimeUnit.SECONDS));
        }
    }
}
