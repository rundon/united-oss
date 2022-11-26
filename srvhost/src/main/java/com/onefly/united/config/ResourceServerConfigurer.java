package com.onefly.united.config;

import com.alibaba.fastjson.JSON;
import com.onefly.united.common.utils.Result;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author LANXE
 */
@Configuration
@EnableResourceServer
@Import(FeignClientsConfiguration.class)
class ResourceServerConfigurer extends ResourceServerConfigurerAdapter {

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .exceptionHandling()
                .accessDeniedHandler(accessDeniedHandler())
                .authenticationEntryPoint(authenticationEntryPoint())
                .and()
                .headers().frameOptions().sameOrigin()
                .and()
                .antMatcher("/**")
                .authorizeRequests()
                .antMatchers("/actuator/**",
                        "/api/public/**",
                        "/bootstrap/**",
                        "/bootstrap-table/**",
                        "/css/**",
                        "/gitalk/**",
                        "/fonts/**",
                        "/images/**",
                        "/js/**",
                        "/pdfjs/**",
                        "/libs/**",
                        "/plugins/**",
                        "/plyr/**",
                        "/file/**").permitAll()
                .anyRequest().authenticated()
                .and()
                .httpBasic();
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, exception) -> {
            sendError(response, HttpServletResponse.SC_OK, HttpServletResponse.SC_FORBIDDEN, exception.getMessage());
        };
    }

    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, exception) -> {
            sendError(response, HttpServletResponse.SC_OK, HttpServletResponse.SC_UNAUTHORIZED, exception.getMessage());
        };
    }

    private void sendError(HttpServletResponse response, int status, int code, String exception) throws IOException {
        response.setHeader("Content-type", "text/html;charset=UTF-8");  //这句话的意思，是告诉servlet用UTF-8转码，而不是用默认的ISO8859
        response.setCharacterEncoding("UTF-8");
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(JSON.toJSONString(new Result().error(code, exception)));
    }


}