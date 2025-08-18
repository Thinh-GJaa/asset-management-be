package com.concentrix.asset.repository;

import com.concentrix.asset.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer>, JpaSpecificationExecutor<Account> {

    Account findByAccountName(String accountName);

    Account findByAccountCode(String accountCode);

}