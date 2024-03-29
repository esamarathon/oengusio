package app.oengus.spring;

import app.oengus.entity.model.User;
import app.oengus.spring.model.Role;
import io.jsonwebtoken.Claims;
import io.sentry.Sentry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AuthenticationFilter extends OncePerRequestFilter {

	@Autowired
	private JWTUtil jwtUtil;

	private static final String AUTH_HEADER = "Authorization";

	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
	                                final FilterChain chain)
			throws ServletException, IOException {

        // TODO: why the fuck is this required, should already have been configured by spring
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Expose-Headers", "Location");
        response.addHeader("Access-Control-Allow-Headers", "*");
        if (request.getHeader("Access-Control-Request-Method") != null &&
            "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.addHeader("Access-Control-Allow-Headers", "Authorization");
            response.addHeader("Access-Control-Allow-Headers", "Content-Type");
            response.addHeader("Access-Control-Max-Age", "1");
            response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH");
        }


		final String authHeader = request.getHeader(AUTH_HEADER);

        // TODO: there is probably a better way of going about this
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			final String token = authHeader.substring(7);
			if (this.jwtUtil.isTokenValid(token)) {
				try {
					final Claims claims = this.jwtUtil.getAllClaimsFromToken(token);
					final List<String> rolesString = claims.get("role", List.class);
					final boolean enabled = claims.get("enabled", Boolean.class);

					final List<Role> roles = new ArrayList<>();
					for (final String r : rolesString) {
						roles.add(Role.valueOf(r));
					}

					final User u = new User();
					u.setUsername(claims.getSubject());
					u.setEnabled(enabled);
					u.setRoles(roles);
					u.setId(Integer.parseInt(claims.getId()));

					final UsernamePasswordAuthenticationToken authentication =
							new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities());
					authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authentication);
				} catch (final Exception e) {
                    Sentry.captureException(e);
					//log.error("ERROR ", e);
				}
			}/* else {
                // cancel request, token is not valid
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }*/
		}
		if (!request.getMethod().equalsIgnoreCase("OPTIONS")) {
			chain.doFilter(request, response);
		}
	}
}
