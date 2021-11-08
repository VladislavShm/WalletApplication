package com.kuehnenageldemo.wallet.repository.wallet;

import com.kuehnenageldemo.wallet.entity.Wallet;
import com.kuehnenageldemo.wallet.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

@Repository
@RequiredArgsConstructor
public class WalletRepositoryCustomImpl implements WalletRepositoryCustom {
    private final EntityManager entityManager;

    @Override
    public Wallet queryByUserAndIdForUpdate(User user, Long walletId) {
        return entityManager
                .createQuery("select a from Wallet a where a.id=:id and a.owner=:owner", Wallet.class)
                .setParameter("owner", user) // user is used to prevent access to someone else's wallet
                .setParameter("id", walletId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getSingleResult();
    }

    @Override
    public Wallet queryByIdForUpdate(Long walletId) {
        return entityManager
                .createQuery("select a from Wallet a where a.id=:id", Wallet.class)
                .setParameter("id", walletId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getSingleResult();
    }
}
