package app.oengus.application;

import app.oengus.application.port.persistence.CategoryPersistencePort;
import app.oengus.application.port.persistence.MarathonPersistencePort;
import app.oengus.application.port.persistence.SubmissionPersistencePort;
import app.oengus.application.port.security.UserSecurityPort;
import app.oengus.domain.Category;
import app.oengus.domain.Marathon;
import app.oengus.domain.RunType;
import app.oengus.domain.Submission;
import app.oengus.exception.OengusBusinessException;
import app.oengus.service.GameService;
import app.oengus.service.OengusWebhookService;
import javassist.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final UserSecurityPort securityPort;
    private final GameService gameService;
    private final CategoryPersistencePort categoryPersistencePort;
    private final SubmissionPersistencePort submissionPersistencePort;
    private final MarathonPersistencePort marathonPersistencePort;
    private final OengusWebhookService webhookService;

    public static final List<RunType> MULTIPLAYER_RUN_TYPES = List.of(RunType.COOP_RACE, RunType.COOP, RunType.RACE, RunType.RELAY, RunType.RELAY_RACE);

    ///////////
    // v2 stuff

    // All parameters are given to ensure no funny business happens with people getting the wrong category for the wrong marathon.
    public List<Category> findByGameId(String marathonId, int submissionId, int gameId) {
        return this.categoryPersistencePort.findByMarathonSubmissionAndGameId(marathonId, submissionId, gameId);
    }

    ///////////
    // v1 stuff

    public Pair<Category, List<Integer>> findCategoryByCode(final String marathonId, final String code) {
        final var optionalCategory = this.categoryPersistencePort.findByCode(code);

        if (optionalCategory.isEmpty()) {
            throw new OengusBusinessException("CODE_NOT_FOUND");
        }

        final var category = optionalCategory.get();

        final var user = this.securityPort.getAuthenticatedUser();
        final Submission submission = this.submissionPersistencePort.getByGameId(category.getGameId());

        if (!Objects.equals(submission.getMarathonId(), marathonId)) {
            throw new OengusBusinessException("DIFFERENT_MARATHON");
        }

        if (!MULTIPLAYER_RUN_TYPES.contains(category.getType())) {
            throw new OengusBusinessException("NOT_MULTIPLAYER");
        }

        if (user != null) {
            if (Objects.equals(submission.getUserId(), user.getId())) {
                throw new OengusBusinessException("SAME_USER");
            }

            if (category.getOpponents()
                .stream()
                .map(opponent -> opponent.getSubmission().getUserId())
                .anyMatch(userId -> userId == user.getId())) {
                throw new OengusBusinessException("ALREADY_IN_OPPONENTS");
            }
        }
        final List<Integer> userIds = new ArrayList<>();

        userIds.add(submission.getUserId());
        userIds.addAll(category
            .getOpponents()
            .stream()
            .map(opponent -> opponent.getSubmission().getUserId())
            .collect(Collectors.toSet()));

        final Marathon marathon = this.marathonPersistencePort.findById(submission.getMarathonId())
            .orElseThrow(
                () -> new OengusBusinessException("SUBMISSION_MARATHON_MISSING")
            );

        if (marathon.getMaxNumberOfScreens() <= userIds.size()) {
            throw new OengusBusinessException("MAX_SIZE_REACHED");
        }

        return Pair.of(
            category, userIds
        );
    }

    public void delete(String marathonId, final int id) throws NotFoundException {
        final var optionalCategory = this.categoryPersistencePort.findById(id);

        if (optionalCategory.isEmpty()) {
            throw new NotFoundException("CATEGORY_NOT_FOUND");
        }

        final var category = optionalCategory.get();
        final var gameCategories = this.categoryPersistencePort.findByGameId(category.getGameId());
        final var deletedBy = this.securityPort.getAuthenticatedUser();

        // only have one category, delete the game
        if (gameCategories.size() == 1) {
            this.gameService.delete(category.getGameId(), deletedBy);
            return;
        }

        final var marathon = this.marathonPersistencePort.findById(marathonId)
            .orElseThrow(() -> new NotFoundException("MARATHON_NOT_FOUND"));
        final String webhook = marathon.getWebhook();

        if (StringUtils.isNotEmpty(webhook)) {
            try {
                this.webhookService.sendCategoryDeleteEvent(webhook, category, deletedBy);
            } catch (Exception e) {
                LoggerFactory.getLogger(GameService.class).error("Error when handling webhook", e);
            }
        }

        this.categoryPersistencePort.delete(category);
    }

}
