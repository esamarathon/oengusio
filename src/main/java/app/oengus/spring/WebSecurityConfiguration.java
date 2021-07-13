package app.oengus.spring;

import app.oengus.spring.handler.ForbiddenHandler;
import app.oengus.spring.handler.UnauthorizedHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

	@Bean
	public UnauthorizedHandler unauthorizedHandler() throws Exception {
		return new UnauthorizedHandler();
	}

	@Bean
	public ForbiddenHandler forbiddenHandler() throws Exception {
		return new ForbiddenHandler();
	}

	@Bean
	public AuthenticationFilter authenticationFilterBean() throws Exception {
		return new AuthenticationFilter();
	}

	@Override
	protected void configure(final HttpSecurity httpSecurity) throws Exception {
		httpSecurity
				// we don't need CSRF because our token is invulnerable
				.csrf().disable()

				.exceptionHandling().authenticationEntryPoint(this.unauthorizedHandler()).and()
				.exceptionHandling().accessDeniedHandler(this.forbiddenHandler()).and()

				// don't create session
				.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

		// custom JWT based security filter
		httpSecurity.addFilterBefore(this.authenticationFilterBean(), UsernamePasswordAuthenticationFilter.class);

		// disable page caching
		httpSecurity.headers().cacheControl();
	}
}
