package com.concentrix.asset.repository;

import com.concentrix.asset.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String > {
    Optional<User> findBySSO(String SSO);

    Optional<User> findByEmail(String email);

    Optional<User> findByMSA(String MSA);


}