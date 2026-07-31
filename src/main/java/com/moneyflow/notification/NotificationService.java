package com.moneyflow.notification;

import com.moneyflow.model.entity.Budget;
import com.moneyflow.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Fans a notification out to every configured {@link NotificationSender}.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final List<NotificationSender> senders;

    public void sendBudgetAlert(User user, Budget budget, BigDecimal spent,
                                BigDecimal percentageUsed, boolean overBudget) {
        String categoryName = budget.getCategory().getName();
        String subject = overBudget
                ? "Budget exceeded: " + categoryName
                : "Budget alert: " + categoryName;
        String message = String.format(
                "You have spent %s of your %s budget for %02d/%d (%.2f%% used).",
                spent, categoryName, budget.getMonth(), budget.getYear(), percentageUsed);

        senders.forEach(sender -> sender.send(user.getEmail(), subject, message));
    }

    public void sendMonthlyReport(User user, String periodLabel, BigDecimal income,
                                  BigDecimal expense, BigDecimal net) {
        String subject = "Your MoneyFlow report for " + periodLabel;
        String message = String.format(
                "Summary for %s — income: %s, expense: %s, net cash flow: %s.",
                periodLabel, income, expense, net);
        senders.forEach(sender -> sender.send(user.getEmail(), subject, message));
    }
}
