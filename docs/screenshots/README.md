# Actual emulator captures

The PNGs in this directory show NEXVARY Traffic Lab running on an Android 15/API 35 emulator in Arabic. They were captured by `GuidedWorkflowTest`, after the surface rendered, from source commit `4d91c5d5b59f7ac489ee4c667282c6cf862341c1`.

Capture run: https://github.com/nexvary/NEXVARY-Traffic/actions/runs/38873048715

- `dashboard-api35.png`: main dashboard with bundled multicolor icons and the guided entry point.
- `workflow-api35.png`: step 2, HTTPS engine setup. The local loopback destination is an ephemeral TLS test fixture.

These files document emulator execution. Physical-device validation is still outstanding. Later authentication tags/test refinements and HTTP/2 result formatting do not change either captured screen.
