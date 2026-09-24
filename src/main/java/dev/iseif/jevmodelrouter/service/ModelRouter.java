package dev.iseif.jevmodelrouter.service;

import dev.iseif.jevmodelrouter.model.RoutingDecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ModelRouter {

  private static final Logger logger = LoggerFactory.getLogger(ModelRouter.class);
  private final JevTaskClassifier classifier;
  private final ModelRoutingPolicy policy;

  public ModelRouter(JevTaskClassifier classifier, ModelRoutingPolicy policy) {
    this.classifier = classifier;
    this.policy = policy;
  }

  public RoutingDecision route(String prompt) {
    var decision = policy.decide(classifier.classify(prompt));
    logger.info("Routing prediction={}, selected={}, confidence={}, reason={}",
        decision.predictedComplexity(), decision.selectedComplexity(), decision.confidence(), decision.reason());
    return decision;
  }
}
