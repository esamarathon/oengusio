package app.oengus.service;

import app.oengus.adapter.jpa.entity.MarathonEntity;
import app.oengus.service.repository.MarathonRepositoryService;
import app.oengus.service.twitter.AbstractTwitterService;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class EventSchedulerService {

	private final TaskScheduler taskScheduler;
	private final MarathonRepositoryService marathonRepositoryService;
	private final AbstractTwitterService twitterService;
	private final Map<String, ScheduledFuture<?>> scheduledEvents = new ConcurrentHashMap<>();

    public EventSchedulerService(TaskScheduler taskScheduler, MarathonRepositoryService marathonRepositoryService, AbstractTwitterService twitterService) {
        this.taskScheduler = taskScheduler;
        this.marathonRepositoryService = marathonRepositoryService;
        this.twitterService = twitterService;
    }

    public void scheduleSubmissions(final MarathonEntity marathon) {
		this.unscheduleSubmissions(marathon);
		if (marathon.getSubmissionsStartDate().isAfter(ZonedDateTime.now())) {
			this.scheduledEvents.put(marathon.getId() + "-start",
					this.taskScheduler.schedule(() -> {
						this.marathonRepositoryService.openSubmissions(marathon);
						// this.twitterService.sendSubmissionsOpenTweet(marathon);
					}, Instant.from(marathon.getSubmissionsStartDate())));
		}
		if (marathon.getSubmissionsEndDate().isAfter(ZonedDateTime.now())) {
			this.scheduledEvents.put(marathon.getId() + "-end",
					this.taskScheduler.schedule(() -> this.marathonRepositoryService.closeSubmissions(marathon),
							Instant.from(marathon.getSubmissionsEndDate())));
		}
	}

	public void unscheduleSubmissions(final MarathonEntity marathon) {
		final ScheduledFuture<?> start = this.scheduledEvents.get(marathon.getId() + "-start");
		final ScheduledFuture<?> end = this.scheduledEvents.get(marathon.getId() + "-end");
		if (start != null) {
			start.cancel(true);
			this.scheduledEvents.remove(marathon.getId() + "-start");
		}
		if (end != null) {
			end.cancel(true);
			this.scheduledEvents.remove(marathon.getId() + "-end");
		}
	}

	public void scheduleMarathonStartAlert(final MarathonEntity marathon) {
		final ScheduledFuture<?> start = this.scheduledEvents.get(marathon.getId());

		if (start != null) {
			start.cancel(true);
		}

		this.scheduledEvents.put(
            marathon.getId(),
            this.taskScheduler.schedule(
                () -> this.twitterService.sendMarathonLiveTweet(marathon),
                Instant.from(marathon.getStartDate())
            )
        );
	}

	@Scheduled(cron = "0 0 0 * * *")
	public void cleanup() {
		this.scheduledEvents.entrySet().removeIf(entry -> entry.getValue().getDelay(TimeUnit.SECONDS) < 0);
	}

}
