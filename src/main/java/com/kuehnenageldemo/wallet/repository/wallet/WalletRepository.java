package com.kuehnenageldemo.wallet.repository.wallet;

import com.kuehnenageldemo.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public interface WalletRepository extends JpaRepository<Wallet, Long>, WalletRepositoryCustom {
    List<Wallet> queryWalletsByOwnerId(Long ownerId);

    List<Wallet> queryWalletsByOwnerIdAndIdIsNot(Long ownerId, Long walletId);
}
