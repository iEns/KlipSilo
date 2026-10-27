# KlipSilo features

This file lists what KlipSilo can do on the current `main` branch, grouped by area, with how to reach each feature in the app. The manual test pass for these features is [`docs/regression-checklist.md`](docs/regression-checklist.md).

- **Released in:** A version such as `1.2.0` means the feature first shipped in that Play release (tag `v1.2.0`). `Unreleased` means it is only on `main` so far. **At each Play release, every `Unreleased` becomes that release's version.**
- **Test:** the checklist item(s) that cover the feature. `—` means no item yet.
- **Automated:** whether an automated test covers the feature end to end. All `no` for now.

## Clipboard / main list

| Feature | How to reach it | Released in | Test | Automated |
|---|---|---|---|---|
| Save the current clipboard | Button at the top of the Clips tab. With an empty library it appears once the clipboard has content (#93); once there are clips it stays in place, disabled and labelled "Clipboard is empty" when there is nothing to save (#120). A failed save says why and offers Retry when the clipboard still has content | 1.1.0 | [A1](docs/regression-checklist.md#A1) | no |
| New empty text clip | + button (FAB) | 1.1.0 | [A2](docs/regression-checklist.md#A2) | no |
| Clip types: Plain Text, Markdown, link, image, PDF, file, location | Rows in the list. Plain Text and Markdown are the two editable text kinds; HTML is a File clip (preview only, not editable) (#127) | 1.1.0 | [A3](docs/regression-checklist.md#A3), [A4](docs/regression-checklist.md#A4), [A5](docs/regression-checklist.md#A5) | no |
| Open a clip | Tap a row: Plain Text and Markdown open the editor with their content (read/edit), a link opens the browser, a location opens maps, a PDF, image or file opens the viewer; an HTML File clip opens the rendered page read-only (#127) | 1.2.0 | [C1](docs/regression-checklist.md#C1), [C2](docs/regression-checklist.md#C2), [C3](docs/regression-checklist.md#C3), [C4](docs/regression-checklist.md#C4) | no |
| Text kind upgrade | On the first open after the upgrade, old text clips are migrated once, without a user action: Plain Text stays Plain Text, FILE clips ending in `.txt`/`.md` become Plain Text/Markdown, and old HTML text clips become `.html` File clips. No Markdown is inferred from `#`/`*` content (#127) | 1.3.0 | [C7](docs/regression-checklist.md#C7) | no |
| Clip meta shows the text kind, not the storage extension | A large Plain Text or Markdown clip spills to `clip.txt`/`clip.md`; the list still shows **Text**/**Markdown**, never bare **TXT**/**MD** (#127) | 1.3.0 | [B7](docs/regression-checklist.md#B7) | no |
| Action sheet (share, save, present, convert, and more) | Long-press a row, or tap the ⋮ button on the row; primary actions sit on top, conversions and PDF editing tools under **Convert & Edit** (collapsed by default), and Rename/Duplicate/Pin/Lock/Delete under **Manage**; the sheet closes when an action runs. Plain Text/Markdown offer **Share as text**, **Share as file**, **Download as file** and **Make this Markdown**/**Make this Plain Text** (#127) | 1.1.0 | [C3](docs/regression-checklist.md#C3), [C6](docs/regression-checklist.md#C6) | no |
| Search clips | Search field above the list | 1.1.0 | [B1](docs/regression-checklist.md#B1) | no |
| Filter by type (All, Text, Markdown, URL, Image, PDF, File, Location, Locked) | Filter chips; Text and Markdown have separate pills. `<` and `>` cues at the row edges show which way more types can be swiped into view (#127) | 1.1.0 | [B2](docs/regression-checklist.md#B2) | no |
| Pin / unpin | Pin icon on a pinned row, or the action sheet → **Manage** | 1.1.0 | [B3](docs/regression-checklist.md#B3) | no |
| Lock / unlock (blocks edit and delete) | Action sheet → **Manage** | 1.1.0 | [B5](docs/regression-checklist.md#B5) | no |
| Rename | Action sheet | 1.1.0 | [B6](docs/regression-checklist.md#B6) | no |
| Duplicate | Action sheet | 1.1.0 | [B6](docs/regression-checklist.md#B6) | no |
| Delete with undo | Action sheet → confirm, or swipe a row away | 1.1.0 | [B4](docs/regression-checklist.md#B4) | no |
| Find links and phone numbers in text or PDF, then tap to open or call | Action sheet → **Convert & Edit** → Find links | 1.1.0 | [C5](docs/regression-checklist.md#C5) | no |
| Link clip: open the URL | Tap the row | 1.1.0 | [C2](docs/regression-checklist.md#C2) | no |
| Location clip: open in maps | Tap the row | 1.1.0 | [C2](docs/regression-checklist.md#C2) | no |
| File info card for files the viewer can't show | Tap a file clip the viewer can't display (e.g. APK, ZIP): name, type and size with Share, Save to Downloads, Convert to PDF and Try as text; **More** opens the full action sheet (rename, pin, lock, duplicate, delete and conversions) without returning to the list (1.3.0) | 1.2.0 | [C4](docs/regression-checklist.md#C4) | no |

## Receiving / sharing in

| Feature | How to reach it | Released in | Test | Automated |
|---|---|---|---|---|
| Share text from any app | Share → KlipSilo | 1.1.0 | [A3](docs/regression-checklist.md#A3) | no |
| Share a link (saved as a link clip) | Share → KlipSilo | 1.1.0 | [A3](docs/regression-checklist.md#A3) | no |
| Share an image (saved as an image clip) | Share → KlipSilo | 1.1.0 | [A4](docs/regression-checklist.md#A4) | no |
| Share a PDF (saved as a PDF clip) | Share → KlipSilo | 1.1.0 | [A4](docs/regression-checklist.md#A4) | no |
| Share any other file, including binaries (saved raw as a file clip) | File manager → Share → KlipSilo | 1.1.0 | [A5](docs/regression-checklist.md#A5) | no |
| Share a file and text together (both saved) | Share → KlipSilo | 1.1.0 | [A7](docs/regression-checklist.md#A7) | no |
| Duplicate file warning | Share a file that is already a clip: an "Already saved" dialog offers Open existing / Save again / Cancel | 1.2.0 | [A6](docs/regression-checklist.md#A6) | no |
| Download what a link points to, as a PDF/image/text/file clip | Link clip → action sheet → Download from URL | 1.1.0 | [A8](docs/regression-checklist.md#A8) | no |

## Sharing / exporting out

| Feature | How to reach it | Released in | Test | Automated |
|---|---|---|---|---|
| Share a clip to another app | Action sheet → Share (Plain Text/Markdown: **Share as text**) | 1.1.0 | [D1](docs/regression-checklist.md#D1) | no |
| Share a Plain Text/Markdown clip as a `.txt`/`.md` attachment | Action sheet → Share as file (#127) | 1.3.0 | [D5](docs/regression-checklist.md#D5) | no |
| Share text from the editor | Editor top bar → Share | 1.1.0 | [J1](docs/regression-checklist.md#J1) | no |
| Save to Downloads (file/PDF/image) | Action sheet → Save to Downloads | 1.1.0 | [D2](docs/regression-checklist.md#D2) | no |
| Download a Plain Text/Markdown clip as a `.txt`/`.md` file | Action sheet → Download as file (#127) | 1.3.0 | [D5](docs/regression-checklist.md#D5) | no |
| Open an HTML file in the browser | HTML file clip → action sheet → Open in browser | 1.1.0 | [D4](docs/regression-checklist.md#D4) | no |
| Preview an HTML file | Tap an HTML File clip; the page renders read-only in the viewer (#127) | 1.3.0 | [D4](docs/regression-checklist.md#D4) | no |
| Back up all clips as a ZIP (unencrypted) | Tools → Maintenance → Backup | 1.1.0 | [D3](docs/regression-checklist.md#D3) | no |
| Restore from a ZIP | Tools → Maintenance → Restore | 1.1.0 | [D3](docs/regression-checklist.md#D3) | no |
| Restore skips clips that already exist from the same backup | Tools → Maintenance → Restore the same ZIP again | 1.3.0 | [D3](docs/regression-checklist.md#D3) | no |

## Conversions

Each conversion creates a new clip. In the action sheet they are grouped under **Convert & Edit** (tap to expand; collapsed by default).

| Feature | How to reach it | Released in | Test | Automated |
|---|---|---|---|---|
| DOCX → PDF (Title and Heading styles, images, tables, lists) | File clip → action sheet → **Convert & Edit** → Convert to PDF; or the file info card, if the viewer can't show the file. Word's `Title` style becomes a heading, List Bullet / List Number keep their markers even when the numbering lives on the style, and text after a table starts below it (1.3.0) | 1.1.0 | [E3](docs/regression-checklist.md#E3) | no |
| XLSX → PDF | Same as DOCX → PDF | 1.1.0 | [E7](docs/regression-checklist.md#E7) | no |
| RTF → PDF | Same as DOCX → PDF | 1.1.0 | [E7](docs/regression-checklist.md#E7) | no |
| TXT → PDF | Same as DOCX → PDF | 1.1.0 | [E7](docs/regression-checklist.md#E7) | no |
| DOCX/XLSX/RTF/TXT shown as readable text | Tap the file clip | 1.1.0 | [C4](docs/regression-checklist.md#C4) | no |
| File → text for unknown formats | Action sheet → **Convert & Edit** → Try as text; also offered on the file info card | 1.1.0 | [E6](docs/regression-checklist.md#E6) | no |
| Text → PDF | Plain Text/Markdown clip → action sheet → **Convert & Edit** → Text to PDF, or the editor. Markdown clips use the Markdown renderer; Plain Text is always rendered literally, never heuristic-detected as Markdown (#127) | 1.1.0 | [E1](docs/regression-checklist.md#E1) | no |
| Make this Markdown / Make this Plain Text | Editor top bar, or Plain Text/Markdown clip → action sheet → **Convert & Edit**. The same clip and body change kind in place; spilled bodies move between `clip.txt` and `clip.md` (#127) | 1.3.0 | [E8](docs/regression-checklist.md#E8) | no |
| HTML file → PDF | File clip ending in `.html`/`.htm` → action sheet → **Convert & Edit** → HTML to PDF (#127) | 1.3.0 | [E9](docs/regression-checklist.md#E9) | no |
| Image → PDF | Image clip → action sheet → **Convert & Edit** → Image to PDF | 1.1.0 | [E2](docs/regression-checklist.md#E2) | no |
| PDF → images | PDF clip → action sheet → **Convert & Edit** → PDF to image | 1.1.0 | [E4](docs/regression-checklist.md#E4) | no |
| PDF → text (OCR of the whole PDF) | PDF clip → action sheet → **Convert & Edit** → PDF to text | 1.1.0 | [E4](docs/regression-checklist.md#E4) | no |
| PDF output settings: font family, size, line spacing, margins, max output | Settings → PDF & export (directly under Display mode; the row shows the current font and expands on tap); font buttons read System / Sans Serif / Serif / Mono, are centered and are drawn in the family they select; the chosen font is embedded in plain Text → PDF output | 1.1.0 | [E10](docs/regression-checklist.md#E10) | no |

## PDF tools

| Feature | How to reach it | Released in | Test | Automated |
|---|---|---|---|---|
| PDF viewer with page flipping and pinch zoom | Tap a PDF clip; a horizontal swipe flips pages when the page is not zoomed, and pinch-zoom/pan take over once zoomed in | 1.1.0 | [F1](docs/regression-checklist.md#F1) | no |
| Zoomed pan stays in view | PDF viewer and Mark up: pinch in slightly past fit-to-screen, then pan in every direction; some of the page always remains on screen and it can be panned back, while fit-to-screen still centers the page (#148) | Unreleased | [F5](docs/regression-checklist.md#F5) | no |
| Viewer content ends above the action bar | View Content PDF preview: the page viewport ends above the Save page / OCR page / ⋮ bar instead of running underneath it, so a zoomed page can be panned to its bottom edge and the page controls stay usable; pinch-zoom stays smooth with no dual-scale flicker while the fingers are down (#160, #160 reopen) | Unreleased | [F6](docs/regression-checklist.md#F6) | no |
| Page navigation and go-to-page | Viewer action bar: previous/next, or tap the page indicator to jump to a page | 1.2.0 | [F1](docs/regression-checklist.md#F1) | no |
| OCR the current page to text | Viewer action bar | 1.1.0 | [F2](docs/regression-checklist.md#F2) | no |
| Viewer bar labels stay on one line | PDF viewer action bar: labels use a single line (no mid-word wrap). Japanese bar strings are short (`OCR`, `保存`); long labels in any locale shrink, then fall back to icons or the overflow menu (1.3.0) | 1.3.0 | [F2](docs/regression-checklist.md#F2) | no |
| Save one page as its own PDF | Viewer action bar | 1.1.0 | [F2](docs/regression-checklist.md#F2) | no |
| Read a QR code on a page | Viewer action bar → Read QR | 1.1.0 | [F2](docs/regression-checklist.md#F2) | no |
| Extract the images on a page | Viewer action bar → Extract images | 1.1.0 | [F2](docs/regression-checklist.md#F2) | no |
| Combine clips into one PDF (images/PDFs) | Tools → Combine PDFs; needs two or more image/PDF clips, otherwise a hint says so instead of opening a picker | 1.1.0 | [F3](docs/regression-checklist.md#F3) | no |
| Combine text clips | Tools → Combine texts; accepts Plain Text and Markdown clips and needs two or more, otherwise a hint offers Create another. Mixed Plain + Markdown combines to a Markdown clip; all Plain stays Plain Text (#127) | 1.1.0 | [F3](docs/regression-checklist.md#F3) | no |
| Create a blank A4 PDF and mark it up | Tools → Create → Blank PDF; creates a one-page white PDF clip and opens it straight in Mark up (#121) | 1.3.0 | [H6](docs/regression-checklist.md#H6) | no |
| Edit pages (reorder/delete) | PDF clip → action sheet → **Convert & Edit** → Edit pages | 1.1.0 | [F4](docs/regression-checklist.md#F4) | no |
| Fill PDF form (text, checkbox, radio, choice) | PDF clip → action sheet → **Convert & Edit** → Fill form | 1.1.0 | [G1](docs/regression-checklist.md#G1), [G2](docs/regression-checklist.md#G2), [G3](docs/regression-checklist.md#G3) | no |
| Fill form: field labels and a live page preview | Fill form screen | 1.2.0 | [G1](docs/regression-checklist.md#G1) | no |
| Fill form: radio options show their printed labels; the group header is the printed group name (omitted when unknown) | Fill form screen, radio groups | 1.3.0 | [G3](docs/regression-checklist.md#G3) | no |
| Fill form: password masking with a show toggle | Fill form screen, password fields | 1.2.0 | [G4](docs/regression-checklist.md#G4) | no |

## Annotation editor (Mark up PDF)

| Feature | How to reach it | Released in | Test | Automated |
|---|---|---|---|---|
| Text box on a page | PDF clip → action sheet → **Convert & Edit** → Annotate PDF → Text | 1.1.0 | [H1](docs/regression-checklist.md#H1) | no |
| Text box wraps like the saved page while typing, the draft is hidden from the page preview during editing, and Enter adds a new line (#138) | Annotate → Text; type a long paragraph or press Enter | 1.3.0 | [H1](docs/regression-checklist.md#H1) | no |
| Straight line | Annotate → Line | 1.1.0 | [H1](docs/regression-checklist.md#H1) | no |
| Freehand drawing | Annotate → Draw | 1.1.0 | [H1](docs/regression-checklist.md#H1) | no |
| Highlighter | Annotate → Highlight | 1.2.0 | [H1](docs/regression-checklist.md#H1) | no |
| Insert an image (clips, gallery, camera, files) | Annotate → Image, then pick a source; the image is placed centred at about 40% page width | 1.3.0 | [H7](docs/regression-checklist.md#H7) | no |
| Move or resize a placed image | Annotate → Select, drag the image to move, drag a corner to resize with the aspect ratio kept. No rotate or crop | 1.3.0 | [H7](docs/regression-checklist.md#H7) | no |
| Select / move / delete an annotation | Annotate → Select | 1.1.0 | [H1](docs/regression-checklist.md#H1) | no |
| Undo / redo | Toolbar | 1.2.0 | [H1](docs/regression-checklist.md#H1) | no |
| Colours and text/pen size, remembered between sessions | Toolbar | 1.1.0 | [H3](docs/regression-checklist.md#H3) | no |
| Solid colour palette from one colour button | Mark up → tap the colour circle above the toolbar; text, line, draw and highlight each keep their own colour (#144) | 1.3.0 | [H3](docs/regression-checklist.md#H3) | no |
| Page edges clearly visible when zoomed out | Mark up → open a white or blank PDF and zoom out until the page is smaller than the screen; a soft tonal canvas gradient, thin frame and light shadow keep the white page distinct (#146) | Unreleased | [H8](docs/regression-checklist.md#H8) | no |
| Text tool with the keyboard up fits the page to the screen **width** and pans so the text field stays readable above the keyboard; dismissing the keyboard or leaving Text restores the previous zoom/pan (#161). Tapping anywhere, including the top ~1/4, keeps the page framed with the text field on page content (no grey letterbox clipping) (#161 reopen). With the keyboard open after a top tap, panning reaches the true top and the true bottom of the page: the top edge (not the upper third) can meet the top of the canvas, and the bottom edge meets the bottom of the canvas with no large grey band (~500px) below the page (#161) | Mark up → Text → tap the very top of a page that has visible top and bottom content; type with the soft keyboard open, then pan as far as it goes up and down | Unreleased | [H9](docs/regression-checklist.md#H9) | no |
| Multi-page markup | Previous/next page in the toolbar | 1.1.0 | [H1](docs/regression-checklist.md#H1) | no |
| Save the annotated PDF as a new clip | Save | 1.1.0 | [H2](docs/regression-checklist.md#H2) | no |
| Back with unsaved marks asks Save / Discard / Cancel | Annotate → Back (system or top bar) | 1.3.0 | [H4](docs/regression-checklist.md#H4) | no |
| Unsaved marks survive a language or configuration change | Annotate, then change the app language | 1.3.0 | [H5](docs/regression-checklist.md#H5) | no |

## Scan and capture

| Feature | How to reach it | Released in | Test | Automated |
|---|---|---|---|---|
| QR code scanner (camera) | Tools → QR scanner; in landscape the title/hint header hides so the preview uses the full height, and the back arrow stays visible | 1.1.0 | [I1](docs/regression-checklist.md#I1) | no |
| Camera OCR (live text) | Tools → OCR camera; in landscape the title/hint header hides so the preview uses the full height, and the back arrow stays visible | 1.1.0 | [I1](docs/regression-checklist.md#I1) | no |
| OCR an image clip | Image clip → action sheet → **Convert & Edit** → OCR image | 1.1.0 | [I2](docs/regression-checklist.md#I2) | no |
| QR from an image clip | Image viewer → Scan QR | 1.1.0 | [I4](docs/regression-checklist.md#I4) | no |
| Save the current location as a clip (with address) | Tools → Location; current location only, with no typed address; the "Getting location" dialog can be cancelled | 1.1.0 | [I3](docs/regression-checklist.md#I3) | no |

## Text editor and reader

| Feature | How to reach it | Released in | Test | Automated |
|---|---|---|---|---|
| Plain Text / Markdown editor (Save is labelled when the bar has room, otherwise a ✓ with a tooltip; leaving with unsaved edits asks "Save changes?") | Tap a Plain Text or Markdown clip, or use the action sheet → Edit | 1.1.0 | [J1](docs/regression-checklist.md#J1) | no |
| Save-changes prompt when leaving the editor | Editor → back with unsaved changes | 1.2.0 | [J1](docs/regression-checklist.md#J1) | no |
| Markdown insert toolbar | Only on Markdown clips: the **MD** button opens Bold/Italic/Heading/List/Code inserts. Plain Text hides it (#127) | 1.3.0 | [J1](docs/regression-checklist.md#J1) | no |
| Markdown preview | Editor top bar → Preview; Markdown clips render formatted pages, Plain Text renders literally, and the pages open in a full-screen overlay with an X that returns to the editor (#127, #130) | 1.1.0 | [J1](docs/regression-checklist.md#J1) | no |
| Select word / sentence / paragraph | Editor toolbar → Select word / sentence / paragraph → Word / Sentence / Select paragraph | 1.1.0 | [J2](docs/regression-checklist.md#J2) | no |
| Reader mode with dark/bright theme and auto-scroll | Plain Text/Markdown clip → action sheet → Present. Markdown renders its headings, lists, quotes and emphasis; Plain Text stays literal (#127) | 1.1.0 | [C1](docs/regression-checklist.md#C1) | no |

## Settings / languages

| Feature | How to reach it | Released in | Test | Automated |
|---|---|---|---|---|
| Theme: light/dark/system | Settings | 1.1.0 | [K1](docs/regression-checklist.md#K1) | no |
| 13 languages plus System default, synced with Android App Info | Settings → Language | 1.2.0 | [K1](docs/regression-checklist.md#K1) | no |
| About, Privacy policy (all 13 app languages; paid-download wording matches the site; effective 25 Oct 2026). Guide (Tips/Limitations/Why/Use cases) removed in #159. | Settings → About, below PDF & export | 1.3.0 | [K3](docs/regression-checklist.md#K3) | no |
| Rate KlipSilo on Google Play: quiet link (no in-app prompt, dialog or "remind me later") under Privacy; opens the production listing, falling back to the web listing when the Play Store app is missing | Settings → About → Rate KlipSilo on Google Play, below Privacy policy | 1.3.0 | [K5](docs/regression-checklist.md#K5) | no |

## Other / maintenance

| Feature | How to reach it | Released in | Test | Automated |
|---|---|---|---|---|
| Delete all clips, or all clips of one type (locked clips kept) | Tools → Maintenance → Delete; choose All or a type, then Delete | 1.1.0 | [K2](docs/regression-checklist.md#K2) | no |
| Unlock all locked clips (asks for confirmation first) | Tools → Maintenance → Locked clips, then confirm Unlock all | 1.1.0 | [K2](docs/regression-checklist.md#K2) | no |
| Offline only: no account, no tracking | Everything works offline | 1.1.0 | [K4](docs/regression-checklist.md#K4) | no |

## Not supported

| Not supported | Notes |
|---|---|
| Converting into DOCX (or any Office format) | Conversions only produce PDF, text or images. DOCX/XLSX/RTF are read and converted to PDF; PDF → HTML has been retired (#127) |
| Sharing several files into KlipSilo at once | The app only receives `ACTION_SEND` (one item). `ACTION_SEND_MULTIPLE` is not handled |
| "Open with KlipSilo" from a file manager | No `ACTION_VIEW` intent filter. Use Share → KlipSilo instead |
| Copying a clip's content from the action sheet | There is no Copy action for a clip. Copy from the text editor's text field, or use Share |
| Encrypted backups | Backup is an unencrypted ZIP |
| Typing an address as a location | Location saves the current GPS position only |
| Sync or accounts | KlipSilo is offline only, with no account or cloud sync |
