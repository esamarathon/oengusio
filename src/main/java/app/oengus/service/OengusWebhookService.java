package app.oengus.service;

import app.oengus.entity.model.*;
import app.oengus.helper.TimeHelpers;
import app.oengus.spring.model.Views;
import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class OengusWebhookService {

    private final OkHttpClient client = new OkHttpClient();
    @Autowired
    private ObjectMapper mapper;

    @Value("${oengus.baseUrl}")
    private String baseUrl;

    @Autowired
    private DiscordApiService jda;

    @Autowired
    private MarathonService marathonService;

    public void sendDonationEvent(final String url, final Donation donation) throws IOException {
        if (handleOnBot(url, null, null, donation, null)) {
            return;
        }

        final JsonNode data = mapper.createObjectNode()
            .put("event", "DONATION")
            .set("donation", mapper.valueToTree(donation));

        callAsync(url, data);
    }

    public void sendNewSubmissionEvent(final String url, final Submission submission) throws IOException {
        if (handleOnBot(url, submission, null, null, null)) {
            return;
        }

        final JsonNode data = mapper.createObjectNode()
            .put("event", "SUBMISSION_ADD")
            .set("submission", parseJson(submission));

        callAsync(url, data);
    }

    public void sendSubmissionUpdateEvent(final String url, final Submission newSubmission, final Submission oldSubmission) throws IOException {
        if (handleOnBot(url, newSubmission, oldSubmission, null, null)) {
            return;
        }

        final ObjectNode data = mapper.createObjectNode().put("event", "SUBMISSION_EDIT");
        data.set("submission", parseJson(newSubmission));
        data.set("original_submission", parseJson(oldSubmission));

        callAsync(url, data);
    }

    public void sendSubmissionDeleteEvent(final String url, final Submission submission, final User deletedBy) throws IOException {
        if (handleOnBot(url, null, submission, null, deletedBy)) {
            return;
        }

        final ObjectNode data = mapper.createObjectNode()
            .put("event", "SUBMISSION_DELETE");
        data.set("submission", parseJson(submission));
        data.set("deleted_by", parseJson(deletedBy));

        callAsync(url, data);
    }

    public boolean sendPingEvent(final String url) {
        try {
            final JsonNode data = mapper.createObjectNode().put("event", "PING");
            final RequestBody body = RequestBody.create(null, mapper.writeValueAsBytes(data));
            final Request request = new Request.Builder()
                .header("User-Agent", "oengus.io webhook")
                .header("Content-Type", "application/json")
                .url(url)
                .post(body)
                .build();

            try (final Response response = this.client.newCall(request).execute()) {
                return response.isSuccessful();
            }
            // IllegalArgumentException in case of an invalid url
        } catch (IOException | IllegalArgumentException ignored) {
            return false;
        }
    }

    private JsonNode parseJson(final Object submission) throws IOException {
        // hacky work around so we can use views
        final String json = mapper.writerWithView(Views.Public.class).writeValueAsString(submission);

        return mapper.readTree(json);
    }

    private boolean handleOnBot(final String rawUrl, Submission submission, Submission oldSubmission, Donation donation, User user) {
        if (!rawUrl.startsWith("oengus-bot")) {
            return false;
        }

        // parse the url
        final OengusBotUrl url = new OengusBotUrl(rawUrl);

        if (url.isEmpty()) {
            // still returning true since oengus-bot is no valid domain
            return true;
        }

        final String marathon = url.get("marathon");

        if (oldSubmission != null && url.has("editsub")) {
            if (submission == null) {
                sendSubmissionDelete(
                    marathon,
                    url.get("editsub"),
                    oldSubmission,
                    user
                );
                return true;
            }

            sendEditSubmission(
                marathon,
                url.get("editsub"),
                // get the new submission channel for when there's a new game added, or get the edit channel
                url.has("newsub") ? url.get("newsub") : url.get("editsub"),
                submission,
                oldSubmission
            );
        } else if (submission != null && url.has("newsub")) {
            final String marathonName = this.marathonService.getNameForCode(marathon);

            sendNewSubmission(
                marathon,
                url.get("newsub"),
                submission,
                marathonName
            );

            if (url.has("editsub")) {
                sendNewSubmission(
                    marathon,
                    url.get("editsub"),
                    submission,
                    marathonName
                );
            }
        } else if (url.has("donation")) {
            sendDonationEvent(
                marathon,
                url.get("donation"),
                donation
            );
        }

        return true;
    }

    private void callAsync(final String url, final JsonNode data) throws IOException {
        final RequestBody body = RequestBody.create(null, mapper.writeValueAsBytes(data));
        final Request request = new Request.Builder()
            .header("User-Agent", "oengus.io webhook")
            .header("Content-Type", "application/json")
            .url(url)
            .post(body)
            .build();

        this.client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@Nonnull Call call, @Nonnull IOException e) {
                //
            }

            @Override
            public void onResponse(@Nonnull Call call, @Nonnull Response response) {
                response.close();
            }
        });
    }

    private void sendEditSubmission(final String marathon, final String channel, final String newSubChannel,
                                    final Submission submission, final Submission oldSubmission) {
        final String marathonName = this.marathonService.getNameForCode(marathon);

        // if the submissions is the same, ignore it
        if (Objects.equals(submission, oldSubmission)) {
            return;
        }

        for (final Game newGame : submission.getGames()) {
            final Game oldGame = oldSubmission.getGames()
                .stream()
                .filter((g) -> g.getId().equals(newGame.getId()))
                .findFirst()
                .orElse(null);

            // The game was just added
            if (oldGame == null) {
                sendNewGame(marathon, newSubChannel, newGame, marathonName);
                sendNewGame(marathon, channel, newGame, marathonName);
                continue;
            }

            // ignore the game if they are equal
            if (Objects.equals(newGame, oldGame)) {
                continue;
            }

            for (final Category newCategory : newGame.getCategories()) {
                final Category oldCategory = oldGame.getCategories()
                    .stream()
                    .filter((c) -> c.getId().equals(newCategory.getId()))
                    .findFirst()
                    .orElse(null);

                if (oldCategory == null) {
                    sendNewCategory(marathon, newSubChannel, newCategory, marathonName);
                    sendNewCategory(marathon, channel, newCategory, marathonName);
                    continue;
                }

                // ignore the category if they are equal
                if (Objects.equals(newCategory, oldCategory)) {
                    continue;
                }

                sendUpdatedCategory(marathon, channel, newCategory, oldCategory, marathonName);
            }
        }
    }

    private void sendNewSubmission(final String marathon, final String channel, final Submission submission, final String marathonName) {
        for (final Game game : submission.getGames()) {
            sendNewGame(marathon, channel, game, marathonName);
        }
    }

    // send all categories
    private void sendNewGame(final String marathon, final String channel, final Game newGame, final String marathonName) {
        for (final Category category : newGame.getCategories()) {
            sendNewCategory(marathon, channel, category, marathonName);
        }
    }

    private void sendNewCategory(final String marathon, final String channel, final Category category, final String marathonName) {
        final Game game = category.getGame();
        final String username = game.getSubmission().getUser().getUsername();

        final WebhookEmbedBuilder builder = new WebhookEmbedBuilder()
            .setTitle(new WebhookEmbed.EmbedTitle(
                username + " submitted a run to " + marathonName,
                this.baseUrl + "/marathon/" + marathon + "/submissions"
            ))
            .setDescription(String.format(
                "**Game:** %s\n**Category:** %s\n**Platform:** %s\n**Estimate:** %s",
                game.getName(),
                category.getName(),
                game.getConsole(),
                TimeHelpers.formatDuration(category.getEstimate())
            ));

        this.jda.sendMessage(channel, builder.build());
    }

    private void sendUpdatedCategory(final String marathon, final String channel, final Category category, final Category oldCategory, final String marathonName) {
        final Game game = category.getGame();
        final Game oldGame = oldCategory.getGame();
        final String username = game.getSubmission().getUser().getUsername();

        final WebhookEmbedBuilder builder = new WebhookEmbedBuilder()
            .setTitle(new WebhookEmbed.EmbedTitle(
                username + " updated a run in " + marathonName,
                this.baseUrl + "/marathon/" + marathon + "/submissions"
            ));
        final StringBuilder sb = new StringBuilder();

        sb.append(String.format(
            "**Game:** %s\n**Category:** %s\n**Platform:** %s\n**Estimate:** %s",
            parseUpdatedString(game.getName(), oldGame.getName()),
            parseUpdatedString(category.getName(), oldCategory.getName()),
            parseUpdatedString(game.getConsole(), oldGame.getConsole()),
            parseUpdatedString(TimeHelpers.formatDuration(category.getEstimate()), TimeHelpers.formatDuration(oldCategory.getEstimate()))
        ));

        if (!category.getVideo().equals(oldCategory.getVideo())) {
            sb.append("\n**Video:** ").append(parseUpdatedString(category.getVideo(), oldCategory.getVideo()));
        }

        if (category.getType() != oldCategory.getType()) {
            sb.append("\n**Type:** ")
                .append(parseUpdatedString(category.getType().name(), oldCategory.getType().name()));
        }

        builder.setDescription(sb.toString());

        this.jda.sendMessage(channel, builder.build());
    }

    private String parseUpdatedString(String current, String old) {
        if (current.equals(old)) {
            return current;
        }

        return current + " (was " + old + ')';
    }

    private void sendDonationEvent(final String marathon, final String channel, final Donation donation) {
        final DecimalFormat df = new DecimalFormat("#.##");
        String formattedAmount = df.format(donation.getAmount().doubleValue());

        final WebhookEmbedBuilder builder = new WebhookEmbedBuilder()
            .setTitle(new WebhookEmbed.EmbedTitle(
                donation.getNickname() + " donated to " + this.marathonService.getNameForCode(marathon),
                this.baseUrl + "/marathon/" + marathon + "/submissions"
            ))
            .setDescription(String.format(
                "**Amount:** %s\n**Comment:** %s",
                formattedAmount,
                donation.getComment() == null ? "None" : donation.getComment()
            ));

        this.jda.sendMessage(channel, builder.build());
    }

    private void sendSubmissionDelete(final String marathon, final String channel, final Submission submission, final User user) {
        final List<WebhookEmbed> messages = new ArrayList<>();
        final String marathonName = this.marathonService.getNameForCode(marathon);

        for (final Game game : submission.getGames()) {
            for (final Category category : game.getCategories()) {
                final String username = submission.getUser().getUsername();

                final WebhookEmbedBuilder builder = new WebhookEmbedBuilder()
                    .setTitle(new WebhookEmbed.EmbedTitle(
                        user.getUsername() + " deleted a run by " + username + " in " + marathonName,
                        this.baseUrl + "/marathon/" + marathon + "/submissions"
                    ))
                    .setDescription(String.format(
                        "**Game:** %s\n**Category:** %s\n**Platform:** %s\n**Estimate:** %s",
                        game.getName(),
                        category.getName(),
                        game.getConsole(),
                        TimeHelpers.formatDuration(category.getEstimate())
                    ));

                messages.add(builder.build());
            }
        }

        try (WebhookClient client = this.jda.forChannel(channel)) {
            for (WebhookEmbed embed : messages) {
                client.send(embed);
            }
        }
    }

    private static class OengusBotUrl {
        private final String donation;
        private final String newSubmission;
        private final String editSubmission;
        private final String marathonId;

        OengusBotUrl(String url) {
            final MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUriString(url)
                .build().getQueryParams();

            this.donation = queryParams.getFirst("donation");
            this.newSubmission = queryParams.getFirst("newsub");
            this.editSubmission = queryParams.getFirst("editsub");
            this.marathonId = queryParams.getFirst("marathon");
        }

        boolean isEmpty() {
            // marathon is required
            if (this.marathonId == null) {
                return true;
            }

            return this.donation == null && this.newSubmission == null && this.editSubmission == null;
        }

        boolean has(String type) {
            switch (type) {
                case "donation":
                    return this.donation != null;
                case "newsub":
                    return this.newSubmission != null;
                case "editsub":
                    return this.editSubmission != null;
                default:
                    return false;
            }
        }

        String get(String type) {
            switch (type) {
                case "donation":
                    return this.donation;
                case "newsub":
                    return this.newSubmission;
                case "editsub":
                    return this.editSubmission;
                case "marathon":
                    return this.marathonId;
                default:
                    return null;
            }
        }
    }
}
