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
  - a DOCX, an XLSX, an RTF, a TXT, an HTML file, an APK or ZIP
  - a PDF form with a password field
  - a link to a PDF or image file, and a link to an ordinary web page
  - a web link
  - a short text

## A. Capture and share intake
- <a id="A1"></a>**A1 · Save clipboard:** with an empty clipboard, open KlipSilo. → The **Save current clipboard** button is hidden and the empty state leads the list. Now copy a text in any app, return to KlipSilo, and tap **Save current clipboard**. → The button reappears and a new text clip appears at the top with that text. A failed save names the reason (empty, nothing saveable, too large) and offers **Retry** when the clipboard still has content; the clipboard is kept.
- <a id="A2"></a>**A2 · New text clip:** tap **+**, type a text, then go back. → The clip is saved. Leaving it empty or discarding it saves nothing.
- <a id="A3"></a>**A3 · Share text and links in:** share a sentence from Notes, then share a URL from Chrome, each to KlipSilo. → You get one Text clip and one URL clip with the right content.
- <a id="A4"></a>**A4 · Share an image and a PDF in:** share the JPG and the PDF from Files. → You get an Image clip (type and size shown) and a PDF clip.
- <a id="A5"></a>**A5 · Share any other file in:** share a DOCX, TXT and APK/ZIP from Files. → Each one appears as a File clip with the right name, type and size.
- <a id="A6"></a>**A6 · Duplicate share:** share the same JPG again. → The "Already saved" dialog appears. Cancel saves nothing, Save again adds a copy, and Open existing opens the original.
- <a id="A7"></a>**A7 · Share a file and text together:** from an app that shares a file with a caption (e.g. Photos or Files with text added), share both to KlipSilo in one go. Use a file that is not already a clip, so the A6 dialog does not appear. → Two clips appear: the file (Image, PDF or File clip) and a Text clip with the text. Neither is lost.
- <a id="A8"></a>**A8 · Download from URL:** on a URL clip that points to a PDF or image, sheet → **Download from URL**. → "Downloading…" then "File downloaded", and a new PDF or Image clip (or Text/File clip for other files) appears at the top. On a link to an ordinary web page you get "Could not download file" and no clip.

## B. Clip list
- <a id="B1"></a>**B1 · Search:** type part of a clip title or text. → Only matching clips are shown. When nothing matches, including on an empty library, the empty state names the query and offers **Clear** instead of saying **No clips saved yet**; clearing the search restores the full list.
- <a id="B2"></a>**B2 · Filter chips:** tap each chip in turn (Text, URL, Image, PDF, File, Location, and Locked if present). → Only clips of that type are shown, and an empty result names that chip with a matching hint (even when the library itself is empty). **All** restores the list. When the row runs past the screen edge, the clipped chip fades out and the row can be swiped sideways to the remaining chips.
- <a id="B3"></a>**B3 · Pin:** pin a clip from the row icon or the sheet. → It moves to the top and stays there after the app restarts. Unpinning reverses this.
- <a id="B4"></a>**B4 · Delete and undo:** delete one clip from the sheet (confirm) — sheet → **Manage** → **Delete** — and swipe another one away. Tap **Undo** on one of them. → The undone clip comes back. The other one stays gone after the app restarts. The undo snackbar sits above the **+** button.
- <a id="B5"></a>**B5 · Lock:** lock a clip from the sheet. → Delete, swipe and edit are blocked for it. Unlocking restores them.
- <a id="B6"></a>**B6 · Rename and Duplicate:** sheet → **Manage** → **Rename**, then **Manage** → **Duplicate**. → The new title shows. The duplicate is a separate clip with the same content.

## C. Opening and viewing clips (tap, or the action sheet)
- <a id="C1"></a>**C1 · Text clip:** tap it. → The editor opens with the full text, ready to read or edit. In the sheet, **Present** still opens the reader, where dark/bright theme and auto-scroll work.
- <a id="C2"></a>**C2 · URL and location clips:** tap each. → The URL opens in the browser. The location opens in Maps.
- <a id="C3"></a>**C3 · Image clip:** tap it, and also try long-press → **View content**. → The image shows, with no crash (#66).
- <a id="C4"></a>**C4 · Non-text file clips:** tap the DOCX, the TXT and the APK/ZIP, and try **View content** on each. → DOCX and TXT show readable text. The APK/ZIP shows a file info card or message with its actions. Tap **More** on the file info card and use an action from the full sheet (e.g. Rename, Pin or Delete) without going back to the list (#71). None of them crash (#66).
- <a id="C5"></a>**C5 · Find links:** on a text or PDF clip that contains a URL and a phone number, open the sheet → **Find links**. → Both are listed. Tapping one opens the browser or the dialer.
- <a id="C6"></a>**C6 · Action sheet layout:** long-press a text clip (or tap ⋮). → **Edit**, **Present** and **Share** sit at the top; **Convert** is collapsed by default and expands to the conversion actions; **Manage** groups **Rename**, **Duplicate** and **Delete**. PDF tools (Annotate, Fill form, Edit pages), Find links, Pin and Lock sit between Convert and Manage. No action is missing.

## D. Sharing out and export
- <a id="D1"></a>**D1 · Share out:** sheet → **Share** on a text, an image and a file clip. → The system share sheet carries the right content or file. For example, it pastes correctly into Gmail.
- <a id="D2"></a>**D2 · Save to Downloads:** run it on an image, a PDF and a file clip. → Each file appears in Downloads with its correct name and opens.
- <a id="D3"></a>**D3 · Backup and restore:** Tools → Maintenance → **Backup** to ZIP. Delete a test clip, then **Restore** from the ZIP. → The deleted clip comes back with its original id, and the other clips aren't duplicated or lost. Restore the same ZIP again → every clip that already exists is skipped (same backup id), so nothing is duplicated.
- <a id="D4"></a>**D4 · Open in browser:** on an HTML file clip, sheet → **Open in browser**. → The page opens in the browser and renders. The clip is unchanged.

## E. Conversions (each one creates a new clip)
All conversions below are under the sheet's **Convert** row — tap it to expand (collapsed by default).
- <a id="E1"></a>**E1 · Text → PDF:** use a Markdown text clip (with a heading and a list), then sheet → **Convert** → PDF. → A new PDF clip shows the formatted text.
- <a id="E2"></a>**E2 · Image → PDF:** run it on the JPG. → A new one-page PDF clip holds the image.
- <a id="E3"></a>**E3 · DOCX → PDF:** on the DOCX file clip, use the sheet (long-press or ⋮) → **Convert** → **Convert to PDF**. → A new PDF clip appears with the document's headings, text, lists, tables and images, and it opens in the PDF viewer. A Word `Title` style renders as a heading, List Bullet / List Number paragraphs keep their `•` / `1.` markers (including styles whose numbering lives on the style), and paragraphs after a table start below the table instead of overlapping its last row (#75).
- <a id="E4"></a>**E4 · PDF → Image and PDF → Text:** run both on the text PDF. → You get image clip(s) per page, and a text clip with the PDF's text.
- <a id="E5"></a>**E5 · PDF → HTML, then edit:** run PDF → HTML, open the result, and tap **Edit**. → The HTML clip is created. Edit shows the HTML **source**, not the preview (#69).
- <a id="E6"></a>**E6 · Try as text:** use the sheet's **Convert** → **Try as text** on an unknown file. → You get a text result or a clear message, with no crash.
- <a id="E7"></a>**E7 · XLSX, RTF and TXT → PDF:** on each file clip, sheet → **Convert** → **Convert to PDF**. → Each gives a new PDF clip with the content (XLSX: sheets as tables; RTF: formatted text; TXT: plain text) that opens in the PDF viewer.
- <a id="E8"></a>**E8 · Text → TXT:** on a text clip, sheet → **Convert** → **Text → TXT**. → A new File clip (.txt) appears with the same text. Tapping it shows the text.
- <a id="E9"></a>**E9 · HTML → PDF:** on a text clip that contains HTML (e.g. the result of E5), sheet → **Convert** → **HTML → PDF**. → A new PDF clip shows the rendered page (headings, paragraphs, lists), not the raw tags. Plain text clips don't offer this row.
- <a id="E10"></a>**E10 · PDF output settings:** Settings → **PDF & export** (collapsed by default; tap the row to expand): change Font type, Font size, Line spacing, the two margins and Max output resolution, then repeat E1. → The new PDF uses the new font, size, spacing and margins. The settings survive an app restart.

## F. PDF viewer and PDF tools
- <a id="F1"></a>**F1 · PDF viewer:** tap a multi-page PDF, swipe horizontally through pages, pinch-zoom, pan while zoomed, and use go-to-page. → The pages render. While the page is not zoomed, a horizontal swipe flips to the next or previous page; once zoomed, pinch and one-finger pan move the page and do not flip it. The page buttons and go-to-page still work.
- <a id="F2"></a>**F2 · Viewer actions:** in the viewer, run OCR page, Save page as PDF, Read QR (on a page with a QR code) and Extract images. → Each one gives a correct new clip or result. The bar's labels, including Japanese **OCRページ** / **ページを保存**, stay on one line: they shrink to fit, then become icon-only or move into the overflow menu rather than wrapping mid-word (#86).
- <a id="F3"></a>**F3 · Combine:** Tools → **Combine** two PDFs and an image, and **Combine texts** with two text clips. → You get one merged PDF clip in the chosen order, and one merged text clip. With fewer than two text clips, **Combine texts** shows a hint and **Create another** instead of opening an empty picker.
- <a id="F4"></a>**F4 · Edit pages:** PDF sheet → **Edit pages**, reorder two pages, delete one, and save. → The new PDF has the expected page order and count.

## G. Fill PDF form (testaroo job-application and event-registration)
- <a id="G1"></a>**G1 · Single-line fields:** fill Full name and Email, then **Save PDF**. → The values show in the preview. The saved PDF clip shows them in Files or another PDF viewer.
- <a id="G2"></a>**G2 · Multi-line field:** type several lines in **Cover letter** (job form) and **Notes** (event form). → There's no crash on the first character, the text wraps in the preview, and it's saved (#68).
- <a id="G3"></a>**G3 · Checkboxes and radio groups:** tick checkboxes. In each radio group (**Work authorization** on the job form, **T-shirt size** on the event form), check the heading and pick an option, then switch to another. → Each radio option shows the text printed on the form ("Citizen / Permanent resident", "Visa holder", "Needs sponsorship"; "S", "M", "L", "XL"), not the internal export value, and only one per group is selected. This is not shown as a single checkbox (#67). The group heading is the printed group name, never the first option; a group without a name has no heading (#72, #78). The choice is kept in the saved PDF.
- <a id="G4"></a>**G4 · Password field:** in a form with a password field, type a value. Use the eye button (**Show password** / **Hide password**). → The field shows dots by default and the value on Show. The note "This will be saved in the PDF as readable text." is shown. The preview shows dots (with a "Saved as readable text" pill when the field is large enough). The saved PDF holds the real value.

## H. Mark up PDF
- <a id="H1"></a>**H1 · Annotate:** on page 1, add a text box (two lines), a line, a freehand stroke and a highlight. Move one item, then delete it via Select. Try Undo/Redo. Go to page 2 and add a stroke. → All the tools work, and undo/redo steps back and forth correctly.
- <a id="H2"></a>**H2 · Save:** tap **Save**. → A new PDF clip has all the annotations on the right pages, line breaks included. Saving with no changes creates no duplicate.
- <a id="H3"></a>**H3 · Colours and sizes remembered:** in Mark up, pick a non-default colour (Black, White, Red, Yellow) and a different text and stroke size, then leave. Open Mark up again, also after an app restart. → The last colours and sizes are preselected.
- <a id="H4"></a>**H4 · Leave with unsaved marks:** add a text box and a stroke, then press system Back and, separately, the top-bar Back. → Each time a Save / Discard / Cancel dialog appears. Cancel keeps editing, Discard drops the marks, Save creates the annotated PDF clip.
- <a id="H5"></a>**H5 · Unsaved marks survive a language change:** add a text box and a stroke, then change the app language (or recreate the activity, e.g. rotate) with Mark up still open and return to it. → The unsaved marks are still there, and Back still asks Save / Discard / Cancel.

## I. Scan and capture
- <a id="I1"></a>**I1 · QR scanner and camera OCR:** Tools → QR on a QR code, and Tools → OCR camera on printed text. → The decoded text or link is offered or saved as a clip, and the OCR text is saved as a clip.
- <a id="I2"></a>**I2 · OCR from an image clip:** sheet → **Convert** → **Crop/OCR from image** on a photo of text. → A text clip holds the recognised text. The crop frame includes everything it visually encloses: a full line at the right or bottom edge keeps its trailing characters in the OCR result (#82).
- <a id="I3"></a>**I3 · Location:** Tools → Location (current location; there is no typed-address entry). → A "Getting location" dialog appears; **Cancel** closes it without saving, and the back gesture or a tap outside does the same. Let it finish: a location clip is created with the address. The snackbar's **Open** opens the clip in KlipSilo, and tapping the clip opens Maps.
- <a id="I4"></a>**I4 · QR from an image clip:** open an image clip that contains a QR code and tap the **Scan QR** button. → The decoded message is shown with **Copy**, **Save to clip** and **Cancel**. Save to clip creates a text or URL clip. An image without a QR shows "No QR code found".

## J. Text editor
- <a id="J1"></a>**J1 · Edit and save prompt:** tap a text clip to open the editor, change the text, and leave. → A "Save changes?" prompt appears (intended since beta.4, #27). Save, then reopen. → The change was kept. The top bar shows **Save** as a label when there is room (or the ✓ with a "Save" tooltip when collapsed); tapping it saves without the prompt. Share from the editor works. The Markdown preview renders.
- <a id="J2"></a>**J2 · Select word / sentence / paragraph:** in the editor, place the cursor in a word and use the **Select word / sentence / paragraph** button → **Word**, then **Sentence**, then **Select paragraph**. → Each selects exactly that unit around the cursor.
- <a id="J3"></a>**J3 · HTML preview toggle:** open an HTML text clip (e.g. from E5) in the editor and use the top-bar toggle (**Preview** / **Source**). → Preview shows the rendered page and Source shows the editable HTML. Switching back and forth keeps edits.

## K. Settings and maintenance
- <a id="K1"></a>**K1 · Theme and language:** switch Light/Dark/System. Switch the language to Danish, then German, then back to English or System default. → Each takes effect at once and survives an app restart. The App Info language stays in sync.
- <a id="K2"></a>**K2 · Bulk delete and unlock:** Tools → Maintenance → **Delete**, choose All or one type, then **Delete**. Then tap **Locked clips**, confirm **Unlock all**. → Only that type is removed, and locked clips are kept. Unlock all asks for confirmation first: **Cancel** (or tapping outside) keeps every lock, and **Unlock** clears every lock.
- <a id="K3"></a>**K3 · Guide and info screens:** in Settings, open **Tips**, **Limitations**, **Why**, **Where the app helps**, **About** and **Privacy policy** in turn; they sit above the collapsed **PDF & export** row. → Each opens with its content, scrolls, and back returns to Settings. In **Where the app helps**, the "Contact details and places" tip says the current location only (no typed address) and that a tap or double-tap opens Maps.
- <a id="K4"></a>**K4 · Offline only:** turn on airplane mode and restart the app. Run A1, A2, C1, E1, E3, F1, F2, G1, H2 and I2. → Everything works with no network and no sign-in prompt. Only Download from URL and opening a link in the browser need the network, and they fail with a message, not a crash.
