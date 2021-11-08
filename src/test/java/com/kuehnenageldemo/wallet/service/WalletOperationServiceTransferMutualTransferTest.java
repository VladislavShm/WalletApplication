package com.kuehnenageldemo.wallet.service;

import com.kuehnenageldemo.wallet.PostgresqlContainer;
import com.kuehnenageldemo.wallet.WalletApplication;
import com.kuehnenageldemo.wallet.entity.User;
import com.kuehnenageldemo.wallet.entity.Wallet;
import com.kuehnenageldemo.wallet.repository.UserRepository;
import com.kuehnenageldemo.wallet.repository.wallet.WalletRepository;
import com.kuehnenageldemo.wallet.repository.wallet.WalletRepositoryCustomImpl;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@DirtiesContext
@SpringBootTest(
        classes = {
                WalletApplication.class
        },
        properties = {
                "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQL95Dialect",
                "spring.datasource.url=${DB_URL}",
                "spring.datasource.username=${DB_USERNAME}",
                "spring.datasource.password=${DB_PASSWORD}"
        })
public class WalletOperationServiceTransferMutualTransferTest {
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WalletOperationService walletOperationService;

    @MockBean
    private WalletRepository walletRepository;

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

    /**
     * This test was made to test the situation in which User1 sends some money from its wallet1 to User2 wallet2,
     * and at the same time User2 sends some money from wallet2 to User1 wallet1. This situation can lead to a deadlock
     * in case of incorrect locking of wallets in the database.
     * We should ensure that will be locked the same wallet first, and then another in both scenarios.
     * Current implementation locks wallets according their IDs. A wallet with the smallest ID is locked first.
    * */
    @Test
    @SneakyThrows
    public void concurrentTransferTest_synchronousMutualMoneyTransfer() {
        WalletRepositoryCustomImpl walletRepositoryCustomImpl = new WalletRepositoryCustomImpl(entityManager);

        User user1 = userRepository.findByUsername("user1");
        User user2 = userRepository.findByUsername("user2");

        Long walletId1 = 1L;
        Long walletId2 = 4L;
        CountDownLatch firstExecutionLatch = new CountDownLatch(1);
        CountDownLatch secondExecutionLatch = new CountDownLatch(1);

        when(walletRepository.queryByUserAndIdForUpdate(any(User.class), anyLong())).then(invocation -> {
            Wallet result = walletRepositoryCustomImpl.queryByUserAndIdForUpdate(invocation.getArgument(0), invocation.getArgument(1));

            User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (currentUser.equals(user1)) {
                secondExecutionLatch.countDown();
                firstExecutionLatch.await(5, TimeUnit.SECONDS);
            } else {
                firstExecutionLatch.countDown();
                secondExecutionLatch.await(5, TimeUnit.SECONDS);
            }

            return result;
        });

        when(walletRepository.queryByIdForUpdate(anyLong())).then(invocation -> {
            Wallet result = walletRepositoryCustomImpl.queryByIdForUpdate(invocation.getArgument(0));

            User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (currentUser.equals(user1)) {
                secondExecutionLatch.countDown();
                firstExecutionLatch.await(5, TimeUnit.SECONDS);
            } else {
                firstExecutionLatch.countDown();
                secondExecutionLatch.await(5, TimeUnit.SECONDS);
            }

            return result;
        });


        executor.execute(() -> {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user1, null));
            walletOperationService.transfer(walletId1, walletId2, new BigDecimal(10));
        });

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user2, null));
        Assertions.assertTrue(secondExecutionLatch.await(5, TimeUnit.SECONDS));
        walletOperationService.transfer(walletId2, walletId1, new BigDecimal(20));

        checkBalance(walletId1, "60.00");
        checkBalance(walletId2, "3540.00");
    }

    private void checkBalance(Long walletId, String balance) {
        Wallet toWallet = entityManager.createQuery("select w from Wallet w where id=:id", Wallet.class)
                .setParameter("id", walletId)
                .getSingleResult();
        assertEquals(new BigDecimal(balance), toWallet.getBalance());
    }
}
