package app.oengus.adapter.rest.controller.v2;

import app.oengus.adapter.rest.dto.BooleanStatusDto;
import app.oengus.adapter.rest.dto.v1.MarathonBasicInfoDto;
import app.oengus.adapter.rest.dto.v2.MarathonHomeDto;
import app.oengus.adapter.rest.dto.v2.marathon.MarathonSettingsDto;
import app.oengus.adapter.rest.dto.v2.marathon.QuestionDto;
import app.oengus.adapter.rest.dto.v2.marathon.request.ModeratorsUpdateRequest;
import app.oengus.adapter.rest.dto.v2.marathon.request.QuestionsUpdateRequest;
import app.oengus.adapter.rest.dto.v2.users.ProfileDto;
import app.oengus.adapter.rest.mapper.MarathonDtoMapper;
import app.oengus.application.MarathonService;
import app.oengus.domain.exception.MarathonNotFoundException;
import app.oengus.domain.marathon.Marathon;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.function.Function;

import static app.oengus.adapter.rest.helper.HeaderHelpers.cachingHeaders;

@RestController("v2MarathonController")
@RequiredArgsConstructor
public class MarathonApiController implements MarathonApi {
    private final MarathonDtoMapper mapper;
    private final MarathonService marathonService;

    @Override
    public ResponseEntity<MarathonHomeDto> getMarathonsForHome() {
        final var next = this.marathonService.findNext();
        final var open = this.marathonService.findSubmitsOpen();
        final var live = this.marathonService.findLive();

        final Function<List<Marathon>, List<MarathonBasicInfoDto>> transform =
            (items) -> items.stream().map(this.mapper::toBasicInfo).toList();

        return ResponseEntity.ok()
            .headers(cachingHeaders(10, false))
            .body(
                new MarathonHomeDto(
                    transform.apply(live),
                    transform.apply(next),
                    transform.apply(open)
                )
            );
    }

    @Override
    public ResponseEntity<MarathonSettingsDto> getSettings(String marathonId) {
        final var marathon = this.marathonService.findById(marathonId)
            .orElseThrow(MarathonNotFoundException::new);

        return ResponseEntity.ok()
            .cacheControl(CacheControl.noCache())
            .body(
                this.mapper.toSettingsDto(marathon)
            );
    }

    @Override
    public ResponseEntity<MarathonSettingsDto> saveSettings(String marathonId, MarathonSettingsDto patch) {
        final String newMstdn = patch.getMastodon();

        if (newMstdn != null && newMstdn.isBlank()) {
            patch.setMastodon(null);
        }

        final var marathon = this.marathonService.findById(marathonId)
            .orElseThrow(MarathonNotFoundException::new);

        this.mapper.applyUpdateRequest(marathon, patch);

        final var savedMarathon = this.marathonService.update(marathonId, marathon);

        return ResponseEntity.ok()
            .cacheControl(CacheControl.noCache())
            .body(
                this.mapper.toSettingsDto(savedMarathon)
            );
    }

    @Override
    public ResponseEntity<List<ProfileDto>> getModerators(String marathonId) {
        return null;
    }

    @Override
    public ResponseEntity<BooleanStatusDto> updateModerators(String marathonId, ModeratorsUpdateRequest body) {
        return null;
    }

    @Override
    public ResponseEntity<BooleanStatusDto> removeModerator(String marathonId, int userId) {
        return null;
    }

    @Override
    public ResponseEntity<List<QuestionDto>> getQuestions(String marathonId) {
        return null;
    }

    @Override
    public ResponseEntity<BooleanStatusDto> updateQuestions(String marathonId, QuestionsUpdateRequest body) {
        return null;
    }

    @Override
    public ResponseEntity<BooleanStatusDto> removeQuestion(String marathonId, int questionId) {
        return null;
    }
}
