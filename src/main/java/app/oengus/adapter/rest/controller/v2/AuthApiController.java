package app.oengus.adapter.rest.controller.v2;

import app.oengus.adapter.rest.dto.BooleanStatusDto;
import app.oengus.adapter.rest.dto.v1.request.LoginRequest;
import app.oengus.adapter.rest.dto.v2.auth.*;
import app.oengus.adapter.rest.mapper.AuthMapper;
import app.oengus.application.AuthService;
import app.oengus.application.TOTPService;
import app.oengus.application.UserService;
import app.oengus.application.port.persistence.EmailVerificationPersistencePort;
import app.oengus.application.port.security.JWTPort;
import app.oengus.application.port.security.UserSecurityPort;
import app.oengus.configuration.OengusConfiguration;
import com.google.zxing.WriterException;
import javassist.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.io.IOException;

// TODO: cleanup
@RestController
@RequiredArgsConstructor
public class AuthApiController implements AuthApi {
    private final AuthMapper authMapper;
    private final UserSecurityPort securityPort;

    private final TOTPService totpService;
    private final AuthService authService;
    private final UserService userService;
    private final EmailVerificationPersistencePort emailVerificationPersistencePort;
    private final JWTPort jwtPort;
    private final OengusConfiguration configuration;

    @Override
    public ResponseEntity<LoginResponseDto> login(@Valid LoginDto body) {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                this.authService.login(body)
            );
    }

    @Override
    public ResponseEntity<LoginResponseDto> loginWithProvider(LoginRequest body) {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                this.authService.loginWithService(body.getService(), body.getCode(), this.configuration.getBaseUrl())
            );
    }

    @Override
    public ResponseEntity<SignupResponseDto> signUp(@Valid SignUpDto body) {
        final var newUser = this.authMapper.toDomain(body);
        final var signupStatus = this.authService.signUp(newUser);
        final var httpStatus = signupStatus == SignupResponseDto.Status.SIGNUP_SUCCESS
            ? HttpStatus.OK
            : HttpStatus.UNPROCESSABLE_ENTITY;

        return ResponseEntity
            .status(httpStatus)
            .body(
                new SignupResponseDto().setStatus(signupStatus)
            );
    }

    @Override
    public ResponseEntity<LoginResponseDto> refreshUserToken() {
        final var user = this.securityPort.getAuthenticatedUser();
        final var newToken = this.jwtPort.generateToken(user);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                new LoginResponseDto()
                    .setStatus(LoginResponseDto.Status.LOGIN_SUCCESS)
                    .setToken(newToken)
            );
    }

    @Override
    public ResponseEntity<?> verifyEmail(final String hash) throws NotFoundException {
        final var verification = this.emailVerificationPersistencePort.findByHash(hash)
            .orElseThrow(() -> new NotFoundException("Email verification not found."));
       final var user = verification.user();

       user.setEmailVerified(true);

       this.userService.save(user);
       this.emailVerificationPersistencePort.delete(verification);

        return ResponseEntity.ok()
            .body("<h1>Thank you for verifying your email address.</h1><p>If you haven't already, you can <a href=\"https://oengus.io/\">log-in on Oengus</a></p>");
    }

    @Override
    public ResponseEntity<BooleanStatusDto> requestNewEmailVerification() {
        final var user = this.securityPort.getAuthenticatedUser();

        if (user.isEmailVerified()) {
            return ResponseEntity
              .status(HttpStatus.BAD_REQUEST)
              .body(new BooleanStatusDto(false));
        }

        this.authService.sendNewVerificationEmail(user);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(new BooleanStatusDto(true));
    }

    @Override
    public ResponseEntity<InitMFADto> initMFA() throws IOException, WriterException {
        final var databaseUser = this.securityPort.getAuthenticatedUser();
        final String newSecret = this.totpService.generateSecretKey();

        // Reset mfa for the user.
        databaseUser.setMfaEnabled(false);
        databaseUser.setMfaSecret(newSecret);

        this.userService.save(databaseUser);

        final String qrUrl = this.totpService.getGoogleAuthenticatorQRCode(newSecret, databaseUser.getUsername());

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                new InitMFADto()
                    .setSecretKey(newSecret)
                    .setQrCode(
                        "data:image/png;base64," + this.totpService.createQRCodeBase64(qrUrl, 500, 500)
                    )
            );
    }

    @Override
    public ResponseEntity<BooleanStatusDto> verifyAndStoreMFA(final String code) {
        final var databaseUser = this.securityPort.getAuthenticatedUser();
        final String mfaSecret = databaseUser.getMfaSecret();

        if (databaseUser.isMfaEnabled() || StringUtils.isBlank(mfaSecret)) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new BooleanStatusDto(false));
        }

        final String serverCode = this.totpService.getTOTPCode(mfaSecret);

        // In case of an invalid code, abort.
        if (!serverCode.equals(code)) {
            return ResponseEntity
               .status(HttpStatus.OK)
               .body(new BooleanStatusDto(false));
        }

        databaseUser.setMfaEnabled(true);

        this.userService.save(databaseUser);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(new BooleanStatusDto(true));
    }

    @Override
    public ResponseEntity<BooleanStatusDto> removeMFA(String code) {
        final var databaseUser = this.securityPort.getAuthenticatedUser();
        final String mfaSecret = databaseUser.getMfaSecret();

        if (!databaseUser.isMfaEnabled() || StringUtils.isBlank(mfaSecret)) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new BooleanStatusDto(false));
        }
        final String serverCode = this.totpService.getTOTPCode(mfaSecret);

        // In case of an invalid code, abort.
        if (!serverCode.equals(code)) {
            return ResponseEntity
                .status(HttpStatus.OK)
                .body(new BooleanStatusDto(false));
        }

        databaseUser.setMfaEnabled(false);
        databaseUser.setMfaSecret(null);

        this.userService.save(databaseUser);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(new BooleanStatusDto(true));
    }

    @Override
    public ResponseEntity<PasswordResetResponseDto> requestPasswordReset(PasswordResetRequestDto body) {
        final var resetResponse = this.authService.initPasswordReset(body);

        if (resetResponse == PasswordResetResponseDto.Status.PASSWORD_RESET_SENT) {
            return ResponseEntity.ok(new PasswordResetResponseDto(resetResponse));
        }

        return ResponseEntity.badRequest().body(new PasswordResetResponseDto(resetResponse));
    }

    @Override
    public ResponseEntity<PasswordResetResponseDto> completePasswordReset(PasswordResetDto body) {
        final var res = this.authService.resetUserPassword(body);

        if (res == PasswordResetResponseDto.Status.PASSWORD_RESET_SUCCESS) {
            return ResponseEntity.ok(new PasswordResetResponseDto(res));
        }

        return ResponseEntity.badRequest().body(new PasswordResetResponseDto(res));
    }
}
