package com.onefly.united.oss.web;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "UNITED.OSS")
public interface UnitedSsoClient {
}
