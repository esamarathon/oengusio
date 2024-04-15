package app.oengus.service.twitter;

import app.oengus.adapter.jpa.entity.MarathonEntity;
import app.oengus.domain.marathon.Marathon;
import app.oengus.service.repository.TwitterAuditRepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Locale;

public abstract class AbstractTwitterService {

	private final static String SUBMISSIONS_OPEN = "SUBMISSIONS_OPEN";
	private final static String SELECTION_DONE = "SELECTION_DONE";
	private final static String SCHEDULE_DONE = "SCHEDULE_DONE";
	private final static String MARATHON_LIVE = "MARATHON_LIVE";

	@Autowired
	private TwitterAuditRepositoryService twitterAuditRepositoryService;

	@Value("${oengus.shortUrl}")
	private String shortUrl;

	abstract void send(String message);

	public void sendMarathonLiveTweet(final Marathon marathon) {
		if (!marathon.isPrivate() && !this.twitterAuditRepositoryService.exists(marathon.getId(), MARATHON_LIVE)) {
			this.send(this.buildMarathonLiveTweet(marathon));
			this.twitterAuditRepositoryService.save(marathon.getId(), MARATHON_LIVE);
		}
	}

	public void deleteAudit(final String marathonId) {
		this.twitterAuditRepositoryService.deleteByMarathon(MarathonEntity.ofId(marathonId));
	}

	protected String buildSubmissionsOpenTweet(final Marathon marathon) {
		final StringBuilder sb = new StringBuilder("Submissions open for marathon ");
		this.appendMarathonName(sb, marathon);
		sb.append(": ");
		this.appendMarathonLink(sb, marathon, null);
		sb.append("\n");
		this.appendLocation(sb, marathon);
		sb.append("\n");
		this.appendLanguage(sb, marathon);
		return sb.toString();
	}

	protected String buildSelectionDoneTweet(final Marathon marathon) {
		final StringBuilder sb = new StringBuilder("Runs selection is available for marathon ");
		this.appendMarathonName(sb, marathon);
		sb.append(": ");
		this.appendMarathonLink(sb, marathon, "submissions");
		return sb.toString();
	}

	protected String buildScheduleDoneTweet(final Marathon marathon) {
		final StringBuilder sb = new StringBuilder("Schedule is available for marathon ");
		this.appendMarathonName(sb, marathon);
		sb.append(": ");
		this.appendMarathonLink(sb, marathon, "schedule");
		return sb.toString();
	}

	protected String buildMarathonLiveTweet(final Marathon marathon) {
		final StringBuilder sb = new StringBuilder("\uD83D\uDD34 LIVE: ");
		this.appendMarathonName(sb, marathon);
		sb.append(": ");
		this.appendMarathonLink(sb, marathon, "schedule");
		sb.append("\n");
		this.appendTwitch(sb, marathon);
		return sb.toString();
	}

	private void appendMarathonName(final StringBuilder sb, final Marathon marathon) {
		sb.append(marathon.getName());
		if (marathon.getTwitter() != null) {
			sb.append(" @").append(marathon.getTwitter());
		}
	}

	private void appendMarathonLink(final StringBuilder sb, final Marathon marathon, final String subPage) {
		sb.append(this.shortUrl)
		  .append('/')
		  .append(marathon.getId());
		if (subPage != null) {
			sb.append('/').append(subPage);
		}
	}

	private void appendLocation(final StringBuilder sb, final Marathon marathon) {
		sb.append("\uD83C\uDF10 Location: ");
		if (marathon.getCountry() == null) {
			sb.append("Online");
		} else {
			final Locale l = new Locale("en", marathon.getCountry());
			sb.append(l.getDisplayCountry());
		}
	}

	private void appendLanguage(final StringBuilder sb, final Marathon marathon) {
		sb.append("\uD83D\uDDE3 Language: ");
		final Locale l = new Locale(marathon.getLanguage());
		sb.append(l.getDisplayLanguage(Locale.ENGLISH));
	}

	private void appendTwitch(final StringBuilder sb, final Marathon marathon) {
		if (marathon.getTwitch() != null) {
			sb.append("\uD83D\uDCFA Twitch: https://twitch.tv/").append(marathon.getTwitch());
		}
	}

}
