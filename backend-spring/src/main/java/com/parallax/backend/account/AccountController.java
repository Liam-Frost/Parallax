package com.parallax.backend.account;

import com.parallax.backend.security.CurrentUserService;
import com.parallax.backend.user.UserEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/account")
public class AccountController {
    private final CurrentUserService currentUserService;
    private final AccountService accountService;

    public AccountController(CurrentUserService currentUserService, AccountService accountService) {
        this.currentUserService = currentUserService;
        this.accountService = accountService;
    }

    @GetMapping("/me")
    public ResponseEntity<AccountResponse> me() {
        UserEntity user = currentUserService.requireUser();
        return ResponseEntity.ok(AccountResponse.from(user));
    }

    @PostMapping("/contact")
    public ResponseEntity<Map<String, Object>> updateContact(@RequestBody UpdateContactRequest request) {
        accountService.updateContact(request);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/password")
    public ResponseEntity<Map<String, Object>> updatePassword(@RequestBody UpdatePasswordRequest request) {
        accountService.updatePassword(request);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Object>> deleteAccount(@RequestBody DeleteAccountRequest request) {
        accountService.deleteAccount(request);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
