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

    public static void consumeServerSentEvent() throws IOException, InterruptedException {
        String url = "https://live.tipminer.com/rounds/DOUBLE/644c2d334be055188b0e6237/live";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(120))
                .build();

        Stream<String> linesInResponse = client.send(request, HttpResponse.BodyHandlers.ofLines()).body();
        linesInResponse.filter( data -> data.contains("data")).map(data -> data.split(": ")[1]).forEach(Application::_save);
    }

    private static void _save(String data) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://cassino-database-manager-production.up.railway.app/api/betfiery/double/save"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(_mapRoll(data)))
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

    private static String _mapRoll(String data) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode jsonRoot = (ArrayNode) mapper.readTree(data);
        JsonNode json = jsonRoot.get(0);
        ObjectNode mapped = mapper.createObjectNode();
        
        mapped.put("roll", json.get("result").asInt());
        mapped.put("color", _mapColor(json.get("result").asInt()));
        mapped.put("platform", "betfiery");
        mapped.put("created", json.get("date"));
        mapped.put("total_red_money", 0);
        mapped.put("total_black_money", 0);
        mapped.put("total_white_money", 0);
        System.out.println(mapped.toString());

        return mapped.toString();
    }

    private static String _mapColor(int number) {
        return number == 0 ? "white" : number < 8 ? "red": "black";
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        consumeServerSentEvent();
    }
}
