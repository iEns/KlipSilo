# KlipSilo manual regression checklist (core features)

- **Build under test:** v1.2.0-beta.x (current beta.6 = `3e2513e`). **Baseline:** v1.1.0 (`2a5e8eb`).
- **Source:** `../FEATURES.md` with Jens's corrections from 2026-10-18:
  - Conversion *to* DOCX never existed and is dropped. DOCX → PDF is a v1.1.0 feature and stays in scope (Silas, 2026-10-18).
  - The duplicate-share dialog (R2) and undo/redo replacing "Clear all" (R3) are intended.
  - Share intake works.
  - Tap opens or views a clip. That is intended.
- **Scope:** core functions only. There are no text-overflow, layout or polish checks.
- **Item IDs:** each item has a stable ID (section letter + number, e.g. C3) that `../FEATURES.md` links to. Keep IDs stable; add new items at the end of a section.
- **Every item passes only if:** nothing crashes, and you return to the list without losing data.
- **Sample files to keep at hand:**
  - a JPG
  - a text PDF
  - testaroo `job-application.pdf` and `event-registration.pdf`
  - a DOCX, a TXT, an APK or ZIP
  - a web link
  - a short text

## A. Capture and share intake
- <a id="A1"></a>**A1 · Save clipboard:** copy a text in any app, open KlipSilo, then tap **Save current clipboard**. → A new text clip appears at the top with that text.
- <a id="A2"></a>**A2 · New text clip:** tap **+**, type a text, then go back. → The clip is saved. Leaving it empty or discarding it saves nothing.
- <a id="A3"></a>**A3 · Share text and links in:** share a sentence from Notes, then share a URL from Chrome, each to KlipSilo. → You get one Text clip and one URL clip with the right content.
- <a id="A4"></a>**A4 · Share an image and a PDF in:** share the JPG and the PDF from Files. → You get an Image clip (type and size shown) and a PDF clip.
- <a id="A5"></a>**A5 · Share any other file in:** share a DOCX, TXT and APK/ZIP from Files. → Each one appears as a File clip with the right name, type and size.
- <a id="A6"></a>**A6 · Duplicate share:** share the same JPG again. → The "Already saved" dialog appears. Cancel saves nothing, Save again adds a copy, and Open existing opens the original.

## B. Clip list
- <a id="B1"></a>**B1 · Search:** type part of a clip title or text. → Only matching clips are shown. Clearing the search restores the full list.
- <a id="B2"></a>**B2 · Filter chips:** tap each chip in turn (Text, URL, Image, PDF, File, Location, and Locked if present). → Only clips of that type are shown. **All** restores the list.
- <a id="B3"></a>**B3 · Pin:** pin a clip from the row icon or the sheet. → It moves to the top and stays there after the app restarts. Unpinning reverses this.
- <a id="B4"></a>**B4 · Delete and undo:** delete one clip from the sheet (confirm), and swipe another one away. Tap **Undo** on one of them. → The undone clip comes back. The other one stays gone after the app restarts.
- <a id="B5"></a>**B5 · Lock:** lock a clip. → Delete, swipe and edit are blocked for it. Unlocking restores them.
- <a id="B6"></a>**B6 · Rename and Duplicate:** rename a clip, then duplicate it. → The new title shows. The duplicate is a separate clip with the same content.

## C. Opening and viewing clips (tap, or sheet → View content)
- <a id="C1"></a>**C1 · Text clip:** tap it. → The reader opens with the full text. Dark/bright theme and auto-scroll work.
- <a id="C2"></a>**C2 · URL and location clips:** tap each. → The URL opens in the browser. The location opens in Maps.
- <a id="C3"></a>**C3 · Image clip:** tap it, and also try long-press → **View content**. → The image shows, with no crash (#66).
- <a id="C4"></a>**C4 · Non-text file clips:** tap the DOCX, the TXT and the APK/ZIP, and try **View content** on each. → DOCX and TXT show readable text. The APK/ZIP shows a file info card or message with its actions. None of them crash (#66).
- <a id="C5"></a>**C5 · Find links:** on a text or PDF clip that contains a URL and a phone number, open the sheet → **Find links**. → Both are listed. Tapping one opens the browser or the dialer.

## D. Sharing out and export
- <a id="D1"></a>**D1 · Share out:** sheet → **Share** on a text, an image and a file clip. → The system share sheet carries the right content or file. For example, it pastes correctly into Gmail.
- <a id="D2"></a>**D2 · Save to Downloads:** run it on an image, a PDF and a file clip. → Each file appears in Downloads with its correct name and opens.
- <a id="D3"></a>**D3 · Backup and restore:** Tools → Maintenance → **Backup** to ZIP. Delete a test clip, then **Restore** from the ZIP. → The deleted clip comes back, and the other clips aren't duplicated or lost.

## E. Conversions (each one creates a new clip)
- <a id="E1"></a>**E1 · Text → PDF:** use a Markdown text clip (with a heading and a list), then sheet → PDF. → A new PDF clip shows the formatted text.
- <a id="E2"></a>**E2 · Image → PDF:** run it on the JPG. → A new one-page PDF clip holds the image.
- <a id="E3"></a>**E3 · DOCX → PDF:** on the DOCX file clip, use the sheet (long-press or ⋮) → **Convert to PDF**. → A new PDF clip appears with the document's headings, text, lists, tables and images, and it opens in the PDF viewer.
- <a id="E4"></a>**E4 · PDF → Image and PDF → Text:** run both on the text PDF. → You get image clip(s) per page, and a text clip with the PDF's text.
- <a id="E5"></a>**E5 · PDF → HTML, then edit:** run PDF → HTML, open the result, and tap **Edit**. → The HTML clip is created. Edit shows the HTML **source**, not the preview (#69).
- <a id="E6"></a>**E6 · Try as text:** use the sheet's **Try as text** on an unknown file. → You get a text result or a clear message, with no crash.

## F. PDF viewer and PDF tools
- <a id="F1"></a>**F1 · PDF viewer:** tap a multi-page PDF, swipe pages, pinch-zoom, and use go-to-page. → The pages render, and navigation and zoom work.
- <a id="F2"></a>**F2 · Viewer actions:** in the viewer, run OCR page, Save page as PDF, Read QR (on a page with a QR code) and Extract images. → Each one gives a correct new clip or result.
- <a id="F3"></a>**F3 · Combine:** Tools → **Combine** two PDFs and an image, and **Combine texts** with two text clips. → You get one merged PDF clip in the chosen order, and one merged text clip.
- <a id="F4"></a>**F4 · Edit pages:** PDF sheet → **Edit pages**, reorder two pages, delete one, and save. → The new PDF has the expected page order and count.

## G. Fill PDF form (testaroo job-application and event-registration)
- <a id="G1"></a>**G1 · Single-line fields:** fill Full name and Email, then **Save PDF**. → The values show in the preview. The saved PDF clip shows them in Files or another PDF viewer.
- <a id="G2"></a>**G2 · Multi-line field:** type several lines in **Cover letter** (job form) and **Notes** (event form). → There's no crash on the first character, the text wraps in the preview, and it's saved (#68).
- <a id="G3"></a>**G3 · Checkboxes and radio groups:** tick checkboxes. In each radio group (Position, Ticket), pick an option and then switch to another. → Each radio option shows separately, and only one per group is selected. This is not shown as a single checkbox (#67). The choice is kept in the saved PDF.

## H. Mark up PDF
- <a id="H1"></a>**H1 · Annotate:** on page 1, add a text box (two lines), a line, a freehand stroke and a highlight. Move one item, then delete it via Select. Try Undo/Redo. Go to page 2 and add a stroke. → All the tools work, and undo/redo steps back and forth correctly.
- <a id="H2"></a>**H2 · Save:** tap **Save**. → A new PDF clip has all the annotations on the right pages, line breaks included. Saving with no changes creates no duplicate.

## I. Scan and capture
- <a id="I1"></a>**I1 · QR scanner and camera OCR:** Tools → QR on a QR code, and Tools → OCR camera on printed text. → The decoded text or link is offered or saved as a clip, and the OCR text is saved as a clip.
- <a id="I2"></a>**I2 · OCR from an image clip:** sheet → **Crop/OCR from image** on a photo of text. → A text clip holds the recognised text.
- <a id="I3"></a>**I3 · Location:** Tools → Location (current location; there is no typed-address entry). → A location clip is created with the address. The snackbar's **Open** opens the clip in KlipSilo, and tapping the clip opens Maps.

## J. Text editor
- <a id="J1"></a>**J1 · Edit and save prompt:** open a text clip in the editor, change the text, and leave. → A "Save changes?" prompt appears (intended since beta.4, #27). Save, then reopen. → The change was kept. Share from the editor works. The Markdown preview renders.

## K. Settings and maintenance
- <a id="K1"></a>**K1 · Theme and language:** switch Light/Dark/System. Switch the language to Danish, then German, then back to English or System default. → Each takes effect at once and survives an app restart. The App Info language stays in sync.
- <a id="K2"></a>**K2 · Bulk delete:** Tools → Maintenance → delete all clips of one type, then **Unlock all**. → Only that type is removed, and locked clips are kept. Unlock all clears every lock.
