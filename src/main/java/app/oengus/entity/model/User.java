package app.oengus.entity.model;

import app.oengus.spring.model.Role;
import app.oengus.spring.model.Views;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonView;
import org.hibernate.annotations.Cache;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static app.oengus.requests.user.IUserRequest.SPEEDRUN_COM_NAME_REGEX;
import static app.oengus.requests.user.IUserRequest.USERNAME_REGEX;

@Entity
@Table(name = "users")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(Views.Public.class)
    private int id;

    @Column
    @JsonView(Views.Public.class)
    @Size(min = 3, max = 32)
    @Pattern(regexp = USERNAME_REGEX)
    private String username;

    @Column(name = "username_ja")
    @JsonView(Views.Public.class)
    @Size(max = 32)
    private String usernameJapanese;

    @Column(name = "active")
    @JsonView(Views.Public.class)
    private boolean enabled;

    @ElementCollection
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @Column(name = "role")
    @JsonView(Views.Public.class)
    private List<Role> roles;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @OrderBy("platform ASC")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonView(Views.Public.class)
    private List<SocialAccount> connections;

    @Column
    @JsonView(Views.Internal.class)
    @Email
    private String mail;

    @Column(name = "discord_id")
    @JsonView(Views.Internal.class)
    private String discordId;

    @Column(name = "twitch_id")
    @JsonView(Views.Internal.class)
    private String twitchId;

    @Column(name = "twitter_id")
    @JsonView(Views.Internal.class)
    private String twitterId;

    @Column(name = "patreon_id")
    @JsonView(Views.Internal.class)
    private String patreonId;

    @Column(name = "pronouns")
    @JsonView(Views.Public.class)
    @Size(max = 20)
    private String pronouns;

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        final List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        for (final Role r : this.roles) {
            authorities.add(new SimpleGrantedAuthority(r.toString()));
        }
        return authorities;
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return false;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return false;
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return false;
    }

    @AssertTrue
    public boolean isAtLeastOneAccountSynchronized() {
        // ignore for disabled users
        if (!this.enabled) {
            return true;
        }

        return StringUtils.isNotEmpty(this.discordId) ||
            StringUtils.isNotEmpty(this.twitchId) ||
            StringUtils.isNotEmpty(this.twitterId);
    }

    @AssertTrue
    public boolean isEmailPresentForExistingUser() {
        if (this.id > 0 && this.enabled) {
            return StringUtils.isNotEmpty(this.mail);
        }

        return true;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public List<Role> getRoles() {
        return this.roles;
    }

    public void setRoles(final List<Role> roles) {
        this.roles = roles;
    }

    public int getId() {
        return this.id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public String getMail() {
        return this.mail;
    }

    public void setMail(final String mail) {
        this.mail = mail;
    }

    public String getDiscordId() {
        return this.discordId;
    }

    public void setDiscordId(final String discordId) {
        this.discordId = discordId;
    }

    public String getTwitchId() {
        return this.twitchId;
    }

    public void setTwitchId(final String twitchId) {
        this.twitchId = twitchId;
    }

    public List<SocialAccount> getConnections() {
        return connections;
    }

    public void setConnections(List<SocialAccount> connections) {
        this.connections = connections;
    }

    public String getUsernameJapanese() {
        return this.usernameJapanese;
    }

    public void setUsernameJapanese(final String usernameJapanese) {
        this.usernameJapanese = usernameJapanese;
    }

    public String getTwitterId() {
        return this.twitterId;
    }

    public void setTwitterId(final String twitterId) {
        this.twitterId = twitterId;
    }

    @JsonIgnore
    public String getUsername(final String locale) {
        if ("ja".equals(locale) && StringUtils.isNotEmpty(this.usernameJapanese)) {
            return this.usernameJapanese;
        }
        return this.username;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id == user.id && username.equals(user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username);
    }
}
