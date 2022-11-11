package com.onefly.united.config;

import com.onefly.united.view.config.KkViewProperties;
import com.onefly.united.view.config.RFCConfig;
import com.onefly.united.view.config.WebConfig;
import com.onefly.united.view.filter.BaseUrlFilter;
import com.onefly.united.view.filter.ChinesePathFilter;
import com.onefly.united.view.filter.TrustHostFilter;
import com.onefly.united.view.filter.WatermarkConfigFilter;
import com.onefly.united.view.service.cache.CacheService;
import com.onefly.united.view.service.cache.impl.CacheServiceRedisImpl;
import org.icepdf.ri.util.FontPropertiesManager;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * kkview 配置
 */
@Configuration
@EnableConfigurationProperties({KkViewProperties.class, RedisProperties.class})
@Import({WebConfig.class, RFCConfig.class})
public class KkViewConfig {

    @Bean
    public CacheService cacheServiceRedisImpl(Config config) {
        CacheService cacheService = new CacheServiceRedisImpl(config);
        return cacheService;
    }


    @Bean
    public FilterRegistrationBean getChinesePathFilter() {
        ChinesePathFilter filter = new ChinesePathFilter();
        FilterRegistrationBean registrationBean = new FilterRegistrationBean();
        registrationBean.setFilter(filter);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean getBaseUrlFilter() {
        Set<String> filterUri = new HashSet<>();
        filterUri.add("/onlinePreview");
        filterUri.add("/onlinePreviewById");
        BaseUrlFilter filter = new BaseUrlFilter();
        FilterRegistrationBean registrationBean = new FilterRegistrationBean();
        registrationBean.setFilter(filter);
        registrationBean.setUrlPatterns(filterUri);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean getWatermarkConfigFilter(KkViewProperties kkViewProperties) {
        Set<String> filterUri = new HashSet<>();
        filterUri.add("/onlinePreview");
        filterUri.add("/onlinePreviewById");
        WatermarkConfigFilter filter = new WatermarkConfigFilter(kkViewProperties);
        FilterRegistrationBean registrationBean = new FilterRegistrationBean();
        registrationBean.setFilter(filter);
        registrationBean.setUrlPatterns(filterUri);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean getTrustHostFilter(KkViewProperties kkViewProperties) {
        Set<String> filterUri = new HashSet<>();
        filterUri.add("/onlinePreview");
        filterUri.add("/onlinePreviewById");
        TrustHostFilter filter = new TrustHostFilter(kkViewProperties);
        FilterRegistrationBean registrationBean = new FilterRegistrationBean();
        registrationBean.setFilter(filter);
        registrationBean.setUrlPatterns(filterUri);
        return registrationBean;
    }

    @Bean("myExecutor")
    public ExecutorService executorService() {
        // read/store the font cache.
        FontPropertiesManager.getInstance().loadOrReadSystemFonts();
        int process = 2 * Runtime.getRuntime().availableProcessors() + 1;
        return Executors.newFixedThreadPool(process);
    }

    @Bean("pptlicense")
    public com.aspose.slides.License pptlicense() throws Exception {
        String license = "<License>\n" +
                "  <Data>\n" +
                "    <Products>\n" +
                "      <Product>Aspose.Total for Java</Product>\n" +
                "      <Product>Aspose.Words for Java</Product>\n" +
                "    </Products>\n" +
                "    <EditionType>Enterprise</EditionType>\n" +
                "    <SubscriptionExpiry>20991231</SubscriptionExpiry>\n" +
                "    <LicenseExpiry>20991231</LicenseExpiry>\n" +
                "    <SerialNumber>8bfe198c-7f0c-4ef8-8ff0-acc3237bf0d7</SerialNumber>\n" +
                "  </Data>\n" +
                "  <Signature>sNLLKGMUdF0r8O1kKilWAGdgfs2BvJb/2Xp8p5iuDVfZXmhppo+d0Ran1P9TKdjV4ABwAgKXxJ3jcQTqE/2IRfqwnPf8itN8aFZlV3TJPYeD3yWE7IT55Gz6EijUpC7aKeoohTb4w2fpox58wWoF3SNp6sK6jDfiAUGEHYJ9pjU=</Signature>\n" +
                "</License>";
        com.aspose.slides.License aposeLic = new com.aspose.slides.License();
        aposeLic.setLicense(new ByteArrayInputStream(license.getBytes()));
        return aposeLic;
    }

    @Bean("xlslicense")
    public com.aspose.cells.License xlslicense() throws Exception {
        String license = "<License>\n" +
                "  <Data>\n" +
                "    <Products>\n" +
                "      <Product>Aspose.Total for Java</Product>\n" +
                "      <Product>Aspose.Words for Java</Product>\n" +
                "    </Products>\n" +
                "    <EditionType>Enterprise</EditionType>\n" +
                "    <SubscriptionExpiry>20991231</SubscriptionExpiry>\n" +
                "    <LicenseExpiry>20991231</LicenseExpiry>\n" +
                "    <SerialNumber>8bfe198c-7f0c-4ef8-8ff0-acc3237bf0d7</SerialNumber>\n" +
                "  </Data>\n" +
                "  <Signature>sNLLKGMUdF0r8O1kKilWAGdgfs2BvJb/2Xp8p5iuDVfZXmhppo+d0Ran1P9TKdjV4ABwAgKXxJ3jcQTqE/2IRfqwnPf8itN8aFZlV3TJPYeD3yWE7IT55Gz6EijUpC7aKeoohTb4w2fpox58wWoF3SNp6sK6jDfiAUGEHYJ9pjU=</Signature>\n" +
                "</License>";
        com.aspose.cells.License aposeLic = new com.aspose.cells.License();
        aposeLic.setLicense(new ByteArrayInputStream(license.getBytes()));
        return aposeLic;
    }
}
