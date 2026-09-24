# Recorded Jev responses

Captured September 24, 2026 from `jev-1.13.0` using the broad routing rubric and default answer-review thresholds. These are real HTTP response bodies, pretty-printed for review. No request headers, API keys, or transport identifiers are retained. The routing rubric has since been revised; these fixtures test response handling independently of the current descriptions.

- `routing-spelling.json`: prompt `Correct the spelling: I recieved your mesage.`
- `review-supported.json`: answer `You may return an unopened item within 30 days if you have a receipt.`
- `review-contradictory.json`: answer `You have 90 days, and no receipt is needed.`
- `review-incomplete.json`: answer `You can return it.`

All reviews use question `When can I return an unopened item?` and reference `Unopened items may be returned within 30 days. A receipt is required.`

These four fixture-collection calls are separate from the later timing run in `examples/results/2026-09-24.json`. Their numbers can differ: for example, the contradictory review scored 0.75 during fixture collection and 0.71 during the benchmark. Tests replay the captured bodies to verify SDK and application behavior; they do not assert what a future live call must answer.
