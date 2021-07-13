package app.oengus.service.login;

import app.oengus.entity.constants.SocialPlatform;
import app.oengus.entity.dto.SyncDto;
import app.oengus.entity.model.SocialAccount;
import app.oengus.entity.model.User;
import app.oengus.exception.OengusBusinessException;
import app.oengus.helper.PrincipalHelper;
import app.oengus.service.repository.UserRepositoryService;
import app.oengus.spring.model.Role;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

import javax.security.auth.login.LoginException;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Objects;

@Service
public class TwitterLoginService {
    private final UserRepositoryService userRepositoryService;

    // TODO: move these values to the constructor
    @Value("${twitter.oauth.consumerKey}")
    private String consumerKey;

    @Value("${twitter.oauth.consumerSecret}")
    private String consumerSecret;

    @Value("${twitter.loginCallback}")
    private String loginCallback;

    @Value("${twitter.syncCallback}")
    private String syncCallback;

    private TwitterFactory twitterFactory;

    @Autowired
    public TwitterLoginService(UserRepositoryService userRepositoryService) {
        this.userRepositoryService = userRepositoryService;
    }

    private Twitter getTwitter() {
        // always return a new instance because it has state :/
        return this.getTwitterFactory().getInstance();
    }

    private TwitterFactory getTwitterFactory() {
        if (this.twitterFactory == null) {
            final ConfigurationBuilder cb = new ConfigurationBuilder();
            cb.setOAuthConsumerKey(this.consumerKey)
                .setOAuthConsumerSecret(this.consumerSecret)
                .setDebugEnabled(false);

            this.twitterFactory = new TwitterFactory(cb.build());
        }

        return this.twitterFactory;
    }

    public String generateAuthUrlForLogin(final String host) {
        try {
            final RequestToken requestToken = this.getTwitter().getOAuthRequestToken(host + this.loginCallback);
            return requestToken.getAuthenticationURL();
        } catch (final TwitterException e) {
            throw new OengusBusinessException("TWITTER_ERROR", e);
        }
    }

    public String generateAuthUrlForSync(final String host) {
        try {
            final RequestToken requestToken = this.getTwitter().getOAuthRequestToken(host + this.syncCallback);
            return requestToken.getAuthenticationURL();
        } catch (final TwitterException e) {
            throw new OengusBusinessException("TWITTER_ERROR", e);
        }
    }

    @Transactional
    public User login(final String oauthToken, final String oauthVerifier) throws LoginException {
        try {
            final AccessToken accessToken = this.getTwitter().getOAuthAccessToken(new RequestToken(oauthToken, ""), oauthVerifier);
            final twitter4j.User twitterUser = this.getTwitterFactory().getInstance(accessToken).verifyCredentials();
            User user = this.userRepositoryService.findByTwitterId(Long.toString(twitterUser.getId()));
            if (user == null) {
                user = new User();
                user.setRoles(List.of(Role.ROLE_USER));
                user.setEnabled(true);
                user.setUsername(twitterUser.getScreenName());

                if (this.userRepositoryService.existsByUsername(user.getUsername())) {
                    throw new LoginException("USERNAME_EXISTS");
                }

                if (StringUtils.length(user.getUsername()) < 3) {
                    user.setUsername("user" + RandomUtils.nextInt(0, 999999));
                }

                final SocialAccount account = new SocialAccount();
                account.setUser(user);
                account.setPlatform(SocialPlatform.TWITTER);
                account.setUsername(twitterUser.getScreenName());
                user.setConnections(List.of(account));

                user.setTwitterId(Long.toString(twitterUser.getId()));
                user = this.userRepositoryService.save(user);
            }

            return user;
        } catch (final TwitterException e) {
            throw new OengusBusinessException("TWITTER_ERROR", e);
        }
    }

    public SyncDto sync(final String oauthToken, final String oauthVerifier) throws LoginException {
        try {
            final AccessToken accessToken = this.getTwitter().getOAuthAccessToken(new RequestToken(oauthToken, ""), oauthVerifier);
            final twitter4j.User twitterUser = this.getTwitterFactory().getInstance(accessToken).verifyCredentials();
            final User user = this.userRepositoryService.findByTwitterId(Long.toString(twitterUser.getId()));
            if (user != null && !Objects.equals(user.getId(), PrincipalHelper.getCurrentUser().getId())) {
                throw new LoginException("ACCOUNT_ALREADY_SYNCED");
            }

            return new SyncDto(Long.toString(twitterUser.getId()), twitterUser.getScreenName());
        } catch (final TwitterException e) {
            throw new OengusBusinessException("TWITTER_ERROR", e);
        }
    }

}
