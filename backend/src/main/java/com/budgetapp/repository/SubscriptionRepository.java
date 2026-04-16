package com.budgetapp.repository;

import com.budgetapp.model.Subscription;
import com.budgetapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByUser(User user);

    List<Subscription> findByUserOrderByCreatedAtDesc(User user);
}
