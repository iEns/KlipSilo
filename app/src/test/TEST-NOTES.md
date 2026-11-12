# JVM/Robolectric coverage notes

These notes record paths that are intentionally **not** exercised end to end in
`app/src/test`; no emulator/instrumentation test was added for them.

- **HTML File clip → PDF execution.** The action surface is covered
  (`PdfHtmlConversionActionsTest`): only `.html`/`.htm` File clips offer HTML → PDF,
  and PDF clips have no PDF → HTML row (#127). The actual conversion,
  `ClipboardRepository.saveHtmlAsPdf`, draws through
  `android.graphics.pdf.PdfDocument`; under Robolectric 4.14.1 a fresh
  `PdfDocument.startPage(...)` throws `IllegalStateException: document is closed!`.
  This is a Robolectric limitation, not a WebView dependency, so the round-trip is
  left for a device/instrumentation pass rather than asserted here.

- **PDF page rendering.** `PdfRenderer` is native. `ClipOpenLoadPathTest` asserts
  the render load path never crashes and checks bitmap dimensions when the
  Robolectric renderer reports pages; the visual result stays a device check
  (the existing `PdfOperationsTest` follows the same policy).

- **Fill-form Compose screen.** `PdfFormRoundTripTest` covers the repository
  read → write → reread behaviour (`getPdfFormFields` / `fillFormCopy`) without
  driving the Compose screen. The screen-level preview is documented in
  `FormPreviewRenderTest`.
