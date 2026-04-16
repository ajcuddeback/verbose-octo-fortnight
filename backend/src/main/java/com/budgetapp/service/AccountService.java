package com.budgetapp.service;

import com.budgetapp.dto.request.AccountRequest;
import com.budgetapp.dto.response.AccountResponse;
import com.budgetapp.exception.ResourceNotFoundException;
import com.budgetapp.exception.UnauthorizedException;
import com.budgetapp.model.Account;
import com.budgetapp.model.User;
import com.budgetapp.repository.AccountRepository;
import com.budgetapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Transactional
    public AccountResponse createAccount(AccountRequest request, Long userId) {
        User user = getUserById(userId);

        Account account = Account.builder()
                .user(user)
                .name(request.getName())
                .type(request.getType())
                .balance(request.getBalance())
                .bankName(request.getBankName())
                .accountNumberLast4(request.getAccountNumberLast4())
                .isActive(true)
                .build();

        Account saved = accountRepository.save(account);
        log.info("Account created with id: {} for userId: {}", saved.getId(), userId);
        return mapToResponse(saved);
    }

    public List<AccountResponse> getUserAccounts(Long userId) {
        User user = getUserById(userId);
        List<Account> accounts = accountRepository.findByUserAndIsActive(user, true);
        return accounts.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public AccountResponse updateAccount(Long id, AccountRequest request, Long userId) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));

        if (!account.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Account does not belong to this user");
        }

        account.setName(request.getName());
        account.setType(request.getType());
        account.setBalance(request.getBalance());
        account.setBankName(request.getBankName());
        account.setAccountNumberLast4(request.getAccountNumberLast4());

        Account updated = accountRepository.save(account);
        log.info("Account updated with id: {} for userId: {}", id, userId);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteAccount(Long id, Long userId) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account", id));

        if (!account.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Account does not belong to this user");
        }

        // Soft delete
        account.setActive(false);
        accountRepository.save(account);
        log.info("Account soft-deleted with id: {} for userId: {}", id, userId);
    }

    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .name(account.getName())
                .type(account.getType())
                .balance(account.getBalance())
                .bankName(account.getBankName())
                .accountNumberLast4(account.getAccountNumberLast4())
                .isActive(account.isActive())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
