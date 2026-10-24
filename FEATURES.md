# KlipSilo features

This file lists what KlipSilo can do on the current `main` branch, grouped by area, with how to reach each feature in the app. The manual test pass for these features is [`docs/regression-checklist.md`](docs/regression-checklist.md).

- **Released in:** A version such as `1.2.0` means the feature first shipped in that Play release (tag `v1.2.0`). `Unreleased` means it is only on `main` so far. **At each Play release, every `Unreleased` becomes that release's version.**
- **Test:** the checklist item(s) that cover the feature. `—` means no item yet.
- **Automated:** whether an automated test covers the feature end to end. All `no` for now.

## Clipboard / main list

| Feature | How to reach it | Released in | Test | Automated |
|---|---|---|---|---|
| Save the current clipboard | Button at the top of the Clips tab, shown while the clipboard has content; a failed save says why and offers Retry when the clipboard still has content | 1.1.0 | [A1](docs/regression-checklist.md#A1) | no |
| New empty text clip | + button (FAB) | 1.1.0 | [A2](docs/regression-checklist.md#A2) | no |
| Clip types: text, link, image, PDF, file, location | Rows in the list | 1.1.0 | [A3](docs/regression-checklist.md#A3), [A4](docs/regression-checklist.md#A4), [A5](docs/regression-checklist.md#A5) | no |
| Open a clip | Tap a row: text opens the editor with its content (read/edit), a link opens the browser, a location opens maps, a PDF, image or file opens the viewer | 1.2.0 | [C1](docs/regression-checklist.md#C1), [C2](docs/regression-checklist.md#C2), [C3](docs/regression-checklist.md#C3), [C4](docs/regression-checklist.md#C4) | no |
| Action sheet (share, save, present, convert, and more) | Long-press a row, or tap the ⋮ button on the row; primary actions sit on top, conversions under **Convert** (collapsed by default), and Rename/Duplicate/Delete under **Manage** | 1.1.0 | [C3](docs/regression-checklist.md#C3), [C6](docs/regression-checklist.md#C6) | no |
| Search clips | Search field above the list | 1.1.0 | [B1](docs/regression-checklist.md#B1) | no |
| Filter by type (All, Text, URL, Image, PDF, File, Location, Locked) | Filter chips; the row fades at the edge when more types are off screen | 1.1.0 | [B2](docs/regression-checklist.md#B2) | no |
| Pin / unpin | Pin icon on a pinned row, or the action sheet | 1.1.0 | [B3](docs/regression-checklist.md#B3) | no |
| Lock / unlock (blocks edit and delete) | Action sheet | 1.1.0 | [B5](docs/regression-checklist.md#B5) | no |
| Rename | Action sheet | 1.1.0 | [B6](docs/regression-checklist.md#B6) | no |
| Duplicate | Action sheet | 1.1.0 | [B6](docs/regression-checklist.md#B6) | no |
| Delete with undo | Action sheet → confirm, or swipe a row away | 1.1.0 | [B4](docs/regression-checklist.md#B4) | no |
| Find links and phone numbers in text or PDF, then tap to open or call | Action sheet → Find links | 1.1.0 | [C5](docs/regression-checklist.md#C5) | no |
| Link clip: open the URL | Tap the row | 1.1.0 | [C2](docs/regression-checklist.md#C2) | no |
| Location clip: open in maps | Tap the row | 1.1.0 | [C2](docs/regression-checklist.md#C2) | no |
| File info card for files the viewer can't show | Tap a file clip the viewer can't display (e.g. APK, ZIP): name, type and size with Share, Save to Downloads, Convert to PDF and Try as text | 1.2.0 | [C4](docs/regression-checklist.md#C4) | no |

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
| Share a clip to another app | Action sheet → Share | 1.1.0 | [D1](docs/regression-checklist.md#D1) | no |
| Share text from the editor | Editor top bar → Share | 1.1.0 | [J1](docs/regression-checklist.md#J1) | no |
| Save to Downloads (file/PDF/image) | Action sheet → Save to Downloads | 1.1.0 | [D2](docs/regression-checklist.md#D2) | no |
| Open an HTML file in the browser | HTML file clip → action sheet → Open in browser | 1.1.0 | [D4](docs/regression-checklist.md#D4) | no |
| Back up all clips as a ZIP (unencrypted) | Tools → Maintenance → Backup | 1.1.0 | [D3](docs/regression-checklist.md#D3) | no |
| Restore from a ZIP | Tools → Maintenance → Restore | 1.1.0 | [D3](docs/regression-checklist.md#D3) | no |
| Restore skips clips that already exist from the same backup | Tools → Maintenance → Restore the same ZIP again | 1.3.0 | [D3](docs/regression-checklist.md#D3) | no |

## Conversions

Each conversion creates a new clip. In the action sheet they are grouped under **Convert** (tap to expand; collapsed by default).

| Feature | How to reach it | Released in | Test | Automated |
|---|---|---|---|---|
| DOCX → PDF (headings, images, tables, lists) | File clip → action sheet → Convert → Convert to PDF; or the file info card, if the viewer can't show the file | 1.1.0 | [E3](docs/regression-checklist.md#E3) | no |
| XLSX → PDF | Same as DOCX → PDF | 1.1.0 | [E7](docs/regression-checklist.md#E7) | no |
| RTF → PDF | Same as DOCX → PDF | 1.1.0 | [E7](docs/regression-checklist.md#E7) | no |
| TXT → PDF | Same as DOCX → PDF | 1.1.0 | [E7](docs/regression-checklist.md#E7) | no |
| DOCX/XLSX/RTF/TXT shown as readable text | Tap the file clip | 1.1.0 | [C4](docs/regression-checklist.md#C4) | no |
| File → text for unknown formats | Action sheet → Convert → Try as text; also offered on the file info card | 1.1.0 | [E6](docs/regression-checklist.md#E6) | no |
| Text → PDF (Markdown-aware) | Text clip → action sheet → Convert → Text to PDF, or the editor | 1.1.0 | [E1](docs/regression-checklist.md#E1) | no |
| Text → .txt file | Text clip → action sheet → Convert → Text to .txt | 1.1.0 | [E8](docs/regression-checklist.md#E8) | no |
| HTML text → PDF | Text clip that looks like HTML → action sheet → Convert → HTML to PDF | 1.1.0 | [E9](docs/regression-checklist.md#E9) | no |
| Image → PDF | Image clip → action sheet → Convert → Image to PDF | 1.1.0 | [E2](docs/regression-checklist.md#E2) | no |
| PDF → images | PDF clip → action sheet → Convert → PDF to image | 1.1.0 | [E4](docs/regression-checklist.md#E4) | no |
| PDF → text (OCR of the whole PDF) | PDF clip → action sheet → Convert → PDF to text | 1.1.0 | [E4](docs/regression-checklist.md#E4) | no |
| PDF → HTML | PDF clip → action sheet → Convert → PDF to HTML | 1.1.0 | [E5](docs/regression-checklist.md#E5) | no |
| PDF output settings: font family, size, line spacing, margins, max output | Settings → PDF & export (tap the row to expand) | 1.1.0 | [E10](docs/regression-checklist.md#E10) | no |

## PDF tools

| Feature | How to reach it | Released in | Test | Automated |
|---|---|---|---|---|
| PDF viewer with page flipping and pinch zoom | Tap a PDF clip | 1.1.0 | [F1](docs/regression-checklist.md#F1) | no |
| Page navigation and go-to-page | Viewer action bar: previous/next, or tap the page indicator to jump to a page | 1.2.0 | [F1](docs/regression-checklist.md#F1) | no |
| OCR the current page to text | Viewer action bar | 1.1.0 | [F2](docs/regression-checklist.md#F2) | no |
| Save one page as its own PDF | Viewer action bar | 1.1.0 | [F2](docs/regression-checklist.md#F2) | no |
| Read a QR code on a page | Viewer action bar → Read QR | 1.1.0 | [F2](docs/regression-checklist.md#F2) | no |
| Extract the images on a page | Viewer action bar → Extract images | 1.1.0 | [F2](docs/regression-checklist.md#F2) | no |
| Combine clips into one PDF (images/PDFs) | Tools → Combine PDFs | 1.1.0 | [F3](docs/regression-checklist.md#F3) | no |
| Combine text clips | Tools → Combine texts; needs two or more text clips, otherwise a hint offers Create another | 1.1.0 | [F3](docs/regression-checklist.md#F3) | no |
| Edit pages (reorder/delete) | PDF clip → action sheet → Edit pages | 1.1.0 | [F4](docs/regression-checklist.md#F4) | no |
| Fill PDF form (text, checkbox, radio, choice) | PDF clip → action sheet → Fill form | 1.1.0 | [G1](docs/regression-checklist.md#G1), [G2](docs/regression-checklist.md#G2), [G3](docs/regression-checklist.md#G3) | no |
| Fill form: field labels and a live page preview | Fill form screen | 1.2.0 | [G1](docs/regression-checklist.md#G1) | no |
| Fill form: password masking with a show toggle | Fill form screen, password fields | 1.2.0 | [G4](docs/regression-checklist.md#G4) | no |

## Annotation editor (Mark up PDF)

| Feature | How to reach it | Released in | Test | Automated |
|---|---|---|---|---|
| Text box on a page | PDF clip → action sheet → Annotate PDF → Text | 1.1.0 | [H1](docs/regression-checklist.md#H1) | no |
| Straight line | Annotate → Line | 1.1.0 | [H1](docs/regression-checklist.md#H1) | no |
| Freehand drawing | Annotate → Draw | 1.1.0 | [H1](docs/regression-checklist.md#H1) | no |
| Highlighter | Annotate → Highlight | 1.2.0 | [H1](docs/regression-checklist.md#H1) | no |
| Select / move / delete an annotation | Annotate → Select | 1.1.0 | [H1](docs/regression-checklist.md#H1) | no |
| Undo / redo | Toolbar | 1.2.0 | [H1](docs/regression-checklist.md#H1) | no |
| Colours and text/pen size, remembered between sessions | Toolbar | 1.1.0 | [H3](docs/regression-checklist.md#H3) | no |
| Multi-page markup | Previous/next page in the toolbar | 1.1.0 | [H1](docs/regression-checklist.md#H1) | no |
| Save the annotated PDF as a new clip | Save | 1.1.0 | [H2](docs/regression-checklist.md#H2) | no |
| Back with unsaved marks asks Save / Discard / Cancel | Annotate → Back (system or top bar) | 1.3.0 | [H4](docs/regression-checklist.md#H4) | no |
| Unsaved marks survive a language or configuration change | Annotate, then change the app language | 1.3.0 | [H5](docs/regression-checklist.md#H5) | no |

## Scan and capture

| Feature | How to reach it | Released in | Test | Automated |
|---|---|---|---|---|
| QR code scanner (camera) | Tools → QR scanner | 1.1.0 | [I1](docs/regression-checklist.md#I1) | no |
| Camera OCR (live text) | Tools → OCR camera | 1.1.0 | [I1](docs/regression-checklist.md#I1) | no |
| OCR an image clip | Image clip → action sheet → Convert → OCR image | 1.1.0 | [I2](docs/regression-checklist.md#I2) | no |
| QR from an image clip | Image viewer → Scan QR | 1.1.0 | [I4](docs/regression-checklist.md#I4) | no |
| Save the current location as a clip (with address) | Tools → Location; the "Getting location" dialog can be cancelled | 1.1.0 | [I3](docs/regression-checklist.md#I3) | no |

## Text editor and reader

| Feature | How to reach it | Released in | Test | Automated |
|---|---|---|---|---|
| Text editor (Save is labelled when the bar has room, otherwise a ✓ with a tooltip; leaving with unsaved edits asks "Save changes?") | Tap a text clip, or use the action sheet → Edit | 1.1.0 | [J1](docs/regression-checklist.md#J1) | no |
| Save-changes prompt when leaving the editor | Editor → back with unsaved changes | 1.2.0 | [J1](docs/regression-checklist.md#J1) | no |
| Markdown preview | Editor → preview | 1.1.0 | [J1](docs/regression-checklist.md#J1) | no |
| Select word / sentence / paragraph | Editor toolbar → Select word / sentence / paragraph → Word / Sentence / Select paragraph | 1.1.0 | [J2](docs/regression-checklist.md#J2) | no |
| Edit an HTML clip's source | HTML text clip (e.g. from PDF → HTML) → tap it, or action sheet → Edit | 1.1.0 | [E5](docs/regression-checklist.md#E5) | no |
| Rendered preview toggle for HTML clips in the editor | Editor top bar → Preview / Source | 1.2.0 | [J3](docs/regression-checklist.md#J3) | no |
| Reader mode with dark/bright theme and auto-scroll | Text clip → action sheet → Present | 1.1.0 | [C1](docs/regression-checklist.md#C1) | no |

## Settings / languages

| Feature | How to reach it | Released in | Test | Automated |
|---|---|---|---|---|
| Theme: light/dark/system | Settings | 1.1.0 | [K1](docs/regression-checklist.md#K1) | no |
| 13 languages plus System default, synced with Android App Info | Settings → Language | 1.2.0 | [K1](docs/regression-checklist.md#K1) | no |
| Tips, Limitations, Why, Use cases ("Where the app helps"), About, Privacy policy | Settings, above PDF & export | 1.1.0 | [K3](docs/regression-checklist.md#K3) | no |

## Other / maintenance

| Feature | How to reach it | Released in | Test | Automated |
|---|---|---|---|---|
| Delete all clips, or all clips of one type (locked clips kept) | Tools → Maintenance → Delete; choose All or a type, then Delete | 1.1.0 | [K2](docs/regression-checklist.md#K2) | no |
| Unlock all locked clips | Tools → Maintenance | 1.1.0 | [K2](docs/regression-checklist.md#K2) | no |
| Offline only: no account, no tracking | Everything works offline | 1.1.0 | [K4](docs/regression-checklist.md#K4) | no |

## Not supported

| Not supported | Notes |
|---|---|
| Converting into DOCX (or any Office format) | Conversions only produce PDF, text, images or HTML. DOCX/XLSX/RTF are read and converted to PDF |
| Sharing several files into KlipSilo at once | The app only receives `ACTION_SEND` (one item). `ACTION_SEND_MULTIPLE` is not handled |
| "Open with KlipSilo" from a file manager | No `ACTION_VIEW` intent filter. Use Share → KlipSilo instead |
| Copying a clip's content from the action sheet | There is no Copy action for a clip. Copy from the text editor's text field, or use Share |
| Encrypted backups | Backup is an unencrypted ZIP |
| Typing an address as a location | Location saves the current GPS position only |
| Sync or accounts | KlipSilo is offline only, with no account or cloud sync |
