package com.budgetapp.repository;

import com.budgetapp.model.Budget;
import com.budgetapp.model.Category;
import com.budgetapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUserAndMonthAndYear(User user, int month, int year);

    Optional<Budget> findByUserAndMonthAndYearAndCategory(
            User user, int month, int year, Category category);
}
