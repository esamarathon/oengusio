package app.oengus.requests.user;

import app.oengus.entity.model.SocialAccount;
import app.oengus.spring.model.Views;
import com.fasterxml.jackson.annotation.JsonView;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

public class UserUpdateRequest implements IUserRequest {

    @JsonView(Views.Public.class)
    @Size(min = 3, max = 32)
    @Pattern(regexp = USERNAME_REGEX)
    private String username;

    @JsonView(Views.Public.class)
    @Size(max = 32)
    private String usernameJapanese;

    @JsonView(Views.Public.class)
    private boolean enabled;

    @JsonView(Views.Internal.class)
    @Email
    private String mail;

    @Nullable
    @JsonView(Views.Internal.class)
    private String discordId;

    @Nullable
    @JsonView(Views.Internal.class)
    private String twitchId;

    @Nullable
    @JsonView(Views.Internal.class)
    private String twitterId;

    @Nullable
    @JsonView(Views.Internal.class)
    private String patreonId;

    @NotNull
    @JsonView(Views.Public.class)
    private List<SocialAccount> connections;

    @Nullable
    @JsonView(Views.Public.class)
    @Size(max = 37)
    private String discordName;

    @Nullable
    @JsonView(Views.Public.class)
    @Size(max = 15)
    private String twitterName;

    @Nullable
    @JsonView(Views.Public.class)
    @Size(max = 25)
    private String twitchName;

    @Nullable
    @JsonView(Views.Public.class)
    @Size(max = 20)
    @Pattern(regexp = SPEEDRUN_COM_NAME_REGEX)
    private String speedruncomName;

    @Nullable
    @Column(name = "pronouns")
    @JsonView(Views.Public.class)
    @Size(max = 20)
    private String pronouns;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsernameJapanese() {
        return usernameJapanese;
    }

    public void setUsernameJapanese(String usernameJapanese) {
        this.usernameJapanese = usernameJapanese;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    @Nullable
    public String getDiscordId() {
        return discordId;
    }

    public void setDiscordId(@Nullable String discordId) {
        this.discordId = discordId;
    }

    @Nullable
    public String getTwitchId() {
        return twitchId;
    }

    public void setTwitchId(@Nullable String twitchId) {
        this.twitchId = twitchId;
    }

    @Nullable
    public String getTwitterId() {
        return twitterId;
    }

    public void setTwitterId(@Nullable String twitterId) {
        this.twitterId = twitterId;
    }

    @Nullable
    public String getPatreonId() {
        return patreonId;
    }

    public void setPatreonId(@Nullable String patreonId) {
        this.patreonId = patreonId;
    }

    @Nullable
    public String getDiscordName() {
        return discordName;
    }

    public void setDiscordName(@Nullable String discordName) {
        this.discordName = discordName;
    }

    @Nullable
    public String getTwitterName() {
        return twitterName;
    }

    public void setTwitterName(@Nullable String twitterName) {
        this.twitterName = twitterName;
    }

    @Nullable
    public String getTwitchName() {
        return twitchName;
    }

    public void setTwitchName(@Nullable String twitchName) {
        this.twitchName = twitchName;
    }

    @Nullable
    public String getSpeedruncomName() {
        return speedruncomName;
    }

    public void setSpeedruncomName(@Nullable String speedruncomName) {
        this.speedruncomName = speedruncomName;
    }

    @Nullable
    public String getPronouns() {
        return pronouns;
    }

    public void setPronouns(@Nullable String pronouns) {
        this.pronouns = pronouns;
    }

    public List<SocialAccount> getConnections() {
        return connections;
    }

    public void setConnections(List<SocialAccount> connections) {
        this.connections = connections;
    }
}
