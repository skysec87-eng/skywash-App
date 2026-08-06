package com.skywash.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.skywash.api.config.ApiException;
import com.skywash.api.entity.AuthSessionEntity;
import com.skywash.api.entity.OtpChallengeEntity;
import com.skywash.api.entity.UserEntity;
import com.skywash.api.repo.AuthSessionRepository;
import com.skywash.api.repo.OtpChallengeRepository;
import com.skywash.api.repo.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private AuthSessionRepository sessionRepository;
  @Mock private OtpChallengeRepository otpRepository;
  @Mock private EmailService emailService;
  @Mock private GoogleIdentityService googleIdentityService;

  private AuthService authService;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  @BeforeEach
  void setUp() {
    authService = new AuthService(
        userRepository, sessionRepository, otpRepository, emailService, googleIdentityService, true
    );
  }

  @Test
  void signupCreatesUserAndPersistsSession() {
    when(userRepository.existsByPhone("+2348011112222")).thenReturn(false);
    when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    when(sessionRepository.save(any(AuthSessionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    Map<String, Object> res = authService.signup("Ada", "+2348011112222", "pass123");
    assertEquals(true, res.get("ok"));
    assertTrue(String.valueOf(res.get("token")).startsWith("sw_"));

    ArgumentCaptor<AuthSessionEntity> captor = ArgumentCaptor.forClass(AuthSessionEntity.class);
    verify(sessionRepository).save(captor.capture());
    assertEquals(res.get("token"), captor.getValue().getToken());
  }

  @Test
  void signupRejectsDuplicatePhone() {
    when(userRepository.existsByPhone("+2348011112222")).thenReturn(true);
    assertThrows(ApiException.class, () -> authService.signup("Ada", "+2348011112222", "pass123"));
  }

  @Test
  void loginRejectsBadPassword() {
    UserEntity user = new UserEntity();
    user.setId("u1");
    user.setName("Ada");
    user.setPhone("+2348011112222");
    user.setPasswordHash(encoder.encode("pass123"));
    when(userRepository.findByPhone("+2348011112222")).thenReturn(Optional.of(user));

    assertThrows(ApiException.class, () -> authService.login("+2348011112222", "wrong"));
  }

  @Test
  void loginSucceedsAndMeResolvesSession() {
    UserEntity user = new UserEntity();
    user.setId("u1");
    user.setName("Ada");
    user.setPhone("+2348011112222");
    user.setPasswordHash(encoder.encode("pass123"));
    when(userRepository.findByPhone("+2348011112222")).thenReturn(Optional.of(user));
    when(sessionRepository.save(any(AuthSessionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    Map<String, Object> res = authService.login("+2348011112222", "pass123");
    String token = String.valueOf(res.get("token"));

    AuthSessionEntity session = new AuthSessionEntity();
    session.setToken(token);
    session.setUserId("u1");
    session.setCreatedAt(Instant.now());
    session.setExpiresAt(Instant.now().plusSeconds(3600));
    when(sessionRepository.findByTokenAndExpiresAtAfter(eq(token), any()))
        .thenReturn(Optional.of(session));
    when(userRepository.findById("u1")).thenReturn(Optional.of(user));

    Map<String, Object> me = authService.me("Bearer " + token);
    assertEquals("Ada", me.get("name"));
  }

  @Test
  void sendOtpEmailsCodeForSignup() {
    when(userRepository.existsByEmail("ada@example.com")).thenReturn(false);
    when(otpRepository.findFirstByEmailAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(anyString(), any()))
        .thenReturn(Optional.empty());
    when(otpRepository.save(any(OtpChallengeEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    Map<String, Object> res = authService.sendOtp("", "ada@example.com", "", "");
    assertEquals(true, res.get("ok"));
    assertEquals(true, res.get("new_account"));
    assertTrue(res.containsKey("debug_code"));
    verify(emailService).sendOtp(eq("ada@example.com"), anyString());
  }

  @Test
  void verifyOtpThenCompleteProfileForSignup() {
    String code = "123456";
    OtpChallengeEntity challenge = new OtpChallengeEntity();
    challenge.setId("otp1");
    challenge.setEmail("ada@example.com");
    challenge.setCodeHash(encoder.encode(code));
    challenge.setPurpose("signup");
    challenge.setCreatedAt(Instant.now());
    challenge.setExpiresAt(Instant.now().plusSeconds(600));
    challenge.setAttempts(0);

    when(otpRepository.findFirstByEmailAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(eq("ada@example.com"), any()))
        .thenReturn(Optional.of(challenge));
    when(otpRepository.save(any(OtpChallengeEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    Map<String, Object> verified = authService.verifyOtp("ada@example.com", code);
    assertEquals(true, verified.get("registration_required"));
    assertEquals("otp1", verified.get("registration_token"));

    when(otpRepository.findByIdAndEmailVerifiedTrueAndConsumedFalseAndExpiresAtAfter(eq("otp1"), any()))
        .thenReturn(Optional.of(challenge));
    when(userRepository.existsByEmail("ada@example.com")).thenReturn(false);
    when(userRepository.existsByPhone("+2348011112222")).thenReturn(false);
    when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    when(sessionRepository.save(any(AuthSessionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    Map<String, Object> res = authService.completeProfile("otp1", "Ada", "+2348011112222");
    assertEquals(true, res.get("ok"));
    assertTrue(String.valueOf(res.get("token")).startsWith("sw_"));
    @SuppressWarnings("unchecked")
    Map<String, Object> user = (Map<String, Object>) res.get("user");
    assertEquals("Ada", user.get("name"));
    assertEquals("ada@example.com", user.get("email"));
  }
}
