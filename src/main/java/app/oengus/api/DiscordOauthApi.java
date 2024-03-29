package app.oengus.api;

import app.oengus.spring.CoreFeignConfiguration;
import app.oengus.spring.model.AccessToken;
import feign.Headers;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;

@FeignClient(value = "discord-oauth", url = "https://discord.com/api/oauth2", configuration = CoreFeignConfiguration.class)
public interface DiscordOauthApi {
    @Headers("Content-Type: application/x-www-form-urlencoded")
    @RequestMapping(method = RequestMethod.POST, value = "/token", consumes = APPLICATION_FORM_URLENCODED_VALUE)
    AccessToken getAccessToken(@RequestBody Map<String, ?> body);
}
