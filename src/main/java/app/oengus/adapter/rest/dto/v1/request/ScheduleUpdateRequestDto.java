package app.oengus.adapter.rest.dto.v1.request;

import app.oengus.domain.submission.RunType;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.AssertTrue;
import java.time.Duration;
import java.util.List;

@Getter
@Setter
public class ScheduleUpdateRequestDto {
    // TODO: id and marathon id should not be able to be set by the end user

    private List<Line> lines;

    @Getter
    @Setter
    public static class Line {
        private String gameName;
        private String console;
        private boolean emulated;
        private String ratio;
        private String categoryName;
        private Duration estimate;
        private Duration setupTime;
        private boolean setupBlock;
        private boolean customRun;
        private RunType runType;
        private String setupBlockText;
        private String customData;
    }

    @Getter
    @Setter
    public static class LineRunner {
        private String runnerName;
        private SimpleUser user;

        @AssertTrue
        public boolean runnerOrNameIsSet() {
            return this.user != null || this.runnerName != null;
        }
    }

    @Getter
    @Setter
    public static class SimpleUser {
        private int id;
    }
}
