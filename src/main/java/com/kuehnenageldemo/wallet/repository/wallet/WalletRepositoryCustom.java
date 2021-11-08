package com.kuehnenageldemo.wallet.repository.wallet;

import com.kuehnenageldemo.wallet.entity.Wallet;
import com.kuehnenageldemo.wallet.entity.User;

public interface WalletRepositoryCustom {
    Wallet queryByUserAndIdForUpdate(User user, Long walletId);
    Wallet queryByIdForUpdate(Long walletId);
}
