package com.xwms.common.feign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import com.xwms.common.core.Result;
import com.xwms.common.feign.dto.OwnerDTO;

/** 货主Feign客户端 */
@FeignClient(name = "wms-base", contextId = "ownerFeignClient", path = "/api/owner")
public interface OwnerFeignClient {

    @GetMapping("/list")
    Result<List<OwnerDTO>> listAll();
}
