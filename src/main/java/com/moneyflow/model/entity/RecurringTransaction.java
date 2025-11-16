package com.moneyflow.model.entity;

import com.moneyflow.model.enums.Frequency;
import com.moneyflow.model.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recurring_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Frequency frequency;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "next_execution_date", nullable = false)
    private LocalDate nextExecutionDate;

    @Column(name = "last_executed_at")
    private LocalDateTime lastExecutedAt;

    @Column(name = "is_paused", nullable = false)
    @Builder.Default
    private Boolean isPaused = false;

    @OneToMany(mappedBy = "recurringTransaction", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Transaction> generatedTransactions = new ArrayList<>();

    public LocalDate calculateNextExecutionDate() {
        if (nextExecutionDate == null) {
            return startDate;
        }

        return switch (frequency) {
            case DAILY -> nextExecutionDate.plusDays(1);
            case WEEKLY -> nextExecutionDate.plusWeeks(1);
            case MONTHLY -> nextExecutionDate.plusMonths(1);
            case YEARLY -> nextExecutionDate.plusYears(1);
        };
    }
}
