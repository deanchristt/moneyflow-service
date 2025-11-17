package com.moneyflow.model.dto.account;

import com.moneyflow.model.enums.AccountType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAccountRequest {

    @Size(max = 100, message = "Account name must not exceed 100 characters")
    private String name;

    private AccountType type;

    private String icon;

    private String color;

    private Boolean isDefault;
}
