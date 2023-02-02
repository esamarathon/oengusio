package app.oengus.web.v2;

import app.oengus.entity.dto.DataListDto;
import app.oengus.entity.dto.v2.marathon.CategoryDto;
import app.oengus.entity.dto.v2.marathon.SubmissionDto;
import app.oengus.entity.dto.v2.marathon.GameDto;
import app.oengus.service.CategoryService;
import app.oengus.service.ExportService;
import app.oengus.service.GameService;
import app.oengus.service.SubmissionService;
import app.oengus.spring.model.Views;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static app.oengus.helper.HeaderHelpers.cachingHeaders;

@CrossOrigin(maxAge = 3600)
@Tag(name = "submissions-v2")
@RestController("v2SubmissionController")
@RequestMapping("/v2/marathons/{marathonId}/submissions")
public class SubmissionsController {

    private final CategoryService categoryService;
    private final ExportService exportService;
    private final GameService gameService;
    private final SubmissionService submissionService;

    public SubmissionsController(CategoryService categoryService, GameService gameService, ExportService exportService, SubmissionService submissionService) {
        this.categoryService = categoryService;
        this.gameService = gameService;
        this.exportService = exportService;
        this.submissionService = submissionService;
    }

    @GetMapping
    @JsonView(Views.Public.class)
    @Operation(
        summary = "List submissions for marathon",
        description = "List all submissions for a marathon, top-level info only, has a 30 minute cache"
    )
    public ResponseEntity<DataListDto<SubmissionDto>> getAllSubmissionsToplevel(
        @PathVariable("marathonId") final String marathonId
    ) {
        return ResponseEntity.ok()
            .headers(cachingHeaders(30, false))
            .body(new DataListDto<>(
                this.submissionService.getToplevelSubmissionsForMarathon(marathonId)
            ));
    }

    // TODO: do we really need the marathon id in the queries?
    // (technically we don't, but is it better for security?)
    @GetMapping("/{submissionId}/games")
    @JsonView(Views.Public.class)
    public ResponseEntity<DataListDto<GameDto>> getGamesForSubmission(
        @PathVariable("marathonId") final String marathonId,
        @PathVariable("submissionId") final int submissionId
    ) {
        return ResponseEntity.ok()
            .headers(cachingHeaders(30, false))
            .body(new DataListDto<>(
                this.gameService.findBySubmissionId(marathonId, submissionId)
            ));
    }

    @GetMapping("/{submissionId}/games/{gameId}/categories")
    @JsonView(Views.Public.class)
    public ResponseEntity<DataListDto<CategoryDto>> getCatgegoriesForGame(
        @PathVariable("marathonId") final String marathonId,
        @PathVariable("submissionId") final int submissionId,
        @PathVariable("gameId") final int gameId
    ) {
        return ResponseEntity.ok()
            .headers(cachingHeaders(30, false))
            .body(new DataListDto<>(
                this.categoryService.findByGameId(marathonId, submissionId, gameId)
            ));
    }
}
