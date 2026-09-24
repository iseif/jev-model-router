package dev.iseif.jevmodelrouter;

import dev.iseif.jevmodelrouter.support.ProviderStub;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = {
    "spring.ai.typesafe.api-key=test-typesafe",
    "spring.ai.typesafe.model=jev-test-model",
    "spring.ai.openai.api-key=test-openai",
    "spring.ai.openai.chat.api-key=test-openai",
    "spring.ai.typesafe.retry.max-retries=0",
    "spring.ai.openai.max-retries=0",
    "spring.ai.openai.chat.max-retries=0",
    "routing.min-confidence=0.70",
    "routing.low-confidence-fallback=DEMANDING",
    "answer-review.min-grounded-probability=0.85",
    "answer-review.min-completeness-score=1.5",
    "answer-review.min-completeness-confidence=0.70",
    "routing.models.routine=test-routine",
    "routing.models.standard=test-standard",
    "routing.models.complex=test-complex",
    "routing.models.demanding=test-demanding"
})
@Import(ProviderApiTest.FixtureConfiguration.class)
@AutoConfigureMockMvc
abstract class ProviderApiTest {

  protected static String fixture(String name) throws IOException {
    return new ClassPathResource("fixtures/jev/" + name + ".json")
        .getContentAsString(StandardCharsets.UTF_8);
  }

  protected static final ProviderStub PROVIDER = new ProviderStub();
  protected static final JsonMapper JSON = JsonMapper.builder().build();

  @Autowired
  protected MockMvc mvc;

  @DynamicPropertySource
  static void providerUrls(DynamicPropertyRegistry registry) {
    registry.add("spring.ai.typesafe.base-url", PROVIDER::baseUrl);
    registry.add("spring.ai.openai.base-url", () -> PROVIDER.baseUrl() + "/v1");
    registry.add("spring.ai.openai.chat.base-url", () -> PROVIDER.baseUrl() + "/v1");
  }

  @BeforeEach
  void resetProvider() {
    PROVIDER.reset();
  }

  protected static String reviewRequest() {
    return """
        {"question":"When can I return an unopened item?",
         "reference":"Unopened items may be returned within 30 days.",
         "answer":"You may return it within 30 days."}
        """;
  }

  protected static String assessment(double grounded, double score, double confidence) {
    return """
        {"model":"jev-1.13.0","answers":{
          "grounded":{"type":"noul","noul":%s},
          "completeness":{"type":"score","score":%s,"confidence":%s,
            "legend":{"0":"Does not answer","1":"Partly answers","2":"Fully answers"},
            "probabilities":{"0":0.0,"1":%s,"2":%s}}},
          "usage":{"input_tokens":150,"output_tokens":35}}
        """.formatted(grounded, score, confidence, 2.0 - score, score - 1.0);
  }

  protected static String choice(String selected, double confidence) {
    return """
        {"model":"jev-1.13.0","answers":{"complexity":{"type":"choice","choice":"%s",
          "confidence":%s,"probabilities":{"ROUTINE":%s,"STANDARD":%s,"COMPLEX":%s,"DEMANDING":%s}}},
          "usage":{"input_tokens":100,"output_tokens":20}}
        """.formatted(selected, confidence,
        selected.equals("ROUTINE") ? 0.85 : 0.05,
        selected.equals("STANDARD") ? 0.85 : 0.05,
        selected.equals("COMPLEX") ? 0.85 : 0.05,
        selected.equals("DEMANDING") ? 0.85 : 0.05);
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class FixtureConfiguration {
    @Bean(destroyMethod = "close")
    ProviderStub providerStub() {
      return PROVIDER;
    }
  }
}
