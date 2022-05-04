package app.oengus.web;

import app.oengus.entity.model.Incentive;
import app.oengus.service.IncentiveService;
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

import javax.annotation.security.RolesAllowed;
import java.util.List;

@CrossOrigin(maxAge = 3600)
@RestController
@RequestMapping({"/v1/marathons/{marathonId}/incentives", "/marathons/{marathonId}/incentives"})
@Api
public class IncentiveController {

    @Autowired
    private IncentiveService incentiveService;

    @GetMapping
    @JsonView(Views.Public.class)
    @ApiOperation(value = "Get all incentives for a marathon",
        response = Incentive.class,
        responseContainer = "List")
    public ResponseEntity<?> findAllForMarathon(@PathVariable("marathonId") final String marathonId,
                                                @RequestParam(required = false, defaultValue = "true") final boolean withLocked,
                                                @RequestParam(required = false, defaultValue = "false") final boolean withUnapproved) throws NotFoundException {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noCache())
            .body(this.incentiveService.findByMarathon(marathonId, withLocked, withUnapproved));
    }

    @PostMapping
    @JsonView(Views.Public.class)
    @RolesAllowed({"ROLE_USER"})
    @PreAuthorize("canUpdateMarathon(#marathonId) && !isBanned()")
    @ApiIgnore
    public ResponseEntity<?> save(@PathVariable("marathonId") final String marathonId,
                                  @RequestBody final List<Incentive> incentives) {
        return ResponseEntity.ok(this.incentiveService.saveAll(incentives, marathonId));
    }

}
