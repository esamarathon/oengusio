package app.oengus.entity.dto.user;

import app.oengus.entity.constants.SocialPlatform;
import app.oengus.entity.dto.MarathonBasicInfoDto;
import app.oengus.entity.model.SocialAccount;
import app.oengus.entity.model.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class UserProfileDto {

    private int id;
    private String username;
    private String usernameJapanese;
    private boolean enabled;
    private List<SocialAccount> connections;
    private List<UserHistoryDto> history;
    private List<MarathonBasicInfoDto> moderatedMarathons;
    private List<UserApplicationHistoryDto> volunteeringHistory;
    @Nullable
    private String pronouns;
    @Nullable
    private String languagesSpoken;
    private boolean banned;
    private String country;

    public UserProfileDto() {
        this.history = new ArrayList<>();
        this.moderatedMarathons = new ArrayList<>();
        this.volunteeringHistory = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getUsernameJapanese() {
        return this.usernameJapanese;
    }

    public void setUsernameJapanese(final String usernameJapanese) {
        this.usernameJapanese = usernameJapanese;
    }

    public boolean getEnabled() {
        return this.enabled;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public List<UserHistoryDto> getHistory() {
        return this.history;
    }

    public void setHistory(final List<UserHistoryDto> history) {
        this.history = history;
    }

    public List<MarathonBasicInfoDto> getModeratedMarathons() {
        return this.moderatedMarathons;
    }

    public void setModeratedMarathons(final List<MarathonBasicInfoDto> moderatedMarathons) {
        this.moderatedMarathons = moderatedMarathons;
    }

    public List<SocialAccount> getConnections() {
        return connections;
    }

    public void setConnections(List<SocialAccount> connections) {
        this.connections = connections;
    }

    @NotNull
    public String[] getPronouns() {
        if (this.pronouns == null || this.pronouns.isBlank()) {
            return new String[0];
        }

        return this.pronouns.split(",");
    }

    public void setPronouns(@Nullable String pronouns) {
        this.pronouns = pronouns;
    }

    @Nullable
    public String[] getLanguagesSpoken() {
        if (this.languagesSpoken == null || this.languagesSpoken.isBlank()) {
            return new String[0];
        }

        return this.languagesSpoken.split(",");
    }

    public void setLanguagesSpoken(@Nullable String languagesSpoken) {
        this.languagesSpoken = languagesSpoken;
    }

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public List<UserApplicationHistoryDto> getVolunteeringHistory() {
        return volunteeringHistory;
    }

    public void setVolunteeringHistory(List<UserApplicationHistoryDto> volunteeringHistory) {
        this.volunteeringHistory = volunteeringHistory;
    }

    ///// deprecated properties
    @Deprecated
    public String getTwitterName() {
        return this.getConnections().stream()
            .filter(
                (it) -> it.getPlatform() == SocialPlatform.TWITTER
            )
            .map(SocialAccount::getUsername)
            .findFirst()
            .orElse("");
    }

    @Deprecated
    public String getDiscordName() {
        return this.getConnections().stream()
            .filter(
                (it) -> it.getPlatform() == SocialPlatform.DISCORD
            )
            .map(SocialAccount::getUsername)
            .findFirst()
            .orElse("");
    }

    @Deprecated
    public String getTwitchName() {
        return this.getConnections().stream()
            .filter(
                (it) -> it.getPlatform() == SocialPlatform.TWITCH
            )
            .map(SocialAccount::getUsername)
            .findFirst()
            .orElse("");
    }

    @Deprecated
    public String getSpeedruncomName() {
        return this.getConnections().stream()
            .filter(
                (it) -> it.getPlatform() == SocialPlatform.SPEEDRUNCOM
            )
            .map(SocialAccount::getUsername)
            .findFirst()
            .orElse("");
    }

    @JsonIgnore
    public String getUsername(final String locale) {
        if ("ja".equals(locale) && StringUtils.isNotEmpty(this.usernameJapanese)) {
            return this.usernameJapanese;
        }

        return this.username;
    }

    public static UserProfileDto fromModel(final User model) {
        return null;
    }
}
