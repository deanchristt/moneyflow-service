package com.moneyflow.scheduler;

import com.moneyflow.service.RecurringTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically executes due recurring transactions (and back-fills any missed
 * periods). The cron expression is configurable via {@code moneyflow.recurring.cron}
 * and defaults to 01:00 every day.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringTransactionScheduler {

    private final RecurringTransactionService recurringTransactionService;

    @Scheduled(cron = "${moneyflow.recurring.cron:0 0 1 * * *}", zone = "${moneyflow.recurring.zone:UTC}")
    public void runDueRecurringTransactions() {
        log.info("Recurring transaction job started");
        int processed = recurringTransactionService.processDueRecurringTransactions();
        log.info("Recurring transaction job finished, {} transaction(s) generated", processed);
    }
}
