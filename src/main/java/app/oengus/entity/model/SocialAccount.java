package app.oengus.entity.model;

import app.oengus.entity.constants.SocialPlatform;
import com.fasterxml.jackson.annotation.JsonBackReference;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.Cache;

import java.io.Serializable;

@SuppressWarnings("unused")
@Entity
@Table(name = "social_accounts")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class SocialAccount implements Serializable {
    @Id
    @NotNull
    @ManyToOne
    @JsonBackReference
    @JoinColumn(name = "user_id")
    private User user;

    @Id
    @NotNull
    @Column(name = "platform")
    @Enumerated(EnumType.STRING) /* Default is ORDINAL */
    private SocialPlatform platform;

    @NotNull
    @Size(max = 320)
    @Column(name = "username")
    private String username;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public SocialPlatform getPlatform() {
        return platform;
    }

    public void setPlatform(SocialPlatform platform) {
        this.platform = platform;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
