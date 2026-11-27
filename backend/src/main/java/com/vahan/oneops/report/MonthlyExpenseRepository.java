package com.vahan.oneops.report;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlyExpenseRepository extends JpaRepository<MonthlyExpense, String> {
}
