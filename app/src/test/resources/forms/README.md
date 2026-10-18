# Form test fixtures

Small public sample AcroForm PDFs, copied from testaroo.com (Novus Examples,
"free to use, no real data" — their own footer):

- [job-application.pdf](https://www.testaroo.com/files/forms/acroform/job-application.pdf)
- [event-registration.pdf](https://www.testaroo.com/files/forms/acroform/event-registration.pdf)

Both carry a radio group written as one parent `/Btn` field with several widget
kids and per-kid `/AP /N` on-states but **no** `/Ff` Radio flag
(`auth`: citizen/visa/sponsor, `shirt`: s/m/l/xl), plus a multi-line text field
(`/Ff` 4096: `cover`, `notes`) and regular checkboxes.
Used by the form-field JVM tests (#67, #68).
