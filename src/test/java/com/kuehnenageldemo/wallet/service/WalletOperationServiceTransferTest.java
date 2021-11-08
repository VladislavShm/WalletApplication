package com.kuehnenageldemo.wallet.service;

import com.kuehnenageldemo.wallet.PostgresqlContainer;
import com.kuehnenageldemo.wallet.WalletApplication;
import com.kuehnenageldemo.wallet.entity.Wallet;
import com.kuehnenageldemo.wallet.entity.User;
import com.kuehnenageldemo.wallet.exception.NotEnoughFoundsException;
import com.kuehnenageldemo.wallet.exception.TransferBetweenSameWalletException;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DirtiesContext
@SpringBootTest(
        classes = {
                WalletApplication.class,
                WalletOperationServiceTransferTest.TransferConcurrentExecutionHelper.class
        },
        properties = {
                "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQL95Dialect",
                "spring.datasource.url=${DB_URL}",
                "spring.datasource.username=${DB_USERNAME}",
                "spring.datasource.password=${DB_PASSWORD}"
        })
public class WalletOperationServiceTransferTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private WalletOperationService walletOperationService;
    @Autowired
    private TransferConcurrentExecutionHelper transferConcurrentExecutionHelper;

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
    public void transferTest() {
        User user = userRepository.findByUsername("user1");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

        Long toWalletId = 4L;
        Long fromWalletId = 1L;
        walletOperationService.transfer(fromWalletId, toWalletId, new BigDecimal("20.45"));

        checkBalance(toWalletId, "3570.45");
        checkBalance(fromWalletId, "29.55");
    }

    @Test
    public void transferBetweenSameWalletExceptionTest() {
        User user = userRepository.findByUsername("user1");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

        Long walletId = 1L;

        assertThatThrownBy(() -> walletOperationService.transfer(walletId, walletId, new BigDecimal("20.45")))
                .isInstanceOf(TransferBetweenSameWalletException.class);
    }

    @Test
    @SneakyThrows
    public void concurrentTransferTest_commitBothOperations() {
        User user = userRepository.findByUsername("user1");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

        Long toWalletId = 5L;
        Long fromWalletId = 2L;
        CountDownLatch firstExecutionLatch = new CountDownLatch(1);
        CountDownLatch secondExecutionLatch = new CountDownLatch(1);

        executor.execute(() -> {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
            transferConcurrentExecutionHelper.transfer(fromWalletId, toWalletId, new BigDecimal("1.90"), firstExecutionLatch, secondExecutionLatch);
        });

        Assertions.assertTrue(firstExecutionLatch.await(5, TimeUnit.SECONDS));

        walletOperationService.transfer(fromWalletId, toWalletId, new BigDecimal("2.87"));

        secondExecutionLatch.countDown();

        checkBalance(toWalletId, "9055.10");
        checkBalance(fromWalletId, "145.28");
    }

    @Test
    @SneakyThrows
    public void concurrentTransferTest_commitOnlyFirstOperation() {
        User user = userRepository.findByUsername("user1");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

        Long toWalletId = 6L;
        Long fromWalletId = 3L;
        CountDownLatch firstExecutionLatch = new CountDownLatch(1);
        CountDownLatch secondExecutionLatch = new CountDownLatch(1);

        executor.execute(() -> {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
            transferConcurrentExecutionHelper.transfer(fromWalletId, toWalletId, new BigDecimal("1.90"), firstExecutionLatch, secondExecutionLatch);
        });

        Assertions.assertTrue(firstExecutionLatch.await(5, TimeUnit.SECONDS));

        try {
            walletOperationService.transfer(fromWalletId, toWalletId, new BigDecimal("123.00"));
            throw new RuntimeException("Unexpected case.");
        } catch (NotEnoughFoundsException e) {
            // expected case
        } finally {
            secondExecutionLatch.countDown();
        }

        checkBalance(toWalletId, "55.96");
        checkBalance(fromWalletId, "28.20");
    }

    private void checkBalance(Long walletId, String balance) {
        Wallet toWallet = walletRepository.findById(walletId).orElseThrow(() -> new RuntimeException("Wallet was not found."));
        assertEquals(new BigDecimal(balance), toWallet.getBalance());
    }

    @Component
    public static class TransferConcurrentExecutionHelper {
        @Autowired
        private WalletOperationService walletOperationService;

        @SneakyThrows
        @Transactional
        public void transfer(Long fromWalletId, Long toWalletId, BigDecimal amount, CountDownLatch firstExecutionLatch, CountDownLatch secondExecutionLatch) {
            walletOperationService.transfer(fromWalletId, toWalletId, amount);
            firstExecutionLatch.countDown();
            assertFalse(secondExecutionLatch.await(1, TimeUnit.SECONDS));
        }
    }
}
