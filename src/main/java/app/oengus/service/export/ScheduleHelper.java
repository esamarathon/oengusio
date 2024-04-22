package app.oengus.service.export;

import app.oengus.domain.schedule.Line;
import app.oengus.domain.schedule.Schedule;
import app.oengus.entity.dto.V1ScheduleDto;
import app.oengus.entity.dto.ScheduleLineDto;
import app.oengus.adapter.jpa.entity.ScheduleLine;
import app.oengus.helper.BeanHelper;
import app.oengus.application.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ScheduleHelper {
	private final ScheduleService scheduleService;

    @Nullable
	public V1ScheduleDto getSchedule(final String marathonId, final String zoneId) {
		final Schedule found = this.scheduleService.findByMarathon(marathonId);

        if (found == null) {
            return null;
        }

        final V1ScheduleDto schedule = V1ScheduleDto.fromSchedule(found);
		final List<ScheduleLineDto> scheduleLineDtos = new ArrayList<>();

		for (int i = 0; i < found.getLines().size(); i++) {
            final Line line = found.getLines().get(i);
            final ScheduleLineDto scheduleLineDto = ScheduleLineDto.fromLine(line);

			BeanHelper.copyProperties(line, scheduleLineDto);

            // TODO: what the fuck is the difference between date and time?
			if (i == 0) {
				scheduleLineDto.setTime(
                    line.getSchedule()
                        .getMarathon()
                        .getStartDate()
                        .withSecond(0)
                        .withZoneSameInstant(ZoneId.of(zoneId))
                );
			} else {
				scheduleLineDto.setTime(scheduleLineDtos.get(i - 1)
				                                        .getTime()
				                                        .plus(found.getLines().get(i - 1)
				                                                           .getEstimate())
				                                        .plus(found.getLines().get(i - 1)
				                                                           .getSetupTime()));
			}
			scheduleLineDtos.add(scheduleLineDto);
		}
		schedule.setLinesWithTime(scheduleLineDtos);
		return schedule;
	}
}
