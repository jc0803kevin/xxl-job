package com.xuxueli.job.client;

import com.xuxueli.job.client.model.XxlJobInfo;
import com.xxl.job.core.biz.model.ReturnT;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author Luo Bao Ding
 * @since 2019/5/23
 */
@FeignClient(name = "xxl-job-admin", url = "${xxl.job.serverAddresses:http://localhost:8080/xxl-job-admin}",
        configuration = XxlJobFeignClientConfiguration.class)
@RequestMapping(path = "/jobops", produces = {"application/json"}, headers = "Content-Type=application/json")
public interface XxlJobClient {

    @PostMapping("/add")
    ReturnT<String> add(@RequestBody XxlJobInfo jobInfo);

    @PostMapping("/update")
    ReturnT<String> update(@RequestBody XxlJobInfo jobInfo);

    @PostMapping("/remove")
    ReturnT<String> remove(@RequestParam("uniqName") String uniqName);

    @PostMapping("/stop")
    ReturnT<String> stop(@RequestParam("uniqName") String uniqName);

    @PostMapping("/start")
    ReturnT<String> start(@RequestParam("uniqName") String uniqName);

    @PostMapping("/trigger")
    ReturnT<String> trigger(@RequestParam("uniqName") String uniqName,
                            @RequestParam("executorParam") String executorParam);

}
