    package hello;

    import com.fasterxml.jackson.databind.JsonNode;
    import com.fasterxml.jackson.databind.ObjectMapper;
    import com.fasterxml.jackson.databind.node.ArrayNode;
    import com.fasterxml.jackson.databind.node.ObjectNode;
    import org.apache.commons.logging.Log;
    import org.apache.commons.logging.LogFactory;
    import org.springframework.core.ParameterizedTypeReference;
    import org.springframework.http.MediaType;
    import org.springframework.http.codec.ServerSentEvent;
    import org.springframework.util.StringUtils;
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
    import java.util.Optional;
    import java.util.Timer;
    import java.util.TimerTask;

    public class Application {
        private final static Log logger = LogFactory.getLog(Application.class);
        private static HttpClient client = HttpClient.newHttpClient();
        private static Stream<String> linesInResponse;

        private static String SMASH_PROVIDER = "644c2f2f95e6be2d44a2e277";
        private static String BETFIERY_PROVIDER = "644c2d334be055188b0e6237";
        private static String CHILLBET_PROVIDER = "64d39889c0731c96defe6fe6";

        private static Map<String, String> providersMap = Map.of(
        "smash",SMASH_PROVIDER, 
        "betfiery",BETFIERY_PROVIDER, 
    "chillbet",CHILLBET_PROVIDER 
    );

        public static void consumeServerSentEvent() {
            String url = "https://live.tipminer.com/rounds/DOUBLE/%s/live";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format(url, System.getenv("PLATFORM_ID"))))
                    .GET()
                    .timeout(Duration.ofSeconds(120))
                    .build();

            try {
            linesInResponse = client.send(request, HttpResponse.BodyHandlers.ofLines()).body();
            linesInResponse.filter( data -> data.contains("data")).map(data -> _mapRoll(data)).forEach(data -> _save(data));
                
            } catch (Exception ex) {
                // TODO: handle exception
                logger.error(ex);
            }
        }

        private static void _save(String data) {
            try {
                String url = "https://djabet-repository-api-production.up.railway.app/api/double/save";

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(String.format(url, System.getenv("PLATFORM") )))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(data))
                        .timeout(Duration.ofSeconds(10))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                logger.info(response);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        private static String _mapRoll(String data) {
            try {
            String jsonData = data.split(": ")[1];
            ObjectMapper mapper = new ObjectMapper();
            ArrayNode jsonRoot = (ArrayNode) mapper.readTree(jsonData);
            JsonNode json = jsonRoot.get(0);
            ObjectNode mapped = mapper.createObjectNode();
            
            mapped.put("roll", json.get("result").asInt());
            mapped.put("color", _mapColor(json.get("result").asInt()));
            mapped.put("platform", System.getenv("PLATFORM"));
            mapped.put("total_red_money", 0);
            mapped.put("total_black_money", 0);
            mapped.put("total_white_money", 0);
            logger.info(mapped);

            return mapped.toString();
            }
            catch (Exception ex){
                logger.error(ex);
                return null;
            }
        }

        private static String _mapColor(int number) {
            return number == 0 ? "white" : number < 8 ? "red": "black";
        }

        public static void main(String[] args) {
            String platform = System.getenv("PLATFORM");
            String platformId = System.getenv("PLATFORM_ID");

            if (!StringUtils.hasText(platform) || !StringUtils.hasText(platformId)) {
                throw new RuntimeException("Please provide platform and platform id.");
            }

            Timer timer = new Timer ();
            TimerTask hourlyTask = new TimerTask () {
        @Override
        public void run () {
            logger.info("creu");
            // your code here...
            if (linesInResponse != null) {
                linesInResponse.close();
            }
            consumeServerSentEvent();
        }
    };

    // schedule the task to run starting now and then every hour...
    timer.schedule (hourlyTask, 0l, 1000*60*60);

        }
    }
