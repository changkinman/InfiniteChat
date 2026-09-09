package com.shanyangcode.infinitechat.realtimecommunicationservice.perf;

import com.shanyangcode.infinitechat.realtimecommunicationservice.utils.JwtUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;

import java.io.IOException;
import java.net.URI;
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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dependency-free (beyond the project's existing Netty/JJWT dependencies)
 * WebSocket load client.  It creates one valid JWT and one socket per user,
 * periodically exercises the project's heartbeat protocol, and writes raw
 * measurements to the supplied output directory.
 */
public final class WebSocketBenchmark {
    private static final String HEARTBEAT = "{\"type\":5}";
    private static final Pattern PERF_MESSAGE_TIMESTAMP = Pattern.compile("perf:[A-Za-z0-9-]+:[0-9]+:([0-9]+)");

    private WebSocketBenchmark() {
    }

    public static void main(String[] args) throws Exception {
        Config config = Config.parse(args);
        Files.createDirectories(config.output);
        Stats stats = new Stats();
        List<ClientConnection> clients = new ArrayList<ClientConnection>();
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup(config.eventLoops);

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class);

            for (int index = 0; index < config.connections; index++) {
                final ClientConnection client = new ClientConnection(
                        String.valueOf(config.userStart + index), stats);
                bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) {
                        DefaultHttpHeaders headers = new DefaultHttpHeaders();
                        headers.set("userUuid", client.userId);
                        headers.set("token", JwtUtil.generate(client.userId));
                        WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                                URI.create("ws://" + config.host + ":" + config.port + "/api/v1/netty"),
                                WebSocketVersion.V13, null, true, headers, 8192);
                        channel.pipeline().addLast(new HttpClientCodec());
                        channel.pipeline().addLast(new HttpObjectAggregator(8192));
                        channel.pipeline().addLast(new WebSocketClientProtocolHandler(handshaker, true));
                        channel.pipeline().addLast(new ClientHandler(client));
                    }
                });

                try {
                    Channel channel = bootstrap.connect(config.host, config.port).sync().channel();
                    client.channel = channel;
                    if (client.handshake.await(config.handshakeTimeoutSeconds, TimeUnit.SECONDS)) {
                        clients.add(client);
                        scheduleHeartbeat(client, config);
                    } else {
                        stats.connectionFailures.increment();
                        client.plannedClose = true;
                        channel.close().syncUninterruptibly();
                    }
                } catch (Exception exception) {
                    stats.connectionFailures.increment();
                    client.failure = exception.getClass().getSimpleName() + ": " + exception.getMessage();
                }

                if (config.rampPerSecond > 0) {
                    Thread.sleep(Math.max(1L, 1000L / config.rampPerSecond));
                }
            }

            Thread.sleep(TimeUnit.SECONDS.toMillis(config.holdSeconds));
            int activeBeforeClose = 0;
            for (ClientConnection client : clients) {
                if (client.channel != null && client.channel.isActive()) {
                    activeBeforeClose++;
                }
            }
            stats.activeBeforeClose.set(activeBeforeClose);

            for (ClientConnection client : clients) {
                client.plannedClose = true;
                if (client.channel != null) {
                    client.channel.close().syncUninterruptibly();
                }
            }
        } finally {
            eventLoopGroup.shutdownGracefully().syncUninterruptibly();
        }

        writeResults(config, stats, clients);
        System.out.println(stats.toConsoleSummary(config));
    }

    /**
     * Start heartbeats as soon as this socket has completed its handshake.
     * Connections are created sequentially during a ramp, so waiting until the
     * entire ramp finishes can leave early connections reader-idle long enough
     * for the server's idle timeout to close them.
     */
    private static void scheduleHeartbeat(final ClientConnection client, Config config) {
        client.channel.eventLoop().scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                client.sendHeartbeat();
            }
        }, 1, config.heartbeatSeconds, TimeUnit.SECONDS);
    }

    private static void writeResults(Config config, Stats stats, List<ClientConnection> clients) throws IOException {
        List<String> rows = new ArrayList<String>();
        rows.add("user_id,handshake_success,heartbeat_sent,heartbeat_received,abnormal_close,failure");
        for (ClientConnection client : clients) {
            rows.add(csv(client.userId) + "," + client.handshakeSuccess + "," + client.heartbeatSent.sum()
                    + "," + client.heartbeatReceived.sum() + "," + client.abnormalClose.get() + ","
                    + csv(client.failure));
        }
        Files.write(config.output.resolve("connections.csv"), rows, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        List<Long> rtts = new ArrayList<Long>(stats.heartbeatRttsNanos);
        Collections.sort(rtts);
        String summary = "{\n"
                + "  \"generatedAt\": \"" + Instant.now() + "\",\n"
                + "  \"target\": \"ws://" + config.host + ":" + config.port + "/api/v1/netty\",\n"
                + "  \"requestedConnections\": " + config.connections + ",\n"
                + "  \"successfulHandshakes\": " + stats.handshakeSuccess.sum() + ",\n"
                + "  \"connectionFailures\": " + stats.connectionFailures.sum() + ",\n"
                + "  \"activeBeforeClose\": " + stats.activeBeforeClose.get() + ",\n"
                + "  \"abnormalCloses\": " + stats.abnormalClose.sum() + ",\n"
                + "  \"heartbeatSent\": " + stats.heartbeatSent.sum() + ",\n"
                + "  \"heartbeatReceived\": " + stats.heartbeatReceived.sum() + ",\n"
                + "  \"heartbeatP50Ms\": " + formatMillis(percentile(rtts, 0.50d)) + ",\n"
                + "  \"heartbeatP95Ms\": " + formatMillis(percentile(rtts, 0.95d)) + ",\n"
                + "  \"heartbeatP99Ms\": " + formatMillis(percentile(rtts, 0.99d)) + ",\n"
                + "  \"applicationMessagesReceived\": " + stats.applicationMessagesReceived.sum() + ",\n"
                + "  \"applicationMessageP95Ms\": " + formatMillis(percentile(new ArrayList<Long>(stats.applicationMessageRttsNanos), 0.95d)) + "\n"
                + "}\n";
        Files.write(config.output.resolve("summary.json"), summary.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static long percentile(List<Long> sorted, double percentile) {
        if (sorted.isEmpty()) {
            return 0L;
        }
        Collections.sort(sorted);
        int index = (int) Math.ceil(percentile * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }

    private static String formatMillis(long nanos) {
        return String.format(Locale.ROOT, "%.3f", nanos / 1_000_000.0d);
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"").replace("\n", " ").replace("\r", " ") + "\"";
    }

    private static final class ClientHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
        private final ClientConnection client;

        private ClientHandler(ClientConnection client) {
            this.client = client;
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext context, Object event) throws Exception {
            if (event == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE) {
                client.handshakeSuccess = true;
                client.stats.handshakeSuccess.increment();
                client.handshake.countDown();
            }
            super.userEventTriggered(context, event);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext context, TextWebSocketFrame frame) {
            String payload = frame.text();
            if (payload.contains("\"type\":5")) {
                long sentAt = client.lastHeartbeatSentNanos.getAndSet(0L);
                if (sentAt > 0L) {
                    client.heartbeatReceived.increment();
                    client.stats.heartbeatReceived.increment();
                    client.stats.heartbeatRttsNanos.add(System.nanoTime() - sentAt);
                }
            }
            Matcher matcher = PERF_MESSAGE_TIMESTAMP.matcher(payload);
            if (matcher.find()) {
                long sentAtMillis = Long.parseLong(matcher.group(1));
                client.stats.applicationMessagesReceived.increment();
                client.stats.applicationMessageRttsNanos.add(
                        TimeUnit.MILLISECONDS.toNanos(Math.max(0L, System.currentTimeMillis() - sentAtMillis)));
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext context) throws Exception {
            if (!client.plannedClose && client.closed.compareAndSet(false, true)) {
                client.abnormalClose.set(true);
                client.stats.abnormalClose.increment();
            }
            super.channelInactive(context);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
            client.failure = cause.getClass().getSimpleName() + ": " + cause.getMessage();
            context.close();
        }
    }

    private static final class ClientConnection {
        private final String userId;
        private final Stats stats;
        private final CountDownLatch handshake = new CountDownLatch(1);
        private final AtomicLong lastHeartbeatSentNanos = new AtomicLong();
        private final LongAdder heartbeatSent = new LongAdder();
        private final LongAdder heartbeatReceived = new LongAdder();
        private final AtomicBoolean closed = new AtomicBoolean();
        private final AtomicBoolean abnormalClose = new AtomicBoolean();
        private volatile Channel channel;
        private volatile boolean handshakeSuccess;
        private volatile boolean plannedClose;
        private volatile String failure;

        private ClientConnection(String userId, Stats stats) {
            this.userId = userId;
            this.stats = stats;
        }

        private void sendHeartbeat() {
            if (channel == null || !channel.isActive()) {
                return;
            }
            long now = System.nanoTime();
            if (lastHeartbeatSentNanos.compareAndSet(0L, now)) {
                heartbeatSent.increment();
                stats.heartbeatSent.increment();
                channel.writeAndFlush(new TextWebSocketFrame(HEARTBEAT));
            }
        }
    }

    private static final class Stats {
        private final LongAdder handshakeSuccess = new LongAdder();
        private final LongAdder connectionFailures = new LongAdder();
        private final LongAdder heartbeatSent = new LongAdder();
        private final LongAdder heartbeatReceived = new LongAdder();
        private final LongAdder abnormalClose = new LongAdder();
        private final ConcurrentLinkedQueue<Long> heartbeatRttsNanos = new ConcurrentLinkedQueue<Long>();
        private final LongAdder applicationMessagesReceived = new LongAdder();
        private final ConcurrentLinkedQueue<Long> applicationMessageRttsNanos = new ConcurrentLinkedQueue<Long>();
        private final AtomicInteger activeBeforeClose = new AtomicInteger();

        private String toConsoleSummary(Config config) {
            List<Long> values = new ArrayList<Long>(heartbeatRttsNanos);
            Collections.sort(values);
            return "WebSocket benchmark completed: requested=" + config.connections
                    + ", handshakeSuccess=" + handshakeSuccess.sum()
                    + ", active=" + activeBeforeClose.get()
                    + ", abnormalClose=" + abnormalClose.sum()
                    + ", heartbeatP95Ms=" + formatMillis(percentile(values, 0.95d))
                    + ", output=" + config.output;
        }
    }

    private static final class Config {
        private String host = "127.0.0.1";
        private int port = 9000;
        private int connections = 100;
        private int rampPerSecond = 25;
        private int holdSeconds = 600;
        private int heartbeatSeconds = 30;
        private int handshakeTimeoutSeconds = 10;
        private long userStart = 9000001L;
        private int eventLoops = Math.max(2, Math.min(8, Runtime.getRuntime().availableProcessors()));
        private Path output = Paths.get("perf", "results", "ws-" + System.currentTimeMillis());

        private static Config parse(String[] args) {
            Config config = new Config();
            Map<String, String> values = new HashMap<String, String>();
            for (int i = 0; i < args.length; i += 2) {
                if (!args[i].startsWith("--") || i + 1 >= args.length) {
                    throw new IllegalArgumentException("Arguments must be --name value pairs");
                }
                values.put(args[i].substring(2), args[i + 1]);
            }
            if (values.containsKey("host")) config.host = values.get("host");
            if (values.containsKey("port")) config.port = Integer.parseInt(values.get("port"));
            if (values.containsKey("connections")) config.connections = Integer.parseInt(values.get("connections"));
            if (values.containsKey("ramp-per-second")) config.rampPerSecond = Integer.parseInt(values.get("ramp-per-second"));
            if (values.containsKey("hold-seconds")) config.holdSeconds = Integer.parseInt(values.get("hold-seconds"));
            if (values.containsKey("heartbeat-seconds")) config.heartbeatSeconds = Integer.parseInt(values.get("heartbeat-seconds"));
            if (values.containsKey("handshake-timeout-seconds")) config.handshakeTimeoutSeconds = Integer.parseInt(values.get("handshake-timeout-seconds"));
            if (values.containsKey("user-start")) config.userStart = Long.parseLong(values.get("user-start"));
            if (values.containsKey("event-loops")) config.eventLoops = Integer.parseInt(values.get("event-loops"));
            if (values.containsKey("output")) config.output = Paths.get(values.get("output"));
            return config;
        }
    }
}
