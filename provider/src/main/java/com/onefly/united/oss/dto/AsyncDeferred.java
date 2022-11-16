package com.onefly.united.oss.dto;

import com.onefly.united.common.utils.Result;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.context.request.async.DeferredResult;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsyncDeferred {
    private MultipartFileParamDto param;
    private DeferredResult<Result<SysOssDto>> result;
}
