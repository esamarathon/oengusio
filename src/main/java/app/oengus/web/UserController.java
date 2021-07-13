package app.oengus.web;

import app.oengus.entity.dto.UserProfileDto;
import app.oengus.entity.model.Error;
import app.oengus.entity.model.User;
import app.oengus.exception.OengusBusinessException;
import app.oengus.requests.user.UserUpdateRequest;
import app.oengus.service.UserService;
import app.oengus.spring.model.LoginRequest;
import app.oengus.spring.model.Role;
import app.oengus.spring.model.Views;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.security.auth.login.LoginException;
import javax.validation.Valid;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(maxAge = 3600)
@RestController
@RequestMapping("/users")
@Api
public class UserController {
    private final UserService userService;
    private final List<String> oauthOrigins;

    @Autowired
    public UserController(final UserService userService, @Value("${oengus.oauthOrigins}") final List<String> oauthOrigins) {
        this.userService = userService;
        this.oauthOrigins = oauthOrigins;
    }

    @PostMapping("/login")
    @PermitAll
    @ApiIgnore
    public ResponseEntity<?> login(@RequestBody final LoginRequest request, @RequestHeader("Origin") final String host) throws LoginException {
        if (!this.oauthOrigins.contains(host)) {
            throw new OengusBusinessException("ORIGIN_DISALLOWED");
        }

        return ResponseEntity.ok(this.userService.login(host, request));
    }

    @PostMapping("/sync")
    @RolesAllowed({"ROLE_USER"})
    @PreAuthorize("!isBanned()")
    @ApiIgnore
    public ResponseEntity<?> sync(@RequestBody final LoginRequest request, @RequestHeader("Origin") final String host) throws LoginException {
        if (!this.oauthOrigins.contains(host)) {
            throw new OengusBusinessException("ORIGIN_DISALLOWED");
        }

        return ResponseEntity.ok(this.userService.sync(host, request));
    }

    @GetMapping("/{name}/exists")
    @PermitAll
    @ApiOperation(value = "Check if username exists")
    public ResponseEntity<Map<String, Boolean>> exists(@PathVariable("name") final String name) {
        final Map<String, Boolean> validationErrors = new HashMap<>();
        if (this.userService.exists(name)) {
            validationErrors.put("exists", true);
        }
        return ResponseEntity.ok(validationErrors);
    }

    @GetMapping("/{name}/search")
    @JsonView(Views.Public.class)
    @PermitAll
    @ApiOperation(value = "Get a list of users that include searched string in their username",
        response = User.class,
        responseContainer = "List")
    public ResponseEntity<List<User>> search(@PathVariable("name") final String name) {
        return ResponseEntity.ok(this.userService.findUsersWithUsername(name));
    }

    @GetMapping("/{name}")
    @JsonView(Views.Public.class)
    @PermitAll
    @ApiOperation(value = "Get a user profile",
        response = UserProfileDto.class)
    public ResponseEntity<UserProfileDto> getUserProfile(@PathVariable("name") final String name) throws NotFoundException {
        final UserProfileDto userProfile = this.userService.getUserProfile(name);

        return ResponseEntity.ok(userProfile);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("isSelf(#id) && !isBanned()")
    @ApiIgnore
    public ResponseEntity<?> updateUser(@PathVariable("id") final int id,
                                        @RequestBody @Valid final UserUpdateRequest userPatch,
                                        final BindingResult bindingResult) throws NotFoundException {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(bindingResult.getAllErrors());
        }

        this.userService.updateRequest(id, userPatch);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("(isSelf(#id) && !isBanned()) || isAdmin()")
    @ApiIgnore
    public ResponseEntity<?> deleteUser(@PathVariable("id") final int id) throws NotFoundException {
        this.userService.markDeleted(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @RolesAllowed({"ROLE_USER"})
    @JsonView(Views.Internal.class)
    @ApiIgnore
    public ResponseEntity<User> me(final Principal principal) throws NotFoundException {
        final int id = ((User) ((UsernamePasswordAuthenticationToken) principal).getPrincipal()).getId();

        return ResponseEntity.ok(this.userService.getUser(id));
    }

    @PostMapping("/{id}/ban")
    @PreAuthorize("isAdmin()")
    @ApiIgnore
    public ResponseEntity<?> ban(@PathVariable int id) throws NotFoundException {
        this.userService.addRole(id, Role.ROLE_BANNED);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/ban")
    @PreAuthorize("isAdmin()")
    @ApiIgnore
    public ResponseEntity<?> unban(@PathVariable int id) throws NotFoundException {
        this.userService.removeRole(id, Role.ROLE_BANNED);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/enabled")
    @PreAuthorize("isAdmin()")
    @ApiIgnore
    public ResponseEntity<?> setEnabled(@PathVariable int id, @RequestParam("status") final boolean status) throws NotFoundException {
        final User patch = this.userService.getUser(id);

        patch.setEnabled(status);

        this.userService.update(id, patch);

        return ResponseEntity.noContent().build();
    }
}
