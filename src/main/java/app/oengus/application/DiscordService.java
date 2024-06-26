package app.oengus.application;

import app.oengus.application.api.DiscordApi;
import app.oengus.application.api.DiscordOauthApi;
import app.oengus.domain.exception.auth.UnknownUserException;
import app.oengus.application.port.persistence.UserPersistencePort;
import app.oengus.application.port.security.UserSecurityPort;
import app.oengus.domain.OengusUser;
import app.oengus.adapter.rest.dto.SyncDto;
import app.oengus.domain.api.discord.DiscordGuild;
import app.oengus.domain.api.discord.DiscordInvite;
import app.oengus.domain.api.discord.DiscordMember;
import app.oengus.domain.api.discord.DiscordUser;
import app.oengus.application.helper.OauthHelper;
import app.oengus.domain.AccessToken;
import app.oengus.configuration.params.DiscordParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.security.auth.login.LoginException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DiscordService {
    private final DiscordParams discordParams;
    private final DiscordApi discordApi;
    private final DiscordOauthApi discordOauthApi;
    private final UserPersistencePort userPersistencePort;
    private final UserSecurityPort securityPort;

    @Value("${discord.botToken}")
    private String botToken;

    public DiscordInvite fetchInvite(final String inviteCode) {
        return this.discordApi.getInvite(this.botToken, inviteCode);
    }

    public DiscordGuild getGuildById(final String guildId) {
        return this.discordApi.getGuild(this.botToken, guildId);
    }

    public DiscordMember getMemberById(final String guildId, final String userId) {
        return this.discordApi.getGuildMember(this.botToken, guildId, userId);
    }

    public OengusUser login(final String code, final String baseUrl) {
        final Map<String, String> oauthParams = OauthHelper.buildOauthMapForLogin(this.discordParams, code, baseUrl);
        final AccessToken accessToken = this.discordOauthApi.getAccessToken(oauthParams);
        final DiscordUser discordUser = this.discordApi.getCurrentUser(
            String.join(" ", accessToken.getTokenType(), accessToken.getAccessToken()));

        final Optional<OengusUser> user = this.userPersistencePort.findByDiscordId(discordUser.getId());

        if (user.isEmpty()) {
            throw new UnknownUserException(
                discordUser.getUsername()
            );
        }

        return user.get();
    }

    public SyncDto sync(final String code, final String host) throws LoginException {
        final Map<String, String> oauthParams = OauthHelper.buildOauthMapForSync(this.discordParams, code, host);
        final AccessToken accessToken = this.discordOauthApi.getAccessToken(oauthParams);
        final DiscordUser discordUser = this.discordApi.getCurrentUser(
            String.join(" ", accessToken.getTokenType(), accessToken.getAccessToken())
        );
        final var optionalUser = this.userPersistencePort.findByDiscordId(discordUser.getId());
        final var authUserId = this.securityPort.getAuthenticatedUserId();

        if (optionalUser.isPresent() && !Objects.equals(optionalUser.get().getId(), authUserId)) {
            throw new LoginException("ACCOUNT_ALREADY_SYNCED");
        }

        return new SyncDto(discordUser.getId(), discordUser.getAsTag());
    }

    public DiscordUser getUserById(final String id) {
        return this.discordApi.getUser(this.botToken, id);
    }
}
