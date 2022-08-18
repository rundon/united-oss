package com.onefly.united.view.filter;


import com.onefly.united.view.config.KkViewProperties;

import javax.servlet.*;
import java.io.IOException;

/**
 * @author chenjh
 * @since 2020/5/13 18:34
 */
public class WatermarkConfigFilter implements Filter {

    private KkViewProperties kkViewProperties;

    public WatermarkConfigFilter(KkViewProperties kkViewProperties) {
        this.kkViewProperties = kkViewProperties;
    }

    @Override
    public void init(FilterConfig filterConfig) {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        String watermarkTxt = request.getParameter("watermarkTxt");
        request.setAttribute("disableSwitch", kkViewProperties.isDisableSwitch());
        request.setAttribute("watermarkTxt", watermarkTxt != null ? watermarkTxt : kkViewProperties.getWatermark().getTxt());
        request.setAttribute("watermarkXSpace", kkViewProperties.getWatermark().getXSpace());
        request.setAttribute("watermarkYSpace", kkViewProperties.getWatermark().getYSpace());
        request.setAttribute("watermarkFont", kkViewProperties.getWatermark().getFont());
        request.setAttribute("watermarkFontsize", kkViewProperties.getWatermark().getFontSize());
        request.setAttribute("watermarkColor", kkViewProperties.getWatermark().getColor());
        request.setAttribute("watermarkAlpha", kkViewProperties.getWatermark().getAlpha());
        request.setAttribute("watermarkWidth", kkViewProperties.getWatermark().getWidth());
        request.setAttribute("watermarkHeight", kkViewProperties.getWatermark().getHeight());
        request.setAttribute("watermarkAngle", kkViewProperties.getWatermark().getAngle());
        filterChain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }
}
