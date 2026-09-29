package com.asfaw.review_ai.web.api;

import com.asfaw.review_ai.service.UserAccountService;
import com.asfaw.review_ai.web.api.dto.UserCreateRequest;
import com.asfaw.review_ai.web.api.dto.UserResponse;
import com.asfaw.review_ai.web.api.dto.UserUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin-only (enforced in SecurityConfig). Users are disabled rather than deleted to keep history intact. */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserApiController {

    private final UserAccountService userAccountService;

    @GetMapping
    public List<UserResponse> list() {
        return userAccountService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserCreateRequest request) {
        return userAccountService.create(request);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return userAccountService.update(id, request);
    }
}
