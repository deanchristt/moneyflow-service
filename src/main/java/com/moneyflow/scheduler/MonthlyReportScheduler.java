package com.moneyflow.scheduler;

import com.moneyflow.model.entity.User;
import com.moneyflow.model.enums.TransactionType;
import com.moneyflow.notification.NotificationService;
import com.moneyflow.repository.TransactionRepository;
import com.moneyflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * On the first of each month, sends every user a summary of the previous month's
 * income/expense via the configured notification channels. Cron is configurable
 * via {@code moneyflow.report.monthly-cron} (default 06:00 on the 1st).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyReportScheduler {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "${moneyflow.report.monthly-cron:0 0 6 1 * *}", zone = "${moneyflow.report.zone:UTC}")
    @Transactional(readOnly = true)
    public void sendMonthlyReports() {
        YearMonth lastMonth = YearMonth.from(LocalDate.now().minusMonths(1));
        LocalDate start = lastMonth.atDay(1);
        LocalDate end = lastMonth.atEndOfMonth();
        String periodLabel = lastMonth.toString();

        log.info("Monthly report job started for {}", periodLabel);
        int sent = 0;
        for (User user : userRepository.findAll()) {
            BigDecimal income = orZero(transactionRepository.sumAmountByTypeAndDateRange(
                    user.getId(), TransactionType.INCOME, start, end));
            BigDecimal expense = orZero(transactionRepository.sumAmountByTypeAndDateRange(
                    user.getId(), TransactionType.EXPENSE, start, end));
            notificationService.sendMonthlyReport(user, periodLabel, income, expense, income.subtract(expense));
            sent++;
        }
        log.info("Monthly report job finished, {} report(s) sent", sent);
    }

    private BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
