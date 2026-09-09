package com.shanyangcode.infinitechat.realtimecommunicationservice.perf;

import com.shanyangcode.infinitechat.realtimecommunicationservice.websocket.ChannelManager;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Local-only observability endpoint for the performance harness. */
@RestController
@Profile("perf")
@RequestMapping("/internal/perf")
public class PerfMetricsController {

    @GetMapping("/metrics")
    public Map<String, Object> metrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("timestamp", Instant.now().toString());
        metrics.put("activeUsers", ChannelManager.activeUserCount());
        metrics.put("activeChannels", ChannelManager.activeChannelCount());
        return metrics;
    }
}
