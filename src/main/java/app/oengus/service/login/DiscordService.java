package app.oengus.service.login;

import app.oengus.api.DiscordApi;
import app.oengus.entity.constants.SocialPlatform;
import app.oengus.entity.dto.SyncDto;
import app.oengus.entity.model.SocialAccount;
import app.oengus.entity.model.User;
import app.oengus.entity.model.api.discord.DiscordUser;
import app.oengus.helper.OauthHelper;
import app.oengus.helper.PrincipalHelper;
import app.oengus.service.repository.UserRepositoryService;
import app.oengus.spring.model.AccessToken;
import app.oengus.spring.model.Role;
import app.oengus.spring.model.params.DiscordParams;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class DiscordService {

    @Autowired
    private DiscordParams discordParams;

    @Autowired
    private DiscordApi discordApi;

    @Autowired
    private UserRepositoryService userRepositoryService;

    @Value("${discord.botToken}")
    private String botToken;

    public User login(final String code, final String host) throws LoginException {
        final Map<String, String> oauthParams = OauthHelper.buildOauthMapForLogin(this.discordParams, code, host);
        final AccessToken accessToken = this.discordApi.getAccessToken(oauthParams);
        final DiscordUser discordUser = this.discordApi.getCurrentUser(
                String.join(" ", accessToken.getTokenType(), accessToken.getAccessToken()));

        User user = this.userRepositoryService.findByDiscordId(discordUser.getId());

        if (user == null) {
            user = new User();
            user.setRoles(List.of(Role.ROLE_USER));
            user.setEnabled(true);
            user.setUsername(
                    StringUtils.substring(
                        discordUser.getUsername().replace(' ', '_').replaceAll("[^\\w\\-]", ""),
                        0, 32
                    )
            );

            if (this.userRepositoryService.existsByUsername(user.getUsername())) {
                throw new LoginException("USERNAME_EXISTS");
            }

            if (StringUtils.length(user.getUsername()) < 3) {
                user.setUsername("user" + RandomUtils.nextInt(0, 999999));
            }

            final SocialAccount account = new SocialAccount();
            account.setUser(user);
            account.setPlatform(SocialPlatform.DISCORD);
            account.setUsername(discordUser.getAsTag());
            user.setConnections(List.of(account));

            user.setDiscordId(discordUser.getId());
            user = this.userRepositoryService.save(user);
        }

        return user;
    }

    public SyncDto sync(final String code, final String host) throws LoginException {
        final Map<String, String> oauthParams = OauthHelper.buildOauthMapForSync(this.discordParams, code, host);
        final AccessToken accessToken = this.discordApi.getAccessToken(oauthParams);
        final DiscordUser discordUser = this.discordApi.getCurrentUser(
                String.join(" ", accessToken.getTokenType(), accessToken.getAccessToken())
        );
        final User user = this.userRepositoryService.findByDiscordId(discordUser.getId());

        if (user != null && !Objects.equals(user.getId(), PrincipalHelper.getCurrentUser().getId())) {
            throw new LoginException("ACCOUNT_ALREADY_SYNCED");
        }

        return new SyncDto(discordUser.getId(), discordUser.getAsTag());
    }

    public DiscordUser getUser(final String id) {
        return this.discordApi.getUser(this.botToken, id);
    }
}
