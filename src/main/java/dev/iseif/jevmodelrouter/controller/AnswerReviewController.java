package dev.iseif.jevmodelrouter.controller;

import dev.iseif.jevmodelrouter.model.AnswerReview;
import dev.iseif.jevmodelrouter.model.AnswerReviewRequest;
import dev.iseif.jevmodelrouter.service.JevAnswerReviewer;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnswerReviewController {

  private final JevAnswerReviewer reviewer;

  public AnswerReviewController(JevAnswerReviewer reviewer) {
    this.reviewer = reviewer;
  }

  @PostMapping("/answer-reviews")
  public AnswerReview review(@Valid @RequestBody AnswerReviewRequest request) {
    return reviewer.review(request);
  }
}
