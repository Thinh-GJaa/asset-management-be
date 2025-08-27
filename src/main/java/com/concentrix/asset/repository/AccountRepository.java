package com.concentrix.asset.repository;

import com.concentrix.asset.entity.Account;
import com.concentrix.asset.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer>, JpaSpecificationExecutor<Account> {

    Optional<Account> findByAccountName(String accountName);

    Optional<Account> findByAccountCode(String accountCode);

    @Query("SELECT distinct a.owner FROM Account a")
    List<User> findAllOwner();

}