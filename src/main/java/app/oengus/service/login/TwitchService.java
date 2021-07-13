package app.oengus.service.login;

import app.oengus.api.TwitchApi;
import app.oengus.api.TwitchOauthApi;
import app.oengus.entity.constants.SocialPlatform;
import app.oengus.entity.dto.SyncDto;
import app.oengus.entity.model.SocialAccount;
import app.oengus.entity.model.User;
import app.oengus.entity.model.api.TwitchUser;
import app.oengus.helper.OauthHelper;
import app.oengus.helper.PrincipalHelper;
import app.oengus.service.repository.UserRepositoryService;
import app.oengus.spring.model.AccessToken;
import app.oengus.spring.model.Role;
import app.oengus.spring.model.params.TwitchLoginParams;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.security.auth.login.LoginException;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class TwitchService {
    private final TwitchLoginParams twitchLoginParams;
    private final TwitchOauthApi twitchOauthApi;
    private final TwitchApi twitchApi;
    private final UserRepositoryService userRepositoryService;

    @Autowired
    public TwitchService(TwitchLoginParams twitchLoginParams, TwitchOauthApi twitchOauthApi, TwitchApi twitchApi, UserRepositoryService userRepositoryService) {
        this.twitchLoginParams = twitchLoginParams;
        this.twitchOauthApi = twitchOauthApi;
        this.twitchApi = twitchApi;
        this.userRepositoryService = userRepositoryService;
    }

    @Transactional
    public User login(final String code, final String host) throws LoginException {
        final Map<String, String> oauthParams = OauthHelper.buildOauthMapForLogin(this.twitchLoginParams, code, host);
        final AccessToken accessToken = this.twitchOauthApi.getAccessToken(oauthParams);
        final TwitchUser twitchUser = this.twitchApi.getCurrentUser(
                String.join(" ", StringUtils.capitalize(accessToken.getTokenType()), accessToken.getAccessToken()),
                this.twitchLoginParams.getClientId())
                                                    .getData()
                                                    .get(0);

        User user = this.userRepositoryService.findByTwitchId(twitchUser.getId());

        if (user == null) {
            user = new User();
            user.setRoles(List.of(Role.ROLE_USER));
            user.setEnabled(true);
            user.setUsername(StringUtils.substring(twitchUser.getLogin(), 0, 32));

            if (this.userRepositoryService.existsByUsername(user.getUsername())) {
                throw new LoginException("USERNAME_EXISTS");
            }

            if (StringUtils.length(user.getUsername()) < 3) {
                user.setUsername("user" + RandomUtils.nextInt(0, 999999));
            }

            final SocialAccount account = new SocialAccount();
            account.setUser(user);
            account.setPlatform(SocialPlatform.TWITCH);
            account.setUsername(twitchUser.getLogin());
            user.setConnections(List.of(account));

            user.setTwitchId(twitchUser.getId());
            user = this.userRepositoryService.save(user);
        }

        return user;
    }

    @Transactional
    public SyncDto sync(final String code, final String host) throws LoginException {
        final Map<String, String> oauthParams = OauthHelper.buildOauthMapForSync(this.twitchLoginParams, code, host);
        final AccessToken accessToken = this.twitchOauthApi.getAccessToken(oauthParams);
        final TwitchUser twitchUser = this.twitchApi.getCurrentUser(
            String.join(" ", StringUtils.capitalize(accessToken.getTokenType()), accessToken.getAccessToken()),
            this.twitchLoginParams.getClientId())
            .getData().get(0);

        final User user = this.userRepositoryService.findByTwitchId(twitchUser.getId());
        if (user != null && !Objects.equals(user.getId(), PrincipalHelper.getCurrentUser().getId())) {
            throw new LoginException("ACCOUNT_ALREADY_SYNCED");
        }
        return new SyncDto(twitchUser.getId(), twitchUser.getLogin());
    }
}
