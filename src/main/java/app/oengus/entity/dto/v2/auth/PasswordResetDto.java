package app.oengus.entity.dto.v2.auth;

import app.oengus.validation.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@Schema(description = "The data we need to reset a user's password.'")
public class PasswordResetDto {
    @NotBlank
    private String token;

    @ValidPassword
    private String password;
}
