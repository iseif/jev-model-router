package dev.iseif.jevmodelrouter.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Local HTTP fixtures exercise both real SDKs without credentials or remote requests. */
public final class ProviderStub implements AutoCloseable {

  private final HttpServer server;
  private final List<String> jevRequests = new CopyOnWriteArrayList<>();
  private final List<String> chatRequests = new CopyOnWriteArrayList<>();
  private volatile Reply jev = new Reply(500, "{}");
  private volatile Reply chat = new Reply(500, "{}");

  public ProviderStub() {
    try {
      server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext("/v1/systemone", exchange -> respond(exchange, jevRequests, jev));
      server.createContext("/v1/chat/completions", exchange -> respond(exchange, chatRequests, chat));
      server.start();
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  public String baseUrl() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  public void reset() {
    jevRequests.clear();
    chatRequests.clear();
    jev = new Reply(500, "{}");
    chat = new Reply(500, "{}");
  }

  public void jev(int status, String body) {
    jev = new Reply(status, body);
  }

  public void chat(int status, String body) {
    chat = new Reply(status, body);
  }

  public List<String> jevRequests() {
    return List.copyOf(jevRequests);
  }

  public List<String> chatRequests() {
    return List.copyOf(chatRequests);
  }

  private void respond(HttpExchange exchange, List<String> requests, Reply reply) throws IOException {
    try (exchange) {
      requests.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
      byte[] bytes = reply.body().getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(reply.status(), bytes.length);
      exchange.getResponseBody().write(bytes);
    }
  }

  @Override
  public void close() {
    server.stop(0);
  }

  private record Reply(int status, String body) {}
}
