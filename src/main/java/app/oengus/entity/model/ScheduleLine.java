package app.oengus.entity.model;

import app.oengus.spring.model.Views;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import org.hibernate.validator.constraints.time.DurationMin;

import javax.annotation.Nullable;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "schedule_line")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScheduleLine {
    // TODO: unique key out of schedule id + position?
    @Id
    @JsonView(Views.Public.class)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "schedule_id")
    @JsonBackReference
    @JsonView(Views.Public.class)
    private Schedule schedule;

    @Column(name = "game_name")
    @JsonView(Views.Public.class)
    @Size(max = 100)
    private String gameName;

    @Column(name = "console")
    @JsonView(Views.Public.class)
    @Size(max = 20) // can grow upto 100 due to db type being varchar(100)
    private String console;

    @Column(name = "emulated")
    @JsonView(Views.Public.class)
    private boolean emulated = false;

    @Column(name = "ratio")
    @JsonView(Views.Public.class)
    @Size(max = 10)
    private String ratio;

    @Column(name = "category_name")
    @JsonView(Views.Public.class)
    @Size(max = 100)
    private String categoryName;

    @Column(name = "estimate")
    @JsonView(Views.Public.class)
    @NotNull
    @DurationMin(seconds = 0)
    private Duration estimate;

    @Column(name = "setup_time")
    @JsonView(Views.Public.class)
    @DurationMin(seconds = 0)
    private Duration setupTime;

    @Column(name = "setup_block")
    @JsonView(Views.Public.class)
    private boolean setupBlock;

    @Column(name = "custom_run")
    @JsonView(Views.Public.class)
    private boolean customRun;

    @Column(name = "position")
    @JsonView(Views.Public.class)
    private int position;

    @Column(name = "category_id")
    @JsonView(Views.Public.class)
    @Nullable
    private Integer categoryId;

    @Column(name = "run_type")
    @JsonView(Views.Public.class)
    private RunType type;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "schedule_line_runner",
        joinColumns = {@JoinColumn(name = "schedule_line_id")}
    )
    @AttributeOverrides({
        @AttributeOverride(name = "user", column = @Column(name = "user_id")),
        @AttributeOverride(name = "runnerName", column = @Column(name = "runner_name"))
    })
    @JsonView(Views.Public.class)
    private List<ScheduleLineRunner> runners;

    @Column(name = "setup_block_text")
    @JsonView(Views.Public.class)
    @Size(max = 50)
    private String setupBlockText;

    @Transient
    private ZonedDateTime date;

    @Column(name = "custom_data")
    @JsonView(Views.Internal.class)
    private String customData;

    @Transient
    @JsonView(Views.Public.class)
    private String customDataDTO;

    public int getId() {
        return this.id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public Schedule getSchedule() {
        return this.schedule;
    }

    public void setSchedule(final Schedule schedule) {
        this.schedule = schedule;
    }

    public String getGameName() {
        return this.gameName;
    }

    public void setGameName(final String gameName) {
        this.gameName = gameName;
    }

    public String getConsole() {
        return this.console;
    }

    public void setConsole(final String console) {
        this.console = console;
    }

    public String getRatio() {
        return this.ratio;
    }

    public void setRatio(final String ratio) {
        this.ratio = ratio;
    }

    public String getCategoryName() {
        return this.categoryName;
    }

    public void setCategoryName(final String categoryName) {
        this.categoryName = categoryName;
    }

    public Duration getEstimate() {
        return this.estimate;
    }

    public void setEstimate(final Duration estimate) {
        this.estimate = estimate;
    }

    public Duration getSetupTime() {
        return this.setupTime;
    }

    public void setSetupTime(final Duration setupTime) {
        this.setupTime = setupTime;
    }

    public boolean isSetupBlock() {
        return this.setupBlock;
    }

    public void setSetupBlock(final boolean setupBlock) {
        this.setupBlock = setupBlock;
    }

    public int getPosition() {
        return this.position;
    }

    public void setPosition(final int position) {
        this.position = position;
    }

    public List<ScheduleLineRunner> getRunners() {
        return this.runners;
    }

    public void setRunners(final List<ScheduleLineRunner> runners) {
        this.runners = runners;
    }

    @Nullable
    public Integer getCategoryId() {
        return this.categoryId;
    }

    public void setCategoryId(@Nullable final Integer categoryId) {
        this.categoryId = categoryId;
    }

    public boolean isEmulated() {
        return this.emulated;
    }

    public void setEmulated(final boolean emulated) {
        this.emulated = emulated;
    }

    public RunType getType() {
        return this.type;
    }

    public void setType(final RunType type) {
        this.type = type;
    }

    public boolean isCustomRun() {
        return this.customRun;
    }

    public void setCustomRun(final boolean customRun) {
        this.customRun = customRun;
    }

    public ZonedDateTime getDate() {
        return this.date;
    }

    public void setDate(final ZonedDateTime date) {
        this.date = date;
    }

    public String getSetupBlockText() {
        return this.setupBlockText;
    }

    public void setSetupBlockText(final String setupBlockText) {
        this.setupBlockText = setupBlockText;
    }

    public String getCustomData() {
        return customData;
    }

    public void setCustomData(String customData) {
        this.customData = customData;
    }

    public String getCustomDataDTO() {
        return customDataDTO;
    }

    public void setCustomDataDTO(String customDataDTO) {
        this.customDataDTO = customDataDTO;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScheduleLine that = (ScheduleLine) o;
        return id == that.id && emulated == that.emulated && setupBlock == that.setupBlock && customRun == that.customRun && Objects.equals(gameName, that.gameName) && Objects.equals(console, that.console) && Objects.equals(ratio, that.ratio) && Objects.equals(categoryName, that.categoryName) && Objects.equals(estimate, that.estimate) && Objects.equals(setupTime, that.setupTime) && Objects.equals(categoryId, that.categoryId) && type == that.type && Objects.equals(setupBlockText, that.setupBlockText) && Objects.equals(customData, that.customData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, gameName, console, emulated, ratio, categoryName, estimate, setupTime, setupBlock, customRun, categoryId, type, setupBlockText, customData);
    }
}
