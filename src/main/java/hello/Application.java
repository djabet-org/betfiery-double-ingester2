package hello;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.apache.catalina.session.FileStore;
import org.apache.catalina.session.PersistentManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


@Configuration
@ComponentScan
@EnableAutoConfiguration
public class Application {
    private final static Log logger = LogFactory.getLog(Application.class);

    public EmbeddedServletContainerCustomizer containerCustomizer() {
        return factory -> {
            TomcatEmbeddedServletContainerFactory containerFactory = (TomcatEmbeddedServletContainerFactory) factory;
            containerFactory.setTomcatContextCustomizers(Arrays.asList(context -> {
                final PersistentManager persistentManager = new PersistentManager();
                final FileStore store = new FileStore();

                final String sessionDirectory = makeSessionDirectory();
                logger.info("Writing sessions to " + sessionDirectory);
                store.setDirectory(sessionDirectory);

                persistentManager.setStore(store);
                context.setManager(persistentManager);
            }));
        };
    }

    private String makeSessionDirectory() {
        final String cwd = System.getProperty("user.dir");
        return cwd + File.separator + "sessions";
    }

    private static Supplier<RestTemplate> _restTemplate = RestTemplate::new;
    private static Supplier<HttpHeaders> _headers = () -> {
        HttpHeaders headers = new HttpHeaders();
        headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)");
        headers.add("Content-Type", "application/json");
        return headers;
    };
    private static BiFunction<String, String, String> _doPostRequest = (url, postBody) -> {
        System.out.println(postBody);
        HttpEntity<String> requestEntity = new HttpEntity<String>(postBody, _headers.get());

        return _restTemplate.get().postForEntity(
                url, requestEntity, String.class).getBody();
    };

    private final static Function<JsonNode, Roll> _mapRoll = (rollNode) ->
    Roll.with(
                rollNode.get("color").asText().toLowerCase(),
                rollNode.get("roll").asText().equalsIgnoreCase("wild") ? "0": rollNode.get("roll").asText(),
                rollNode.get("create_time").asText(),
                "betfiery",
                rollNode.get("round_id").asInt()
        );

    private final static Function<String, JsonNode> _doGetRecentRollData = (response) ->
    {
        try {
            return new ObjectMapper().readTree(response).get("data").get("list").get(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    };

    public static void main(String[] args) throws InterruptedException, IOException {
        int lastId = -1;
        while (true) {
            Roll recentRollData = _doPostRequest.andThen(_doGetRecentRollData).andThen(_mapRoll).apply("https://api.betfiery.com/game/double/list", "");
            logger.info("roll -> "+recentRollData);
            if (lastId != recentRollData.getId()) {
                lastId = recentRollData.getId();
                String result = _doPostRequest.apply("https://cassino-database-manager-production.up.railway.app/api/betfiery/double/save",
                        new ObjectMapper().valueToTree(recentRollData).toString());
                logger.info("manager api response -> "+result);
                Thread.sleep(22*1000);
            }
        }
    }
}
