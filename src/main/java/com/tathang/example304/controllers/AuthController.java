package com.tathang.example304.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.tathang.example304.dto.ApiResponse;
import com.tathang.example304.dto.ChangePasswordRequest;
import com.tathang.example304.dto.ForgotPasswordRequest;
import com.tathang.example304.dto.LoginDto;
import com.tathang.example304.dto.RegisterDto;
import com.tathang.example304.dto.ResetPasswordRequest;
import com.tathang.example304.dto.VerifyOtpRequest;
import com.tathang.example304.model.ERole;
import com.tathang.example304.model.Role;
import com.tathang.example304.model.User;
import com.tathang.example304.repository.RoleRepository;
import com.tathang.example304.repository.UserRepository;
import com.tathang.example304.security.jwt.JwtUtils;
import com.tathang.example304.security.services.PasswordResetService;
import com.tathang.example304.security.services.UserDetailsImpl;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetService passwordResetService;

    public AuthController(UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtUtils jwtUtils,
            AuthenticationManager authenticationManager,
            PasswordResetService passwordResetService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.authenticationManager = authenticationManager;
        this.passwordResetService = passwordResetService;
    }

    // ✅ Health check
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Server is running! Auth controller is healthy.");
    }

    // ✅ Register
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterDto registerDto) {
        System.out.println("=== REGISTER ===");
        System.out.println("Username: " + registerDto.getUsername());
        System.out.println("Email: " + registerDto.getEmail());
        System.out.println("Password: " + registerDto.getPassword());
        System.out.println("Requested Roles: " + registerDto.getRoles());

        if (userRepository.existsByUsername(registerDto.getUsername())) {
            return new ResponseEntity<>("Username is taken!", HttpStatus.BAD_REQUEST);
        }

        if (userRepository.existsByEmail(registerDto.getEmail())) {
            return new ResponseEntity<>("Email is already in use!", HttpStatus.BAD_REQUEST);
        }

        User user = new User();
        user.setUsername(registerDto.getUsername());
        user.setEmail(registerDto.getEmail());
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));

        Set<Role> roles = new HashSet<>();

        if (registerDto.getRoles() == null || registerDto.getRoles().isEmpty()) {
            Role userRole = roleRepository.findByName(ERole.ROLE_CUSTOMER)
                    .orElseThrow(() -> new RuntimeException("Error: USER Role is not found."));
            roles.add(userRole);
            System.out.println("Assigning default USER role");
        } else {
            for (String roleName : registerDto.getRoles()) {
                try {
                    String roleEnumName = "ROLE_" + roleName.toUpperCase();
                    ERole roleEnum = ERole.valueOf(roleEnumName);

                    Role role = roleRepository.findByName(roleEnum)
                            .orElseThrow(() -> new RuntimeException("Error: Role " + roleName + " is not found."));
                    roles.add(role);
                    System.out.println("Assigning role: " + roleName);
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid role requested: " + roleName);
                    return new ResponseEntity<>("Invalid role: " + roleName, HttpStatus.BAD_REQUEST);
                }
            }
        }

        user.setRoles(roles);
        User savedUser = userRepository.save(user);

        System.out.println("User registered successfully with ID: " + savedUser.getId());
        System.out.println("Assigned roles: " + roles.stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList()));
        return new ResponseEntity<>("User registered success!", HttpStatus.OK);
    }

    // ✅ Login với refresh token
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDto loginDto) {
        try {
            System.out.println("🔐 LOGIN ATTEMPT ==================================");
            System.out.println("Username: " + loginDto.getUsername());

            // Kiểm tra user
            User user = userRepository.findByUsername(loginDto.getUsername())
                    .orElseThrow(() -> {
                        System.out.println("❌ USER NOT FOUND: " + loginDto.getUsername());
                        return new RuntimeException("User not found");
                    });

            System.out.println("✅ User found: " + user.getUsername());
            System.out.println("👤 Full Name: " + user.getFullName());
            System.out.println("👥 Roles count: " + user.getRoles().size());
            user.getRoles().forEach(role -> System.out.println("   - Role: " + role.getName()));

            // Authentication
            System.out.println("🔄 Attempting authentication...");
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword()));

            System.out.println("🎉 Authentication SUCCESS!");

            // Generate tokens
            System.out.println("🔑 Generating JWT token...");
            String jwt = jwtUtils.generateJwtToken(authentication);

            // 🆕 GENERATE REFRESH TOKEN
            String refreshToken = jwtUtils.generateRefreshToken(authentication);

            System.out.println("✅ JWT Token generated, length: " + jwt.length());
            System.out.println("✅ Refresh Token generated, length: " + refreshToken.length());

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(item -> item.getAuthority())
                    .collect(Collectors.toList());

            System.out.println("✅ Login successful! User: " + userDetails.getUsername());
            System.out.println("✅ Full Name: " + user.getFullName());
            System.out.println("✅ Roles: " + roles);
            System.out.println("==================================================");

            // 🆕 SỬA RESPONSE ĐỂ BAO GỒM REFRESH TOKEN
            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put("refreshToken", refreshToken); // 🆕 THÊM REFRESH TOKEN
            response.put("type", "Bearer");
            response.put("id", userDetails.getId());
            response.put("username", userDetails.getUsername());
            response.put("fullName", user.getFullName());
            response.put("email", userDetails.getEmail());
            response.put("roles", roles);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("❌ LOGIN FAILED: " + e.getMessage());
            e.printStackTrace();
            System.out.println("==================================================");
            return new ResponseEntity<>("Invalid username or password! Error: " + e.getMessage(),
                    HttpStatus.UNAUTHORIZED);
        }
    }

    // 🆕 ENDPOINT REFRESH TOKEN (Sử dụng POST /refresh thay vì /refresh-token)
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");

            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.badRequest().body("Refresh token is required");
            }

            System.out.println("🔄 REFRESH TOKEN ATTEMPT ===========================");
            System.out.println("Refresh token length: " + refreshToken.length());

            // Validate refresh token
            if (!jwtUtils.validateJwtToken(refreshToken)) {
                System.out.println("❌ Invalid refresh token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
            }

            // Check if token is expired
            if (jwtUtils.isTokenExpired(refreshToken)) {
                System.out.println("❌ Refresh token expired");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token expired");
            }

            // Get username from refresh token
            String username = jwtUtils.getUserNameFromJwtToken(refreshToken);
            System.out.println("✅ Valid refresh token for user: " + username);

            // Load user details
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Create authentication object từ user
            List<GrantedAuthority> authorities = user.getRoles().stream()
                    .map(role -> (GrantedAuthority) () -> role.getName().name())
                    .collect(Collectors.toList());

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    user.getUsername(), null, authorities);

            // Tạo new access token
            String newAccessToken = jwtUtils.generateJwtToken(authentication);
            System.out.println("✅ New access token generated, length: " + newAccessToken.length());

            // Tạo new refresh token
            String newRefreshToken = jwtUtils.generateRefreshToken(authentication);

            System.out.println("✅ Token refresh successful!");
            System.out.println("==================================================");

            // Return new tokens
            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", newAccessToken);
            response.put("refreshToken", newRefreshToken);
            response.put("tokenType", "Bearer");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("❌ REFRESH TOKEN FAILED: " + e.getMessage());
            e.printStackTrace();
            System.out.println("==================================================");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token refresh failed: " + e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("fullName", user.getFullName());
        response.put("imageUrl", user.getImageUrl());
        response.put("phone", user.getPhone());
        response.put("address", user.getAddress());
        response.put("roles", user.getRoles().stream()
                .map(r -> r.getName().name())
                .toList());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            return ResponseEntity
                    .badRequest()
                    .body("Mật khẩu hiện tại không đúng");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok("Đổi mật khẩu thành công");
    }

    // 🆕 ENDPOINT VALIDATE TOKEN
    @GetMapping("/validate-token")
    public ResponseEntity<?> validateToken() {
        // Nếu request đến được đây, token hợp lệ
        return ResponseEntity.ok(Map.of("message", "Token is valid"));
    }

    // 🆕 ENDPOINT VALIDATE (simple)
    @GetMapping("/validate")
    public ResponseEntity<?> validate() {
        return ResponseEntity.ok(Map.of("valid", true, "message", "Token is valid"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        boolean success = passwordResetService.sendPasswordResetEmail(request.getEmail());

        if (success) {
            return ResponseEntity.ok(new ApiResponse(true,
                    "Mã OTP đã được gửi đến email của bạn. Vui lòng kiểm tra email."));
        } else {
            return ResponseEntity.badRequest().body(new ApiResponse(false,
                    "Không tìm thấy email hoặc có lỗi xảy ra. Vui lòng thử lại."));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request,
            BindingResult bindingResult) {

        System.out.println("=== VERIFY OTP CONTROLLER ===");
        System.out.println("Email: " + request.getEmail());
        System.out.println("OTP: " + request.getOtp());

        // Kiểm tra validation errors
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            System.out.println("Validation error: " + errorMessage);
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Validation error: " + errorMessage));
        }

        try {
            var tokenOptional = passwordResetService.verifyOtp(request.getEmail(), request.getOtp());

            if (tokenOptional.isPresent()) {
                System.out.println("OTP verification successful. Token: " + tokenOptional.get());
                return ResponseEntity.ok(new ApiResponse(true,
                        "Xác thực OTP thành công",
                        Map.of("token", tokenOptional.get())));
            } else {
                System.out.println("OTP verification failed");
                return ResponseEntity.badRequest().body(new ApiResponse(false,
                        "Mã OTP không hợp lệ hoặc đã hết hạn"));
            }
        } catch (Exception e) {
            System.err.println("Error in verifyOtp controller: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse(false, "Lỗi server: " + e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        boolean success = passwordResetService.resetPassword(request.getToken(), request.getNewPassword());

        if (success) {
            return ResponseEntity.ok(new ApiResponse(true,
                    "Đặt lại mật khẩu thành công. Bạn có thể đăng nhập bằng mật khẩu mới."));
        } else {
            return ResponseEntity.badRequest().body(new ApiResponse(false,
                    "Token không hợp lệ hoặc đã hết hạn. Vui lòng thử lại."));
        }
    }

    /**
     * Gửi lại OTP
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse> resendOtp(@Valid @RequestBody ForgotPasswordRequest request) {
        boolean success = passwordResetService.resendOtp(request.getEmail());

        if (success) {
            return ResponseEntity.ok(new ApiResponse(true,
                    "Đã gửi lại mã OTP. Vui lòng kiểm tra email."));
        } else {
            return ResponseEntity.badRequest().body(new ApiResponse(false,
                    "Không thể gửi lại mã OTP. Vui lòng thử lại."));
        }
    }

    // DTO helper
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class TokenResponse {
        private String token;
    }
}