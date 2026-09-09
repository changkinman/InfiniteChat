package com.shanyangcode.infinitechat.messageingservice.perf;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Sends traceable text messages through the real HTTP -> Kafka path. */
public final class MessageBenchmark {
    private MessageBenchmark() {
    }

    public static void main(String[] args) throws Exception {
        Config config = Config.parse(args);
        Files.createDirectories(config.output);
        ExecutorService executor = Executors.newFixedThreadPool(config.workers);
        List<Future<Result>> futures = new ArrayList<Future<Result>>();
        long intervalNanos = TimeUnit.SECONDS.toNanos(1) / config.ratePerSecond;
        long firstAt = System.nanoTime();

        for (int sequence = 0; sequence < config.messages; sequence++) {
            long scheduledAt = firstAt + intervalNanos * sequence;
            long remaining = scheduledAt - System.nanoTime();
            if (remaining > 0) {
                TimeUnit.NANOSECONDS.sleep(remaining);
            }
            final int currentSequence = sequence;
            futures.add(executor.submit(new Callable<Result>() {
                @Override
                public Result call() {
                    return send(config, currentSequence);
                }
            }));
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);
        List<Result> results = new ArrayList<Result>();
        for (Future<Result> future : futures) {
            results.add(future.get());
        }
        write(config, results);
    }

    private static Result send(Config config, int sequence) {
        long start = System.nanoTime();
        long sentAtMillis = System.currentTimeMillis();
        String content = "perf:" + config.runId + ":" + sequence + ":" + sentAtMillis;
        String request = "{\"sessionId\":" + config.sessionId
                + ",\"sendUserId\":" + config.senderId
                + ",\"sessionType\":1,\"type\":1,\"receiveUserId\":" + config.receiverId
                + ",\"body\":{\"content\":\"" + content + "\",\"replyId\":null}}";
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(config.url).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(15000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            byte[] bytes = request.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bytes.length);
            OutputStream output = connection.getOutputStream();
            output.write(bytes);
            output.close();
            int status = connection.getResponseCode();
            String response = read(status >= 400 ? connection.getErrorStream() : connection.getInputStream());
            return new Result(sequence, status, System.nanoTime() - start, response, null);
        } catch (Exception exception) {
            return new Result(sequence, 0, System.nanoTime() - start, "", exception.getClass().getSimpleName() + ": " + exception.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String read(InputStream stream) throws Exception {
        if (stream == null) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder value = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) value.append(line);
        reader.close();
        return value.toString();
    }

    private static void write(Config config, List<Result> results) throws Exception {
        List<Long> successfulLatencies = new ArrayList<Long>();
        int successful = 0;
        List<String> csv = new ArrayList<String>();
        csv.add("sequence,http_status,latency_ms,error,response");
        for (Result result : results) {
            if (result.status >= 200 && result.status < 300) {
                successful++;
                successfulLatencies.add(result.latencyNanos);
            }
            csv.add(result.sequence + "," + result.status + "," + formatMillis(result.latencyNanos) + ","
                    + csv(result.error) + "," + csv(result.response));
        }
        Collections.sort(successfulLatencies);
        Files.write(config.output.resolve("http-results.csv"), csv, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String summary = "{\n"
                + "  \"generatedAt\": \"" + Instant.now() + "\",\n"
                + "  \"runId\": \"" + config.runId + "\",\n"
                + "  \"messages\": " + config.messages + ",\n"
                + "  \"ratePerSecond\": " + config.ratePerSecond + ",\n"
                + "  \"httpSuccess\": " + successful + ",\n"
                + "  \"httpSuccessRate\": " + String.format(Locale.ROOT, "%.4f", successful * 100.0d / config.messages) + ",\n"
                + "  \"httpP50Ms\": " + formatMillis(percentile(successfulLatencies, 0.50d)) + ",\n"
                + "  \"httpP95Ms\": " + formatMillis(percentile(successfulLatencies, 0.95d)) + ",\n"
                + "  \"httpP99Ms\": " + formatMillis(percentile(successfulLatencies, 0.99d)) + "\n"
                + "}\n";
        Files.write(config.output.resolve("summary.json"), summary.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        System.out.println(summary);
    }

    private static long percentile(List<Long> sorted, double p) {
        if (sorted.isEmpty()) return 0L;
        int index = (int) Math.ceil(sorted.size() * p) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }

    private static String formatMillis(long nanos) {
        return String.format(Locale.ROOT, "%.3f", nanos / 1_000_000.0d);
    }

    private static String csv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"").replace("\n", " ").replace("\r", " ") + "\"";
    }

    private static final class Result {
        private final int sequence;
        private final int status;
        private final long latencyNanos;
        private final String response;
        private final String error;
        private Result(int sequence, int status, long latencyNanos, String response, String error) {
            this.sequence = sequence; this.status = status; this.latencyNanos = latencyNanos;
            this.response = response; this.error = error;
        }
    }

    private static final class Config {
        private String url = "http://127.0.0.1:8081/api/v1/chat/session";
        private int messages = 100;
        private int ratePerSecond = 50;
        private int workers = 16;
        private long sessionId = 9200001L;
        private long senderId = 9000001L;
        private long receiverId = 9000002L;
        private String runId = UUID.randomUUID().toString();
        private Path output = Paths.get("perf", "results", "message-" + System.currentTimeMillis());
        private static Config parse(String[] args) {
            Config config = new Config();
            Map<String, String> values = new HashMap<String, String>();
            for (int i = 0; i < args.length; i += 2) {
                if (!args[i].startsWith("--") || i + 1 >= args.length) throw new IllegalArgumentException("Arguments must be --name value pairs");
                values.put(args[i].substring(2), args[i + 1]);
            }
            if (values.containsKey("url")) config.url = values.get("url");
            if (values.containsKey("messages")) config.messages = Integer.parseInt(values.get("messages"));
            if (values.containsKey("rate-per-second")) config.ratePerSecond = Integer.parseInt(values.get("rate-per-second"));
            if (values.containsKey("workers")) config.workers = Integer.parseInt(values.get("workers"));
            if (values.containsKey("run-id")) config.runId = values.get("run-id");
            if (values.containsKey("output")) config.output = Paths.get(values.get("output"));
            return config;
        }
    }
}
