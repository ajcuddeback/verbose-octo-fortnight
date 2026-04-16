package com.budgetapp.repository;

import com.budgetapp.model.Bill;
import com.budgetapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    List<Bill> findByUserAndIsActive(User user, boolean isActive);

    List<Bill> findByUserAndDueDayLessThanEqual(User user, int dueDay);
}
