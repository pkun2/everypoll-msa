package com.everypoll.pollService.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "rag-agent", url = "${rag.agent.url:http://rag-agent:8000}")
public interface RagAgentClient {

    @GetMapping("/api/v1/polls/{pollId}/summary")
    Map<String, Object> getPollSummary(@PathVariable("pollId") Long pollId);
}
