package com.skywash.api.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skywash.api.config.ApiException;
import com.skywash.api.entity.AuthSessionEntity;
import com.skywash.api.entity.OtpChallengeEntity;
import com.skywash.api.entity.UserEntity;
import com.skywash.api.repo.AuthSessionRepository;
import com.skywash.api.repo.OtpChallengeRepository;
import com.skywash.api.repo.UserRepository;

@Service
public class AuthService {

  private static final int SESSION_DAYS = 30;
  private static final int OTP_TTL_MINUTES = 10;
  private static final int OTP_RESEND_SECONDS = 45;
  private static final int OTP_MAX_ATTEMPTS = 5;

  private final UserRepository userRepository;
  private final AuthSessionRepository sessionRepository;
  private final OtpChallengeRepository otpRepository;
  private final EmailService emailService;
  private final GoogleIdentityService googleIdentityService;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
  private final SecureRandom random = new SecureRandom();
  private final boolean exposeOtpCode;

  public AuthService(
      UserRepository userRepository,
      AuthSessionRepository sessionRepository,
      OtpChallengeRepository otpRepository,
      EmailService emailService,
      GoogleIdentityService googleIdentityService,
      @Value("${skywash.otp.expose-code:false}") boolean exposeOtpCode
  ) {
    this.userRepository = userRepository;
    this.sessionRepository = sessionRepository;
    this.otpRepository = otpRepository;
    this.emailService = emailService;
    this.googleIdentityService = googleIdentityService;
    this.exposeOtpCode = exposeOtpCode;
  }

  @Transactional
  public Map<String, Object> signup(String name, String phone, String password) {
    String n = trim(name);
    String p = normalizePhone(phone);
    String pw = password == null ? "" : password;

    if (n.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "name is required");
    if (p.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "phone is required");
    if (pw.length() < 4) throw new ApiException(HttpStatus.BAD_REQUEST, "password must be at least 4 characters");
    if (userRepository.existsByPhone(p)) {
      throw new ApiException(HttpStatus.CONFLICT, "An account with this phone already exists");
    }

    UserEntity user = new UserEntity();
    user.setId(UUID.randomUUID().toString());
    user.setName(n);
    user.setPhone(p);
    user.setPasswordHash(encoder.encode(pw));
    userRepository.save(user);

    String token = issueToken(user.getId());
    return Map.of("ok", true, "token", token, "user", toPublic(user));
  }

  @Transactional
  public Map<String, Object> login(String phone, String password) {
    String p = normalizePhone(phone);
    String pw = password == null ? "" : password;
    if (p.isEmpty() || pw.isEmpty()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "phone and password are required");
    }

    UserEntity user = userRepository.findByPhone(p)
        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid phone or password"));
    if (user.getPasswordHash() == null || !encoder.matches(pw, user.getPasswordHash())) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid phone or password");
    }

    String token = issueToken(user.getId());
    return Map.of("ok", true, "token", token, "user", toPublic(user));
  }

  /**
   * Send a 6-digit email OTP. Email alone is enough — existing users log in, new users continue to profile.
   */
  @Transactional
  public Map<String, Object> sendOtp(String purposeRaw, String emailRaw, String name, String phone) {
    String email = normalizeEmail(emailRaw);
    if (email.isEmpty() || !email.contains("@")) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "A valid email is required");
    }

    boolean exists = userRepository.existsByEmail(email);
    String purpose = trim(purposeRaw).toLowerCase();

    // Reset access = existing account only (forgot / trouble signing in)
    if (purpose.equals("reset") || purpose.equals("forgot")) {
      if (!exists) {
        throw new ApiException(HttpStatus.NOT_FOUND, "No account found for this email");
      }
      purpose = "login";
    } else if (!purpose.equals("signup") && !purpose.equals("login")) {
      purpose = exists ? "login" : "signup";
    }
    if (purpose.equals("login") && !exists) {
      purpose = "signup";
    }
    if (purpose.equals("signup") && exists) {
      purpose = "login";
    }

    Optional<OtpChallengeEntity> latest = otpRepository
        .findFirstByEmailAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(email, Instant.now());
    if (latest.isPresent() && !latest.get().isEmailVerified()) {
      long ageSec = ChronoUnit.SECONDS.between(latest.get().getCreatedAt(), Instant.now());
      if (ageSec < OTP_RESEND_SECONDS) {
        throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
            "Please wait " + (OTP_RESEND_SECONDS - ageSec) + "s before requesting another code");
      }
    }

    otpRepository.consumeAllForEmail(email);

    String code = String.format("%06d", random.nextInt(1_000_000));
    OtpChallengeEntity challenge = new OtpChallengeEntity();
    challenge.setId(UUID.randomUUID().toString());
    challenge.setEmail(email);
    challenge.setCodeHash(encoder.encode(code));
    challenge.setPurpose(purpose);
    challenge.setPendingName(trim(name).isEmpty() ? null : trim(name));
    challenge.setPendingPhone(normalizePhone(phone).isEmpty() ? null : normalizePhone(phone));
    challenge.setCreatedAt(Instant.now());
    challenge.setExpiresAt(Instant.now().plus(OTP_TTL_MINUTES, ChronoUnit.MINUTES));
    otpRepository.save(challenge);

    emailService.sendOtp(email, code);

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("ok", true);
    out.put("email", email);
    out.put("purpose", purpose);
    out.put("new_account", "signup".equals(purpose));
    out.put("expires_in_seconds", OTP_TTL_MINUTES * 60);
    out.put("message", "Check your email for a 6-digit code");
    if (exposeOtpCode) out.put("debug_code", code);
    return out;
  }

  @Transactional
  public Map<String, Object> verifyOtp(String emailRaw, String codeRaw) {
    String email = normalizeEmail(emailRaw);
    String code = trim(codeRaw);
    if (email.isEmpty() || code.length() != 6) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "email and 6-digit code are required");
    }

    OtpChallengeEntity challenge = otpRepository
        .findFirstByEmailAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(email, Instant.now())
        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Code expired or not found — request a new one"));

    if (challenge.isEmailVerified()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Email already verified — finish your profile");
    }

    if (challenge.getAttempts() >= OTP_MAX_ATTEMPTS) {
      challenge.setConsumed(true);
      otpRepository.save(challenge);
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Too many attempts — request a new code");
    }

    if (!encoder.matches(code, challenge.getCodeHash())) {
      challenge.setAttempts(challenge.getAttempts() + 1);
      otpRepository.save(challenge);
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid code");
    }

    if ("login".equals(challenge.getPurpose())) {
      challenge.setConsumed(true);
      otpRepository.save(challenge);
      UserEntity user = userRepository.findByEmail(email)
          .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Account not found"));
      String token = issueToken(user.getId());
      return Map.of(
          "ok", true,
          "registration_required", false,
          "token", token,
          "user", toPublic(user)
      );
    }

    // New user: email verified — collect name + phone next
    challenge.setEmailVerified(true);
    challenge.setExpiresAt(Instant.now().plus(OTP_TTL_MINUTES, ChronoUnit.MINUTES));
    otpRepository.save(challenge);

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("ok", true);
    out.put("registration_required", true);
    out.put("registration_token", challenge.getId());
    out.put("email", email);
    return out;
  }

  @Transactional
  public Map<String, Object> completeProfile(String registrationToken, String name, String phone) {
    String tokenId = trim(registrationToken);
    String n = trim(name);
    String p = normalizePhone(phone);
    if (tokenId.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "registration_token is required");
    if (n.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "name is required");
    if (p.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "phone is required");

    OtpChallengeEntity challenge = otpRepository
        .findByIdAndEmailVerifiedTrueAndConsumedFalseAndExpiresAtAfter(tokenId, Instant.now())
        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Session expired — verify your email again"));

    String email = challenge.getEmail();
    if (userRepository.existsByEmail(email)) {
      throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
    }
    if (userRepository.existsByPhone(p)) {
      throw new ApiException(HttpStatus.CONFLICT, "An account with this phone already exists");
    }

    UserEntity user = new UserEntity();
    user.setId(UUID.randomUUID().toString());
    user.setName(n);
    user.setPhone(p);
    user.setEmail(email);
    if (challenge.getPendingGoogleSub() != null && !challenge.getPendingGoogleSub().isBlank()) {
      user.setGoogleSub(challenge.getPendingGoogleSub());
    }
    user.setPasswordHash(encoder.encode(UUID.randomUUID().toString()));
    userRepository.save(user);

    challenge.setConsumed(true);
    challenge.setPendingName(n);
    challenge.setPendingPhone(p);
    otpRepository.save(challenge);

    String token = issueToken(user.getId());
    return Map.of("ok", true, "token", token, "user", toPublic(user));
  }

  public Map<String, Object> googleConfig() {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("enabled", googleIdentityService.isConfigured());
    out.put("client_id", googleIdentityService.clientId());
    return out;
  }

  @Transactional
  public Map<String, Object> loginWithGoogle(String idToken) {
    GoogleIdentityService.GoogleProfile profile = googleIdentityService.verifyIdToken(idToken);

    Optional<UserEntity> existing = userRepository.findByGoogleSub(profile.subject())
        .or(() -> userRepository.findByEmail(profile.email()));

    if (existing.isPresent()) {
      UserEntity user = existing.get();
      if (user.getGoogleSub() == null || user.getGoogleSub().isBlank()) {
        user.setGoogleSub(profile.subject());
        userRepository.save(user);
      }
      String token = issueToken(user.getId());
      return Map.of(
          "ok", true,
          "registration_required", false,
          "token", token,
          "user", toPublic(user)
      );
    }

    otpRepository.consumeAllForEmail(profile.email());

    OtpChallengeEntity challenge = new OtpChallengeEntity();
    challenge.setId(UUID.randomUUID().toString());
    challenge.setEmail(profile.email());
    challenge.setCodeHash(encoder.encode("google"));
    challenge.setPurpose("signup");
    challenge.setPendingName(profile.name());
    challenge.setPendingGoogleSub(profile.subject());
    challenge.setEmailVerified(true);
    challenge.setCreatedAt(Instant.now());
    challenge.setExpiresAt(Instant.now().plus(OTP_TTL_MINUTES, ChronoUnit.MINUTES));
    otpRepository.save(challenge);

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("ok", true);
    out.put("registration_required", true);
    out.put("registration_token", challenge.getId());
    out.put("email", profile.email());
    out.put("name", profile.name());
    return out;
  }

  @Transactional
  public void logout(String authHeader) {
    String token = extractToken(authHeader);
    if (token != null) sessionRepository.deleteById(token);
  }

  /** End every session for this user (useful after account recovery). */
  @Transactional
  public Map<String, Object> logoutAll(String authHeader) {
    UserEntity user = requireUser(authHeader);
    sessionRepository.deleteByUserId(user.getId());
    return Map.of("ok", true);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> me(String authHeader) {
    UserEntity user = resolveUser(authHeader);
    if (user == null) return guest();
    return toPublic(user);
  }

  /** Returns the logged-in user or throws 401. */
  @Transactional(readOnly = true)
  public UserEntity requireUser(String authHeader) {
    UserEntity user = resolveUser(authHeader);
    if (user == null) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Please log in first");
    }
    return user;
  }

  @Transactional(readOnly = true)
  public Optional<UserEntity> optionalUser(String authHeader) {
    return Optional.ofNullable(resolveUser(authHeader));
  }

  @Transactional
  public Map<String, Object> patchMe(String authHeader, Map<String, Object> body) {
    UserEntity user = requireUser(authHeader);
    if (body.containsKey("name")) {
      String n = trim(String.valueOf(body.get("name")));
      if (!n.isEmpty()) user.setName(n);
    }
    if (body.containsKey("phone")) {
      String p = normalizePhone(String.valueOf(body.get("phone")));
      if (p.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "phone is required");
      Optional<UserEntity> other = userRepository.findByPhone(p);
      if (other.isPresent() && !other.get().getId().equals(user.getId())) {
        throw new ApiException(HttpStatus.CONFLICT, "That phone is already on another account");
      }
      user.setPhone(p);
    }
    if (body.containsKey("payment_preference") && body.get("payment_preference") instanceof Map<?, ?> pref) {
      Object key = pref.get("key");
      Object name = pref.get("name");
      if (key != null) user.setPaymentKey(String.valueOf(key));
      if (name != null) user.setPaymentName(String.valueOf(name));
    }
    userRepository.save(user);
    return toPublic(user);
  }

  private UserEntity resolveUser(String authHeader) {
    String token = extractToken(authHeader);
    if (token == null) return null;
    return sessionRepository.findByTokenAndExpiresAtAfter(token, Instant.now())
        .flatMap(s -> userRepository.findById(s.getUserId()))
        .orElse(null);
  }

  private String issueToken(String userId) {
    String token = "sw_" + UUID.randomUUID();
    AuthSessionEntity session = new AuthSessionEntity();
    session.setToken(token);
    session.setUserId(userId);
    session.setCreatedAt(Instant.now());
    session.setExpiresAt(Instant.now().plus(SESSION_DAYS, ChronoUnit.DAYS));
    sessionRepository.save(session);
    return token;
  }

  private Map<String, Object> toPublic(UserEntity user) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", user.getId());
    m.put("name", user.getName());
    m.put("phone", user.getPhone());
    if (user.getEmail() != null) m.put("email", user.getEmail());
    m.put("payment_preference", Map.of(
        "key", user.getPaymentKey() == null ? "card" : user.getPaymentKey(),
        "name", user.getPaymentName() == null ? "Debit / Credit Card" : user.getPaymentName()
    ));
    return m;
  }

  private Map<String, Object> guest() {
    Map<String, Object> anon = new LinkedHashMap<>();
    anon.put("id", "anon");
    anon.put("name", "Guest");
    anon.put("payment_preference", Map.of("key", "card", "name", "Debit / Credit Card"));
    anon.put("demo", true);
    return anon;
  }

  private static String extractToken(String auth) {
    if (auth == null || auth.isBlank()) return null;
    if (auth.startsWith("Bearer ")) return auth.substring(7).trim();
    return auth.trim();
  }

  private static String normalizePhone(String phone) {
    return trim(phone).replace(" ", "");
  }

  private static String normalizeEmail(String email) {
    return trim(email).toLowerCase();
  }

  private static String trim(String s) {
    return s == null ? "" : s.trim();
  }
}
