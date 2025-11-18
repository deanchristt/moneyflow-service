package com.moneyflow.service;

import com.moneyflow.model.entity.Account;
import com.moneyflow.model.entity.Category;
import com.moneyflow.model.entity.User;
import com.moneyflow.model.enums.AccountType;
import com.moneyflow.model.enums.CategoryType;
import com.moneyflow.repository.AccountRepository;
import com.moneyflow.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DataSeederService {

    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public void seedDefaultDataForUser(User user) {
        seedDefaultCategories(user);
        seedDefaultAccount(user);
    }

    private void seedDefaultCategories(User user) {
        // Expense categories
        List<CategoryData> expenseCategories = Arrays.asList(
                new CategoryData("Food & Dining", "restaurant", "#FF6B6B"),
                new CategoryData("Transportation", "car", "#4ECDC4"),
                new CategoryData("Shopping", "cart", "#45B7D1"),
                new CategoryData("Entertainment", "film", "#96CEB4"),
                new CategoryData("Bills & Utilities", "receipt", "#FFEAA7"),
                new CategoryData("Healthcare", "heart", "#DDA0DD"),
                new CategoryData("Education", "book", "#98D8C8"),
                new CategoryData("Personal Care", "user", "#F7DC6F"),
                new CategoryData("Home", "home", "#BB8FCE"),
                new CategoryData("Gifts & Donations", "gift", "#85C1E9"),
                new CategoryData("Travel", "plane", "#F8B500"),
                new CategoryData("Insurance", "shield", "#A3E4D7"),
                new CategoryData("Taxes", "file-text", "#D5DBDB"),
                new CategoryData("Other Expense", "more-horizontal", "#BDC3C7")
        );

        for (CategoryData data : expenseCategories) {
            Category category = Category.builder()
                    .user(user)
                    .name(data.name)
                    .type(CategoryType.EXPENSE)
                    .icon(data.icon)
                    .color(data.color)
                    .isDefault(true)
                    .build();
            categoryRepository.save(category);
        }

        // Income categories
        List<CategoryData> incomeCategories = Arrays.asList(
                new CategoryData("Salary", "briefcase", "#27AE60"),
                new CategoryData("Freelance", "laptop", "#3498DB"),
                new CategoryData("Investments", "trending-up", "#9B59B6"),
                new CategoryData("Rental Income", "home", "#E67E22"),
                new CategoryData("Business", "building", "#1ABC9C"),
                new CategoryData("Bonus", "award", "#F39C12"),
                new CategoryData("Gifts Received", "gift", "#E91E63"),
                new CategoryData("Refunds", "refresh-cw", "#00BCD4"),
                new CategoryData("Other Income", "more-horizontal", "#95A5A6")
        );

        for (CategoryData data : incomeCategories) {
            Category category = Category.builder()
                    .user(user)
                    .name(data.name)
                    .type(CategoryType.INCOME)
                    .icon(data.icon)
                    .color(data.color)
                    .isDefault(true)
                    .build();
            categoryRepository.save(category);
        }

        // Transfer category
        Category transferCategory = Category.builder()
                .user(user)
                .name("Transfer")
                .type(CategoryType.EXPENSE)
                .icon("repeat")
                .color("#7F8C8D")
                .isDefault(true)
                .build();
        categoryRepository.save(transferCategory);
    }

    private void seedDefaultAccount(User user) {
        Account defaultAccount = Account.builder()
                .user(user)
                .name("Cash")
                .type(AccountType.CASH)
                .balance(BigDecimal.ZERO)
                .currency("USD")
                .icon("wallet")
                .color("#2ECC71")
                .isDefault(true)
                .build();
        accountRepository.save(defaultAccount);
    }

    private record CategoryData(String name, String icon, String color) {}
}
