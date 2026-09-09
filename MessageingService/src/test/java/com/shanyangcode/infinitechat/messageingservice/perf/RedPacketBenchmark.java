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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Exercises the real red-packet send/receive HTTP endpoints with a synchronized claim burst. */
public final class RedPacketBenchmark {
    private static final Pattern RED_PACKET_ID = Pattern.compile("\\\"redPacketId\\\"\\s*:\\s*\\\"?(\\d+)\\\"?");
    private RedPacketBenchmark() { }

    public static void main(String[] args) throws Exception {
        int concurrency = Integer.parseInt(value(args, "--concurrency", "100"));
        int rounds = Integer.parseInt(value(args, "--rounds", "10"));
        Path output = Paths.get(value(args, "--output", "perf/results/red-packet-" + System.currentTimeMillis()));
        Files.createDirectories(output);
        List<Result> all = new ArrayList<Result>();
        List<String> packetIds = new ArrayList<String>();
        for (int round = 0; round < rounds; round++) {
            String send = request("http://127.0.0.1:8081/api/v1/chat/redPacket/send",
                    "{\"sessionId\":9200001,\"receiveUserId\":9000002,\"sendUserId\":9000001,\"type\":5,\"sessionType\":1,\"body\":{\"redPacketType\":1,\"totalAmount\":100.00,\"totalCount\":100,\"redPacketWrapperText\":\"perf\"}}");
            Matcher matcher = RED_PACKET_ID.matcher(send);
            if (!matcher.find()) throw new IllegalStateException("send did not return redPacketId: " + send);
            String packetId = matcher.group(1);
            packetIds.add(packetId);
            final CyclicBarrier barrier = new CyclicBarrier(concurrency);
            ExecutorService workers = Executors.newFixedThreadPool(concurrency);
            List<Future<Result>> futures = new ArrayList<Future<Result>>();
            for (int i = 0; i < concurrency; i++) {
                final int sequence = round * concurrency + i;
                final long userId = 9000002L + i;
                futures.add(workers.submit(new Callable<Result>() {
                    @Override public Result call() throws Exception {
                        barrier.await(30, TimeUnit.SECONDS);
                        long start = System.nanoTime();
                        String body = "{\"userId\":" + userId + ",\"redPacketId\":" + packetId + "}";
                        try {
                            String response = request("http://127.0.0.1:8081/api/v1/chat/redPacket/receive", body);
                            return new Result(sequence, packetId, userId, System.nanoTime() - start, response, null);
                        } catch (Exception e) {
                            return new Result(sequence, packetId, userId, System.nanoTime() - start, "", e.toString());
                        }
                    }
                }));
            }
            workers.shutdown(); workers.awaitTermination(2, TimeUnit.MINUTES);
            for (Future<Result> future : futures) all.add(future.get());
        }
        write(output, concurrency, rounds, packetIds, all);
    }

    private static String request(String target, String body) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(target).openConnection();
        try {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            connection.setRequestMethod("POST"); connection.setConnectTimeout(5000); connection.setReadTimeout(30000);
            connection.setDoOutput(true); connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setFixedLengthStreamingMode(bytes.length);
            OutputStream out = connection.getOutputStream(); out.write(bytes); out.close();
            int status = connection.getResponseCode();
            String response = read(status >= 400 ? connection.getErrorStream() : connection.getInputStream());
            if (status < 200 || status >= 300) throw new IllegalStateException("HTTP " + status + ": " + response);
            return response;
        } finally { connection.disconnect(); }
    }

    private static String read(InputStream stream) throws Exception {
        if (stream == null) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder text = new StringBuilder(); String line;
        while ((line = reader.readLine()) != null) text.append(line);
        reader.close(); return text.toString();
    }

    private static void write(Path output, int concurrency, int rounds, List<String> packetIds, List<Result> results) throws Exception {
        List<String> csv = new ArrayList<String>(); csv.add("sequence,red_packet_id,user_id,latency_ms,outcome,error,response");
        List<Long> successful = new ArrayList<Long>(); int claimed = 0, errors = 0;
        for (Result result : results) {
            String outcome = result.error != null ? "error" : (result.response.contains("\"receivedAmount\":null") ? "claimed" : "received");
            if ("received".equals(outcome)) successful.add(result.latency); else if ("claimed".equals(outcome)) claimed++; else errors++;
            csv.add(result.sequence + "," + result.packetId + "," + result.userId + "," + ms(result.latency) + "," + outcome + "," + quote(result.error) + "," + quote(result.response));
        }
        Collections.sort(successful);
        Files.write(output.resolve("requests.csv"), csv, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        String summary = "{\n  \"generatedAt\":\"" + Instant.now() + "\",\n  \"concurrency\":" + concurrency + ",\n  \"rounds\":" + rounds
                + ",\n  \"packets\":\"" + join(packetIds) + "\",\n  \"requests\":" + results.size() + ",\n  \"received\":" + successful.size()
                + ",\n  \"claimed\":" + claimed + ",\n  \"errors\":" + errors + ",\n  \"p50Ms\":" + ms(percentile(successful,.5))
                + ",\n  \"p95Ms\":" + ms(percentile(successful,.95)) + ",\n  \"p99Ms\":" + ms(percentile(successful,.99)) + "\n}\n";
        Files.write(output.resolve("summary.json"), summary.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        System.out.println(summary);
    }
    private static String value(String[] args, String key, String fallback) { for (int i=0;i+1<args.length;i++) if(key.equals(args[i])) return args[i+1]; return fallback; }
    private static long percentile(List<Long> values, double p) { if(values.isEmpty()) return 0; return values.get(Math.max(0, (int)Math.ceil(values.size()*p)-1)); }
    private static String ms(long nanos) { return String.format(java.util.Locale.ROOT, "%.3f", nanos / 1000000d); }
    private static String quote(String value) { return "\"" + (value == null ? "" : value.replace("\"", "\"\"").replace("\n", " ")) + "\""; }
    private static String join(List<String> values) { StringBuilder b=new StringBuilder(); for(String v:values){if(b.length()>0)b.append(',');b.append(v);} return b.toString(); }
    private static final class Result { final int sequence; final String packetId; final long userId; final long latency; final String response; final String error; Result(int s,String p,long u,long l,String r,String e){sequence=s;packetId=p;userId=u;latency=l;response=r;error=e;} }
}
