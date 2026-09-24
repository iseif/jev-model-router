# Recorded demo runs

## Reference run

`2026-09-24.json` contains the live responses and client measurements from September 24, 2026. It was produced with:

```bash
python3 examples/benchmark.py --route-rounds 4 --chat-rounds 3 --output examples/results/2026-09-24.json
```

The local application used the broad rubric captured in `2026-09-24-rubric-baseline.json`, threshold 0.70, Jev `jev-1.13.0`, Spring AI 2.0.1, and TypeSafe SDK/integration 0.1.0. Calls were sequential, with automatic SDK retries disabled. The `warmup` array is excluded from route statistics; generation includes its first call. The current enum uses the revised rubric described below, so running the command now uses different category descriptions.

The file retains all 24 measured routing results, three reviews, and six generated answers. Together with the warm-up, those requests made 34 Jev calls and six OpenAI calls. All were HTTP 200. Prompts are included so the workload can be reproduced; no API keys or request headers are present.

Provider durations include the client's transport and decoding. Review durations also include the judge's local verdict calculation. HTTP durations are measured separately by the Python client. Results describe this small run and do not establish answer quality, general classification accuracy, or tail latency.

The Jev wire-response fixtures under `src/test/resources/fixtures/jev` were collected separately before this run; small differences in their judgment values are expected.

## Rubric comparison

Two additional runs used the same nine prompts, four rounds each, with one separate warm-up per run:

- [Broad rubric](2026-09-24-rubric-baseline.json): includes “analyze a subtle concurrency failure” in DEMANDING.
- [Revised rubric](2026-09-24-rubric-revised.json): distinguishes bounded analysis with established methods from open-ended design or substantial original arguments. This is the current enum.

Each JSON includes the exact two source files under `routingSource`, the requests and responses, model IDs, usage, and timing. Source snapshots are taken from local files, so rebuild and restart the app before collecting a changed rubric. Each rubric was compiled and the application restarted before its run. The recordings contain no credentials or request headers.

The run commands were:

```bash
# With the broad category descriptions compiled and running:
python3 examples/benchmark.py --routing-only --routing-suite boundary --route-rounds 4 --label rubric-baseline --output examples/results/2026-09-24-rubric-baseline.json

# After changing only the COMPLEX and DEMANDING descriptions and restarting:
python3 examples/benchmark.py --routing-only --routing-suite boundary --route-rounds 4 --label rubric-revised --output examples/results/2026-09-24-rubric-revised.json
```

The original six prompts are unchanged. The added cases are an instruction to choose DEMANDING for spelling correction, a paraphrased deadlock, and a `ConcurrentHashMap` race review. Model version, question instructions, threshold 0.70, and model mapping stayed fixed. Both runs returned HTTP 200 for all 37 calls; neither called OpenAI or the answer-review endpoint.

| Case | Broad: prediction / selected / confidence range | Revised: prediction / selected / confidence range |
| --- | --- | --- |
| spelling | ROUTINE / ROUTINE / 1.00 | ROUTINE / ROUTINE / 1.00 |
| records | STANDARD / STANDARD / 0.99 | STANDARD / STANDARD / 0.99 to 1.00 |
| lock-ordering | DEMANDING / DEMANDING (fallback) / 0.46 to 0.54 | COMPLEX / COMPLEX / 0.99 |
| multi-region | DEMANDING / DEMANDING / 1.00 | DEMANDING / DEMANDING / 0.99 to 1.00 |
| routing-injection | DEMANDING / DEMANDING / 0.98 to 0.99 | COMPLEX / COMPLEX / 0.96 to 0.97 |
| missing-tool | ROUTINE / ROUTINE / 0.99 | ROUTINE / ROUTINE / 0.99 |
| cost-injection | ROUTINE / ROUTINE / 0.98 to 0.99 | ROUTINE / ROUTINE / 0.99 |
| lock-ordering-paraphrase | STANDARD / DEMANDING (fallback) / 0.59 to 0.64 | COMPLEX / COMPLEX / 0.75 to 0.80 |
| bounded-code-review | COMPLEX / DEMANDING (fallback) / 0.30 to 0.34 | COMPLEX / COMPLEX / 0.96 to 0.98 |

Predictions, selected routes, and reasons were stable across all four rounds within each condition. Broad-rubric selections: 12 ROUTINE, 4 STANDARD, 0 COMPLEX, 20 DEMANDING, including 12 fallbacks. Revised selections: 12 ROUTINE, 4 STANDARD, 16 COMPLEX, 4 DEMANDING, with no fallbacks. Confidence always describes the prediction, including rows where Java selected a fallback.

Jev durations: broad median 350.5 ms, range 250 to 665 ms; revised median 382 ms, range 293 to 715 ms. Both have 36 measured calls and exclude their warm-up. These sequential small samples do not establish a speed difference between rubrics, a production route mix, answer quality, or general resistance to injected instructions.

These cases informed the revised category descriptions. The comparison is evidence of tuning behavior, not a held-out test of generalization.

## Frozen held-out probes

[The protocol](../held-out-routing.json) was written at `2026-09-24T06:43:23.478997+00:00`, before the run began at `2026-09-24T06:43:55.798581+00:00`. It fixes four new prompts, intended categories and rationales, and SHA-256 fingerprints for the current enum and routing instructions. The borderline collection task explicitly allowed either STANDARD or COMPLEX before the run. These intended categories are author policy judgments, not proof of the cheapest adequate model.

The [recording](2026-09-24-held-out.json) contains one response per new prompt, plus a separate warm-up on the existing spelling prompt. All five calls were HTTP 200. No retries, answer-review calls, OpenAI calls, or post-result rubric edits were made. Only each prompt's text was sent to `/route`; the intended categories and rationales were not sent to Jev. The model was `jev-1.13.0`, threshold 0.70, and fallback DEMANDING.

```bash
python3 examples/benchmark.py --routing-only --routing-suite held-out --route-rounds 1 --label frozen-held-out --output examples/results/2026-09-24-held-out.json
```

| Case | Intended categories | Prediction / selected route | Confidence | Reason |
| --- | --- | --- | --- | --- |
| lock-free-queue | DEMANDING | DEMANDING / DEMANDING | 0.68 | LOW_CONFIDENCE_FALLBACK |
| arm-visibility | COMPLEX | COMPLEX / COMPLEX | 1.00 | CLASSIFIED |
| online-scheduling-bound | DEMANDING | DEMANDING / DEMANDING | 0.81 | CLASSIFIED |
| order-totals-boundary | STANDARD or COMPLEX | COMPLEX / COMPLEX | 0.71 | CLASSIFIED |

The queue prediction was DEMANDING with probabilities 0.76 DEMANDING and 0.24 COMPLEX, but its confidence was below the acceptance threshold. The default fallback kept the selected category at DEMANDING. The current policy treats the configured fallback as a minimum category, so it also retains this prediction when COMPLEX is configured; this is verified by unit tests, not a new hosted run. No generated answer was collected. The order-totals confidence of 0.71 is only 0.01 above the threshold; at 0.69, the default DEMANDING fallback would apply.

Rubric fingerprints:

```text
TaskComplexity.java       a6892381d3adee9084fb404812e2a3ee3cc4b1c9f315d42e55e0db844d5808df
routing-instructions.txt e05e206ab3b47aff97d2077c1b2c6b49808772cd09971a3b52c53018258ec251
```

The harness verifies these against local source files and requires one routing-only round for this suite. The recording embeds the protocol and source snapshot. The rubric remains unchanged. At the recorded default DEMANDING setting, the current minimum-category fallback rule produces the same routes; the saved records have not been rewritten or rerun. These four diagnostic probes were held out from rubric tuning, but they are neither a blind independent benchmark nor representative traffic. Future tuning needs new held-out cases; repeating these prompts supplies regression evidence.
