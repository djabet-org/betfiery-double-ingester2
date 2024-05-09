package hello;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.beans.EventHandler;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class Application {
    private final static Log logger = LogFactory.getLog(Application.class);
    private static HttpClient client = HttpClient.newHttpClient();

    private static String SMASH_PROVIDER = "644c2f2f95e6be2d44a2e277";
    private static String BETFIERY_PROVIDER = "644c2d334be055188b0e6237";
    private static String CHILLBET_PROVIDER = "64d39889c0731c96defe6fe6";

    private static Map<String, String> providersMap = Map.of(
    SMASH_PROVIDER, "smash",
    BETFIERY_PROVIDER, "betfiery",
    CHILLBET_PROVIDER, "chillbet"
);

    public static void consumeServerSentEvent(String providerId) {
        try {
        String url = "https://live.tipminer.com/rounds/DOUBLE/%s/live";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format(url, providerId)))
                .GET()
                .timeout(Duration.ofSeconds(120))
                .build();

        Stream<String> linesInResponse = client.send(request, HttpResponse.BodyHandlers.ofLines()).body();
        linesInResponse.filter( data -> data.contains("data")).map(data -> data.split(": ")[1]).forEach(data -> _save(data, providerId));
        } catch (Throwable ex) {
            logger.error(providerId, ex);
            consumeServerSentEvent(providerId);
        }
    }

    private static void _save(String data, String providerId) {
        try {
            String url = "https://cassino-database-manager-production.up.railway.app/api/%s/double/save";
            String platform = providersMap.get(providerId);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format(url, platform )))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(_mapRoll(data, platform)))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            logger.info(response.body());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static String _mapRoll(String data, String platform) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode jsonRoot = (ArrayNode) mapper.readTree(data);
        JsonNode json = jsonRoot.get(0);
        ObjectNode mapped = mapper.createObjectNode();
        
        mapped.put("roll", json.get("result").asInt());
        mapped.put("color", _mapColor(json.get("result").asInt()));
        mapped.put("platform", platform);
        mapped.put("created", json.get("date"));
        mapped.put("total_red_money", 0);
        mapped.put("total_black_money", 0);
        mapped.put("total_white_money", 0);
        logger.info(mapped);

        return mapped.toString();
    }

    private static String _mapColor(int number) {
        return number == 0 ? "white" : number < 8 ? "red": "black";
    }

    public static void main(String[] args) {
        new Thread(new Runnable() {
            @Override
            public void run() {
        consumeServerSentEvent(BETFIERY_PROVIDER);
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
        consumeServerSentEvent(SMASH_PROVIDER);
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
        consumeServerSentEvent(CHILLBET_PROVIDER);
            }
        }).start();
    }
}
