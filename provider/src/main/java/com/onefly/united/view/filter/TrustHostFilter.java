package com.onefly.united.view.filter;

import com.onefly.united.common.utils.IpUtils;
import com.onefly.united.view.config.KkViewProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * @author chenjh
 * @since 2020/2/18 19:13
 */
public class TrustHostFilter implements Filter {

    public TrustHostFilter(KkViewProperties kkViewProperties) {
        this.kkViewProperties = kkViewProperties;
    }

    private String notTrustHost;

    private KkViewProperties kkViewProperties;

    @Override
    public void init(FilterConfig filterConfig) {
        ClassPathResource classPathResource = new ClassPathResource("web/notTrustHost.html");
        try {
            classPathResource.getInputStream();
            byte[] bytes = FileCopyUtils.copyToByteArray(classPathResource.getInputStream());
            this.notTrustHost = new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String ip = IpUtils.getIpAddr((HttpServletRequest) request);
        if (kkViewProperties.getCertification().isBindIp()) {
            String host = getHost(kkViewProperties.getCertification().getResourceUri());
            if (!ip.equals(host)) {
                String html = this.notTrustHost.replace("${current_host}", ip);
                response.getWriter().write(html);
                response.getWriter().close();
            }

        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }

    private String getHost(String urlStr) {
        try {
            URL url = new URL(urlStr);
            return url.getHost().toLowerCase();
        } catch (MalformedURLException ignored) {
        }
        return null;
    }
}
