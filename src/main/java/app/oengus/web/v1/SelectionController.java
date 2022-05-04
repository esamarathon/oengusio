package app.oengus.web.v1;

import app.oengus.entity.dto.SelectionDto;
import app.oengus.entity.model.Status;
import app.oengus.service.SelectionService;
import app.oengus.spring.model.Views;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;

@CrossOrigin(maxAge = 3600)
@RestController
@RequestMapping({"/v1/marathons/{marathonId}/selections", "/marathons/{marathonId}/selections"})
@Api
public class SelectionController {

    @Autowired
    private SelectionService selectionService;

    @GetMapping
    @PreAuthorize("(!isBanned() && canUpdateMarathon(#marathonId) || isSelectionDone(#marathonId))")
    @JsonView(Views.Public.class)
    @ApiOperation(value = "Get all selection statuses a marathon",
        response = SelectionDto.class)
    public ResponseEntity<?> findAllForMarathon(@PathVariable("marathonId") final String marathonId,
                                                @RequestParam(name = "status", required = false) final List<Status> statuses) {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noCache())
            .body(this.selectionService.findByMarathon(marathonId, statuses));
    }

    @PutMapping
    @PreAuthorize("!isBanned() && canUpdateMarathon(#marathonId)")
    @ApiIgnore
    public ResponseEntity<?> saveOrUpdate(@PathVariable("marathonId") final String marathonId,
                                          @RequestBody final List<SelectionDto> selections) throws NotFoundException {
        this.selectionService.saveOrUpdate(marathonId, selections);

        return ResponseEntity.noContent().build();
    }

}
