package com.budgetapp.repository;

import com.budgetapp.model.Account;
import com.budgetapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByUserAndIsActive(User user, boolean isActive);

    List<Account> findByUser(User user);
}
