package com.kuehnenageldemo.wallet.service;

import com.kuehnenageldemo.wallet.entity.Wallet;
import com.kuehnenageldemo.wallet.entity.User;
import com.kuehnenageldemo.wallet.exception.WalletNotFoundException;
import com.kuehnenageldemo.wallet.exception.NotEnoughFoundsException;
import com.kuehnenageldemo.wallet.exception.TransferBetweenSameWalletException;
import com.kuehnenageldemo.wallet.repository.wallet.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class WalletOperationService {
    private final WalletRepository walletRepository;

    public void topUp(Long walletId, BigDecimal amount) {
        User user = UserService.getCurrentUser();
        Wallet wallet = findUserWallet(user, walletId);
        wallet.setBalance(wallet.getBalance().add(amount));
    }

    public void withdraw(Long walletId, BigDecimal amount) {
        User user = UserService.getCurrentUser();
        Wallet wallet = findUserWallet(user, walletId);
        checkWalletBalance(amount, wallet);
        wallet.setBalance(wallet.getBalance().add(amount.negate()));
    }

    public void transfer(Long fromWalletId, Long toWalletId, BigDecimal amount) {
        if (fromWalletId.equals(toWalletId)) {
            throw new TransferBetweenSameWalletException();
        }

        Wallet toWallet;
        Wallet fromWallet;
        User user = UserService.getCurrentUser();

        // To avoid a deadlock in the case of synchronous money transfer between two identical wallets,
        // first we block a wallet with the smallest ID,
        // and then we block a wallet with the largest ID.
        if (fromWalletId > toWalletId) {
            toWallet = walletRepository.queryByIdForUpdate(toWalletId);
            fromWallet = findUserWallet(user, fromWalletId);
        } else {
            fromWallet = findUserWallet(user, fromWalletId);
            toWallet = walletRepository.queryByIdForUpdate(toWalletId);
        }

        if (toWallet == null) {
            log.warn("Wallet with ID={} wasn't found.", toWalletId);
            throw new WalletNotFoundException();
        }

        checkWalletBalance(amount, fromWallet);

        toWallet.setBalance(toWallet.getBalance().add(amount));
        fromWallet.setBalance(fromWallet.getBalance().add(amount.negate()));
    }

    private void checkWalletBalance(BigDecimal amountToDeduct, Wallet wallet) {
        if (wallet.getBalance().compareTo(amountToDeduct) < 0) {
            throw new NotEnoughFoundsException();
        }
    }

    private Wallet findUserWallet(User user, Long walletId) {
        Wallet wallet = walletRepository.queryByUserAndIdForUpdate(user, walletId);
        if (wallet == null) {
            log.warn("Wallet with ID={} wasn't found for user: {}", walletId, user);
            throw new WalletNotFoundException();
        }

        return wallet;
    }
}
