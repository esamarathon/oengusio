package app.oengus.application;

import app.oengus.application.port.persistence.MarathonPersistencePort;
import app.oengus.application.port.persistence.SubmissionPersistencePort;
import app.oengus.application.port.persistence.UserPersistencePort;
import app.oengus.application.port.security.UserSecurityPort;
import app.oengus.domain.OengusUser;
import app.oengus.adapter.rest.dto.SyncDto;
import app.oengus.adapter.rest.dto.v1.request.LoginRequest;
import app.oengus.domain.Role;
import app.oengus.domain.marathon.Marathon;
import app.oengus.domain.user.SubmissionHistoryEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.security.auth.login.LoginException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserSecurityPort securityPort;
    private final UserPersistencePort userPersistencePort;
    private final MarathonPersistencePort marathonPersistencePort;
    private final SubmissionPersistencePort submissionPersistencePort;
    private final DiscordService discordService;
    private final TwitchService twitchService;

    // TODO: move these all to the lookup service
    public Optional<OengusUser> findByUsername(final String username) {
        return this.userPersistencePort.findByUsername(username);
    }

    public List<OengusUser> searchByUsername(String username) {
        return this.userPersistencePort.findEnabledByUsername(username);
    }

    public Optional<OengusUser> findByEmail(final String email) {
        return this.userPersistencePort.findByEmail(email);
    }

    public List<SubmissionHistoryEntry> getSubmissionHistory(final int userId) {
        final Map<String, Marathon> marathonCache = new HashMap<>();

        // We need to combine some data in order for this to work!
        return this.submissionPersistencePort.findByUser(userId)
            .stream()
            .map((submission) -> {
                final var marathon = marathonCache.computeIfAbsent(
                    submission.getMarathonId(), (marathonId) -> this.marathonPersistencePort.findById(marathonId).get()
                );

                final var entry = new SubmissionHistoryEntry();

                entry.setMarathon(marathon);
                entry.setGames(submission.getGames());

                return entry;
            })
            .toList();
    }

    public List<Marathon> getModeratedHistory(final int userId) {
        return this.marathonPersistencePort.findAllModeratedBy(userId);
    }

    public boolean existsByUsername(String name) {
        return this.userPersistencePort.existsByUsername(name)
            || "new".equalsIgnoreCase(name)
            || "settings".equalsIgnoreCase(name);
    }

    public OengusUser save(final OengusUser user) {
        return this.userPersistencePort.save(user);
    }

    // TODO: test if user exists for these two?
    public void addRole(final int id, final Role role) {
        this.userPersistencePort.addRole(id, role);
    }

    public void removeRole(final int id, final Role role) {
        this.userPersistencePort.removeRole(id, role);
    }

    public void markDeleted(int userId) {
        final var user = this.userPersistencePort.getById(userId);

        if (user == null) {
            return; // TODO: return error
        }

        // TODO: delete all connections manually?

        user.getConnections().clear();
        user.setDiscordId(null);
        user.setTwitchId(null);
        user.setTwitterId(null);
        user.setEnabled(false);
        user.setEmailVerified(false);
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        user.setPassword(null);
        user.setRoles(Set.of(
            Role.ROLE_BANNED
        ));

        final String randomHash = String.valueOf(Objects.hash(user.getUsername(), user.getId()));

        // We need an email or stuff breaks, this anonymizes it.
        user.setEmail(randomHash + "@example.com");

        // "Deleted" is 7 in length
        user.setUsername(
            "Deleted" + randomHash.substring(
                0,
                Math.min(25, randomHash.length())
            )
        );
        user.setDisplayName("Deleted user");

        this.save(user);
    }

    // TODO: reimplement this when we actually do applications.
    /*public ApplicationUserInformation getApplicationInfo(User user) throws NotFoundException {
        return this.applicationUserInformationRepository.findByUser(user)
            .orElseThrow(() -> new NotFoundException("Application not found"));
    }

    public ApplicationUserInformation updateApplicationInfo(User user, ApplicationUserInformationDto dto) {
        ApplicationUserInformation infoForUser = this.applicationUserInformationRepository.findByUser(user).orElse(null);

        if (infoForUser == null) {
            infoForUser = new ApplicationUserInformation();
            infoForUser.setId(-1);
            infoForUser.setUser(user);
        }

        BeanHelper.copyProperties(dto, infoForUser);

        return this.applicationUserInformationRepository.save(infoForUser);
    }*/


    // TODO: move to auth?
    public SyncDto sync(final String host, final LoginRequest request) throws LoginException {
        final String service = request.getService();
        final String code = request.getCode();

        if (code == null || code.isBlank()) {
            throw new LoginException("Missing code in request");
        }

        return switch (service) {
            case "discord" -> this.discordService.sync(code, host);
            case "twitch" -> this.twitchService.sync(code, host);
            case "patreon" -> this.checkPatreonSync(code);
            default -> throw new LoginException("UNKNOWN_SERVICE");
        };
    }

    private SyncDto checkPatreonSync(String id) throws LoginException {
        final var user = this.userPersistencePort.findByPatreonId(id).orElseThrow(
            () -> new LoginException("User not found")
        );

        if (!Objects.equals(user.getId(), this.securityPort.getAuthenticatedUserId())) {
            throw new LoginException("ACCOUNT_ALREADY_SYNCED");
        }

        return new SyncDto(id, null);
    }
}
