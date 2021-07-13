package app.oengus.service;

import app.oengus.entity.dto.MarathonBasicInfoDto;
import app.oengus.entity.dto.SelectionDto;
import app.oengus.entity.dto.UserHistoryDto;
import app.oengus.entity.dto.UserProfileDto;
import app.oengus.entity.model.*;
import app.oengus.helper.BeanHelper;
import app.oengus.requests.user.UserUpdateRequest;
import app.oengus.service.login.DiscordService;
import app.oengus.service.login.TwitchService;
import app.oengus.service.login.TwitterLoginService;
import app.oengus.service.repository.SubmissionRepositoryService;
import app.oengus.service.repository.UserRepositoryService;
import app.oengus.spring.JWTUtil;
import app.oengus.spring.model.LoginRequest;
import app.oengus.spring.model.Role;
import javassist.NotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.security.auth.login.LoginException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final DiscordService discordService;
    private final TwitterLoginService twitterLoginService;
    private final TwitchService twitchService;
    private final JWTUtil jwtUtil;
    private final UserRepositoryService userRepositoryService;
    private final SubmissionRepositoryService submissionRepositoryService;
    private final MarathonService marathonService;
    private final SelectionService selectionService;

    @Autowired
    public UserService(
        final DiscordService discordService, final TwitterLoginService twitterLoginService,
        final TwitchService twitchService, final JWTUtil jwtUtil, final UserRepositoryService userRepositoryService,
        final SubmissionRepositoryService submissionRepositoryService, final MarathonService marathonService,
        final SelectionService selectionService
    ) {
        this.discordService = discordService;
        this.twitterLoginService = twitterLoginService;
        this.twitchService = twitchService;
        this.jwtUtil = jwtUtil;
        this.userRepositoryService = userRepositoryService;
        this.submissionRepositoryService = submissionRepositoryService;
        this.marathonService = marathonService;
        this.selectionService = selectionService;
    }

    public Token login(final String host, final LoginRequest request) throws LoginException {
        final String service = request.getService();
        final String code = request.getCode();

        if ((code == null || code.isBlank()) && !service.contains("twitter")) {
            throw new LoginException("Missing code in request");
        }

        final User user;
        switch (service) {
            case "discord":
                user = this.discordService.login(code, host);
                break;
            case "twitch":
                user = this.twitchService.login(code, host);
                break;
            case "twitterAuth":
                return new Token(this.twitterLoginService.generateAuthUrlForLogin(host));
            case "twitter":
                user = this.twitterLoginService.login(request.getOauthToken(), request.getOauthVerifier());
                break;
            default:
                throw new LoginException("UNKNOWN_SERVICE");
        }

        if (!user.isEnabled()) {
            throw new LoginException("DISABLED_ACCOUNT");
        }

        return new Token(this.jwtUtil.generateToken(user));
    }

    public Object sync(final String host, final LoginRequest request) throws LoginException {
        final String service = request.getService();
        final String code = request.getCode();

        if ((code == null || code.isBlank()) && !service.contains("twitter")) {
            throw new LoginException("Missing code in request");
        }

        return switch (service) {
            case "discord" -> this.discordService.sync(code, host);
            case "twitch" -> this.twitchService.sync(code, host);
            case "twitterAuth" -> new Token(this.twitterLoginService.generateAuthUrlForSync(host));
            case "twitter" -> this.twitterLoginService.sync(request.getOauthToken(), request.getOauthVerifier());
            default -> throw new LoginException("UNKNOWN_SERVICE");
        };
    }

    public void updateRequest(final int id, final UserUpdateRequest userPatch) throws NotFoundException {
        final User user = this.userRepositoryService.findById(id);

        BeanHelper.copyProperties(userPatch, user);

        this.userRepositoryService.update(user);
    }

    @Deprecated
    public void update(final int id, final User userPatch) throws NotFoundException {
        final User user = this.userRepositoryService.findById(id);

        // overwrite the user's roles to make sure they can't set them themselves
        final List<Role> currentRoles = user.getRoles();
        userPatch.setRoles(currentRoles);

        BeanUtils.copyProperties(userPatch, user);
        this.userRepositoryService.update(user);
    }

    public void markDeleted(final int id) throws NotFoundException {
        final User user = this.userRepositoryService.findById(id);

        // TODO: delete all connections

        user.setConnections(null);
        user.setUsernameJapanese(null);
        user.setDiscordId(null);
        user.setTwitchId(null);
        user.setTwitterId(null);
        user.setMail(null);
//        user.setMail("deleted-user@oengus.io");
        user.setEnabled(false);

        final String randomHash = String.valueOf(Objects.hash(user.getUsername(), user.getId()));

        // "Deleted" is 7 in length
        // TODO: update for new limit
        user.setUsername("Deleted" + randomHash.substring(0, Math.min(7, randomHash.length())));

        this.userRepositoryService.save(user);
    }

    public void addRole(final int id, final Role role) throws NotFoundException {
        final User user = this.userRepositoryService.findById(id);

        // only update if the user does not have the role yet
        if (!user.getRoles().contains(role)) {
            user.getRoles().add(role);

            this.userRepositoryService.update(user);
        }
    }

    public void removeRole(final int id, final Role role) throws NotFoundException {
        final User user = this.userRepositoryService.findById(id);

        // only update if the user does have the role
        if (user.getRoles().contains(role)) {
            user.getRoles().remove(role);

            this.userRepositoryService.update(user);
        }
    }

    public User getUser(final int id) throws NotFoundException {
        return this.userRepositoryService.findById(id);
    }

    public UserProfileDto getUserProfile(final String username) throws NotFoundException {
        final User user = this.userRepositoryService.findByUsername(username);

        if (user == null) {
            throw new NotFoundException("Unknown user");
        }

        final UserProfileDto userProfileDto = new UserProfileDto();

        BeanUtils.copyProperties(user, userProfileDto);
        userProfileDto.setBanned(user.getRoles().contains(Role.ROLE_BANNED));
        final List<Submission> submissions = this.submissionRepositoryService.findByUser(user);
        if (submissions != null && !submissions.isEmpty()) {
            final List<Submission> filteredSubmissions = submissions.stream()
                .filter((submission) -> submission.getMarathon() != null)
                .sorted(
                    Comparator.comparing(
                        o -> ((Submission) o).getMarathon().getStartDate()
                    ).reversed()
                )
                .collect(Collectors.toList());
            final Map<Integer, SelectionDto> selections =
                this.selectionService.findAllByCategory(filteredSubmissions.stream()
                    .flatMap((submission) ->
                        submission.getGames()
                            .stream()
                            .flatMap((game) -> game.getCategories().stream())
                    )
                    .collect(Collectors.toList()));
            filteredSubmissions.forEach(submission -> {
                final UserHistoryDto userHistoryDto = new UserHistoryDto();
                if (!submission.getMarathon().getIsPrivate()) {
                    userHistoryDto.setMarathonId(submission.getMarathon().getId());
                    userHistoryDto.setMarathonName(submission.getMarathon().getName());
                    userHistoryDto.setMarathonStartDate(submission.getMarathon().getStartDate());
                    userHistoryDto.setGames(new ArrayList<>(submission.getGames()));
                    userHistoryDto.setOpponents(new ArrayList<>(submission.getOpponents()));
                    userHistoryDto.getGames()
                        .forEach(game -> {
                            game.getCategories()
                                .forEach(category -> {
                                    if (submission.getMarathon().isSelectionDone()) {
                                        category.setStatus(
                                            selections.get(category.getId()).getStatus());
                                    } else {
                                        category.setStatus(Status.TODO);
                                    }
                                });
                            game.getCategories().sort(Comparator.comparing(Category::getId));
                        });
                    userHistoryDto.getGames().sort(Comparator.comparing(Game::getId));
                    userProfileDto.getHistory().add(userHistoryDto);
                }
            });
        }

        final List<MarathonBasicInfoDto> marathons = this.marathonService.findAllMarathonsIModerate(user);

        userProfileDto.setModeratedMarathons(
            marathons.stream().filter(m -> !m.getPrivate()).collect(Collectors.toList())
        );

        return userProfileDto;
    }

    public List<User> findUsersWithUsername(final String username) {
        return this.userRepositoryService.findByUsernameContainingIgnoreCase(username);
    }

    public boolean exists(final String name) {
        return this.userRepositoryService.existsByUsername(name) || "new".equalsIgnoreCase(name) ||
            "settings".equalsIgnoreCase(name);
    }
}
