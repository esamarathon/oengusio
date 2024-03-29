package app.oengus.web.v2;

import app.oengus.entity.dto.DataListDto;
import app.oengus.entity.dto.v2.schedule.ScheduleDto;
import app.oengus.entity.dto.v2.schedule.ScheduleInfoDto;
import app.oengus.spring.model.Views;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(maxAge = 3600)
@Tag(name = "schedules-v2")
@RequestMapping("/v2/marathons/{marathonId}/schedules")
public interface ScheduleApi {

    @GetMapping
    @JsonView(Views.Public.class)
    @PreAuthorize("canUpdateMarathon(#marathonId) || isScheduleDone(#marathonId)")
    @Operation(
        summary = "Get all schedules for a marathon, has a 5 minute cache",
        responses = {
            @ApiResponse(
                description = "List of schedules for this marathon",
                responseCode = "200",
                content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = ScheduleDto.class))
                )
            ),
            @ApiResponse(
                description = "Marathon not found",
                responseCode = "404"
            )
        }
    )
    ResponseEntity<DataListDto<ScheduleInfoDto>> findAllForMarathon(@PathVariable("marathonId") final String marathonId);

    @GetMapping("/{scheduleId}")
    @JsonView(Views.Public.class)
    @PreAuthorize("canUpdateMarathon(#marathonId) || isScheduleDone(#marathonId)")
    @Operation(
        summary = "Get a schedule for a marathon by its id, has a 5 minute cache",
        responses = {
            @ApiResponse(
                description = "The requested schedule",
                responseCode = "200",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ScheduleDto.class)
                )
            ),
            @ApiResponse(
                description = "Marathon not found",
                responseCode = "404"
            )
        }
    )
    ResponseEntity<ScheduleDto> findScheduleById(
        @PathVariable("marathonId") final String marathonId,
        @PathVariable("scheduleId") final int scheduleId,
        @RequestParam(defaultValue = "false", required = false) boolean withCustomData
    );
}
