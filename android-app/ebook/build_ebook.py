"""Builds the People Skills e-book from people.json.  Run: python3 ebook/build_ebook.py  (needs Pillow)

Writes one source of truth (assets/books/people.json, read by the in-app reader)
and, from the same blocks, an EPUB 3 + cover for Amazon KDP.
"""
import html, json, os, shutil, uuid, zipfile
from PIL import Image, ImageDraw, ImageFont

APP = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PACK = f"{APP}/app/src/main/assets/content/people.json"
BOOKS = f"{APP}/app/src/main/assets/books"
KDP = f"{APP}/ebook"

AUTHOR = "Yash Modi"  # change to your pen name before publishing
TITLE = "The Office Playbook"
SUBTITLE = "100 People-Skills Scripts for Indian Professionals: what to say to your manager, peers and stakeholders, practised in 5 minutes a day"
SHORT_SUB = "100 People-Skills Scripts for Indian Professionals"
BOOK_ID = "urn:uuid:" + str(uuid.uuid5(uuid.NAMESPACE_URL, "gyaanly/people-book"))

INTROS = {
    "Self-awareness & First Impressions":
        "Most people skills start before you say a word. How you greet people, how you listen, how you handle your own nerves and moods: colleagues notice all of it, and they form opinions fast. This chapter covers the basics that make every later conversation easier.",
    "Working with Your Manager":
        "Your manager is the single person with the most influence on your day-to-day work, your ratings and your growth. Technical people often treat that relationship as 'just do good work'. Good work matters, but so do updates, expectations and trust. This chapter shows how to manage that relationship deliberately.",
    "Peers & Teamwork":
        "Your peers decide whether work flows smoothly or gets stuck. They are also your future managers, references and partners. This chapter is about being the colleague people want on their project: reliable, generous and easy to work with.",
    "Meetings":
        "Meetings are where many careers are quietly made or stalled. Leaders judge you by how you prepare, speak up and follow through, often more than by the work they never see. This chapter turns meetings from something you sit through into something you use.",
    "Emails, Chat & Writing":
        "Most office communication is now written, and written words can't smile, nod or correct themselves. Clear, polite, structured writing saves hours and avoids misunderstandings. This chapter gives you templates you can reuse every day.",
    "Influence & Stakeholders":
        "Sooner or later you need people who don't report to you to say yes. Influence is not manipulation; it is understanding what others care about and making it easy for them to agree. This chapter covers the core skills.",
    "Feedback & Difficult Conversations":
        "The conversations we avoid are usually the ones that matter most. This chapter gives you a calm structure for receiving criticism, giving feedback, saying no, apologising and dealing with angry people, so these moments build trust instead of breaking it.",
    "Conflict, Politics & Power":
        "Every organisation has politics: people with different goals, limited resources and unwritten rules. Ignoring it doesn't make you neutral; it makes you naive. This chapter shows how to handle conflict and power ethically, without becoming the office politician you dislike.",
    "Visibility, Presentations & Executive Presence":
        "Good work that nobody sees rarely gets rewarded. This chapter is about being seen for the right reasons: clear updates, confident presentations, calm answers to tough questions, and asking for the growth you've earned.",
    "Leading Others":
        "You don't need a title to lead. Mentoring a junior, delegating a task or running a good one-to-one are leadership skills you can practise from your current seat. This final chapter prepares you for the step up, and ends with a daily routine to keep everything going.",
}

FRONT = [
    ("Who this book is for", [
        ("p", "You are good at the technical side of your job. You can build the model, fix the system or close the books. But some parts of office life feel harder than the work itself: speaking up in meetings, pushing back on your manager, handling a difficult colleague, or getting noticed by leadership."),
        ("p", "This book is for engineers, analysts, accountants and other specialists, especially in Indian workplaces, who want to get better at the people side of work without becoming someone they're not."),
    ]),
    ("How to use this book", [
        ("p", "There are 100 short lessons in 10 chapters. Each one takes about five minutes to read and gives you something to try at the office the same day."),
        ("li", "The situation: a common problem at work."),
        ("li", "What skilled people do: the habit to copy."),
        ("li", "Say it like this: ready-made phrases you can use word for word."),
        ("li", "Avoid: the mistakes that make things worse."),
        ("li", "Practise today: one small action for your next working day."),
        ("li", "Say it aloud: three phrases to rehearse until they feel natural."),
        ("p", "Read one lesson a day, or jump to the chapter you need right now. A quick self-check closes every chapter. The 30-day plan and phrase bank at the end help you keep practising."),
        ("p", "The scripts are starting points, not rules. Adapt the words to your company, your culture and your own voice."),
    ]),
]


def lesson_blocks(n, chapter_no, l):
    s = {sec["h"]: sec["b"] for sec in l["sections"]}
    b = [("h", f"{chapter_no}.{n}  {l['title']}"), ("em", l["summary"]),
         ("h3", "The situation"), ("p", s["The situation"]),
         ("h3", "What skilled people do"), ("p", s["What skilled people do"]),
         ("h3", "Say it like this")]
    b += [("q", line.strip()) for line in s["Say it like this"].split("\n") if line.strip()]
    b += [("h3", "Avoid"), ("p", s["Avoid"]),
          ("h3", "Practise today"), ("p", l["action"]),
          ("h3", "Say it aloud")]
    b += [("li", p) for p in l["speak"]]
    return b


def build():
    pack = json.load(open(PACK, encoding="utf-8"))
    chapters = []
    for title, blocks in FRONT:
        chapters.append({"title": title, "blocks": [{"t": t, "x": x} for t, x in blocks]})
    phrase_bank = []
    for ci, level in enumerate(pack["levels"], start=1):
        lessons = [l for l in pack["lessons"] if l["level"] == level]
        blocks = [("p", INTROS[level])]
        for n, l in enumerate(lessons, start=1):
            blocks += lesson_blocks(n, ci, l)
        blocks.append(("h", "Quick self-check"))
        answers = []
        for n, l in enumerate(lessons, start=1):
            q = l["quiz"][0]
            letters = "ABCD"
            blocks.append(("p", f"{n}. {q['q']}  " + "  ".join(f"({letters[i]}) {o}" for i, o in enumerate(q["options"]))))
            answers.append(f"{n}: {letters[q['answer']]}")
        blocks.append(("em", "Answers: " + ", ".join(answers)))
        chapters.append({"title": f"Chapter {ci}: {level}", "blocks": [{"t": t, "x": x} for t, x in blocks]})
        phrase_bank.append((level, [p for l in lessons for p in l["speak"]]))

    plan = [("p", "Pick one habit a day. Repeat the week's habits until they feel normal. Each week, write two lines: what went well, and what to try next.")]
    weeks = [
        ("Week 1: Show up well", ["Greet five people by name", "Listen fully in one conversation without interrupting", "Send your manager a three-line update", "Ask one clarifying question in a meeting", "Thank one colleague for something specific"]),
        ("Week 2: Speak up", ["Prepare one point before each meeting", "Say 'Can I add something?' once", "Summarise action items at the end of a call", "Rewrite one email with the main point first", "Give one piece of positive SBI feedback"]),
        ("Week 3: Handle friction", ["Ask 'which task should I pause?' when overloaded", "Disagree once using 'I see it differently because…'", "Turn one complaint into a proposal", "Apologise cleanly for one small mistake", "Write a factual timeline for one ongoing issue"]),
        ("Week 4: Get seen and lead", ["Send a one-paragraph win update", "Practise your 30-second introduction aloud", "Have a 20-minute coffee with someone from another team", "Delegate or teach one small task", "Start your impact list for your next review"]),
    ]
    for w, days in weeks:
        plan.append(("h3", w))
        plan += [("li", d) for d in days]
    plan.append(("p", "Days 21 to 30: repeat the habits that felt hardest. The hard ones are where you grow."))
    chapters.append({"title": "Your 30-day practice plan", "blocks": [{"t": t, "x": x} for t, x in plan]})

    bank = [("p", "Every 'say it aloud' phrase from the book, grouped by chapter. Read a few aloud on your way to work.")]
    for level, phrases in phrase_bank:
        bank.append(("h3", level))
        bank += [("li", p) for p in phrases]
    chapters.append({"title": "Phrase bank", "blocks": [{"t": t, "x": x} for t, x in bank]})

    chapters.append({"title": "A final word", "blocks": [{"t": t, "x": x} for t, x in [
        ("p", "People skills are not a personality type. They are habits, and habits can be practised. You don't need to change who you are; you need a few good words ready for the moments that matter, and the courage to use them."),
        ("p", "If this book helped you, an honest review on Amazon helps other professionals find it."),
        ("h3", "About the author"),
        ("p", f"{AUTHOR} is a technical professional who found the people side of office life harder than the technical work, and collected these scripts while learning it."),
    ]]})

    book = {"key": "people", "title": TITLE, "subtitle": SUBTITLE, "author": AUTHOR,
            "notice": "Written with the help of AI and reviewed by the author. The scripts are general guidance, not legal or HR advice.",
            "chapters": chapters}
    os.makedirs(BOOKS, exist_ok=True)
    os.makedirs(KDP, exist_ok=True)
    with open(f"{BOOKS}/people.json", "w", encoding="utf-8") as f:
        json.dump(book, f, ensure_ascii=False, indent=1)
    cover = f"{KDP}/cover.jpg"
    make_cover(cover)
    epub = f"{KDP}/the-office-playbook.epub"
    make_epub(book, cover, epub)
    shutil.copy(epub, f"{BOOKS}/people.epub")  # the app offers it as "Save EPUB"
    words = sum(len(b["x"].split()) for c in chapters for b in c["blocks"])
    print(len(chapters), "chapters,", words, "words ->", epub)


def make_cover(path):
    W_, H_ = 1600, 2560  # KDP's recommended 1:1.6 ratio
    img = Image.new("RGB", (W_, H_), "#C2255C")
    d = ImageDraw.Draw(img)
    for y in range(H_):  # vertical gradient
        t = y / H_
        d.line([(0, y), (W_, y)], fill=(int(194 - 90 * t), int(37 + 10 * t), int(92 + 60 * t)))
    inter = os.environ.get("COVER_FONT", "/usr/share/fonts/opentype/inter/Inter-{}.otf")  # any bold .otf/.ttf; {} = weight
    big = ImageFont.truetype(inter.format("Black"), 210)
    mid = ImageFont.truetype(inter.format("SemiBold"), 78)
    small = ImageFont.truetype(inter.format("Bold"), 90)

    def center(text, font, y, fill="white"):
        w = d.textlength(text, font=font)
        d.text(((W_ - w) / 2, y), text, font=font, fill=fill)

    d.rounded_rectangle([140, 300, 1460, 330], 15, fill="#FFD43B")
    center("THE", big, 480)
    center("OFFICE", big, 700)
    center("PLAYBOOK", big, 920)
    d.rounded_rectangle([140, 1200, 1460, 1230], 15, fill="#FFD43B")
    for i, line in enumerate(["100 people-skills scripts", "for Indian professionals", "", "What to say to your manager,", "peers and stakeholders,", "practised in 5 minutes a day"]):
        center(line, mid, 1340 + i * 110)
    center(AUTHOR.upper(), small, 2280, fill="#FFD43B")
    img.save(path, "JPEG", quality=92)


def xhtml(title, body):
    return f"""<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" lang="en" xml:lang="en">
<head><meta charset="utf-8"/><title>{html.escape(title)}</title><link rel="stylesheet" type="text/css" href="style.css"/></head>
<body>
{body}
</body>
</html>
"""


def render(blocks):
    out, in_list = [], False
    for b in blocks:
        t, x = b["t"], html.escape(b["x"])
        if t == "li" and not in_list:
            out.append("<ul>"); in_list = True
        if t != "li" and in_list:
            out.append("</ul>"); in_list = False
        out.append({"h": f"<h2>{x}</h2>", "h3": f"<h3>{x}</h3>", "p": f"<p>{x}</p>", "em": f"<p class=\"lead\"><em>{x}</em></p>",
                    "q": f"<blockquote><p>{x}</p></blockquote>", "li": f"<li>{x}</li>"}[t])
    if in_list:
        out.append("</ul>")
    return "\n".join(out)


CSS = """body { font-family: serif; line-height: 1.5; }
h1 { text-align: center; margin-top: 2em; }
h2 { margin-top: 1.6em; page-break-before: auto; }
h3 { margin-bottom: 0.2em; color: #A61E4D; }
p.lead { color: #555; }
blockquote { margin: 0.4em 1em; padding-left: 0.8em; border-left: 3px solid #C2255C; }
.title { text-align: center; }
.small { font-size: 0.85em; color: #555; }
"""


def make_epub(book, cover, path):
    items, spine, nav = [], [], []
    title_page = xhtml(book["title"], f"""<section class="title" epub:type="titlepage">
<h1>{html.escape(book['title'])}</h1>
<p>{html.escape(book['subtitle'])}</p>
<p><strong>{html.escape(book['author'])}</strong></p>
<p class="small">Copyright © 2026 {html.escape(book['author'])}. All rights reserved.</p>
<p class="small">{html.escape(book['notice'])}</p>
</section>""")
    files = {"title.xhtml": title_page}
    for i, c in enumerate(book["chapters"], start=1):
        name = f"c{i:02d}.xhtml"
        files[name] = xhtml(c["title"], f"<section epub:type=\"chapter\">\n<h1>{html.escape(c['title'])}</h1>\n{render(c['blocks'])}\n</section>")
        nav.append(f'<li><a href="{name}">{html.escape(c["title"])}</a></li>')
    files["nav.xhtml"] = xhtml("Contents", '<nav epub:type="toc" id="toc"><h1>Contents</h1><ol>\n<li><a href="title.xhtml">Title page</a></li>\n' + "\n".join(nav) + "\n</ol></nav>")
    for name in files:
        props = ' properties="nav"' if name == "nav.xhtml" else ""
        items.append(f'<item id="{name[:-6]}" href="{name}" media-type="application/xhtml+xml"{props}/>')
        if name != "nav.xhtml":
            spine.append(f'<itemref idref="{name[:-6]}"/>')
    spine.insert(1, '<itemref idref="nav"/>')
    opf = f"""<?xml version="1.0" encoding="utf-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="bookid" xml:lang="en">
<metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
<dc:identifier id="bookid">{BOOK_ID}</dc:identifier>
<dc:title>{html.escape(book['title'])}</dc:title>
<dc:creator>{html.escape(book['author'])}</dc:creator>
<dc:language>en</dc:language>
<dc:description>{html.escape(book['subtitle'])}</dc:description>
<meta property="dcterms:modified">2026-10-20T00:00:00Z</meta>
<meta name="cover" content="cover-img"/>
</metadata>
<manifest>
<item id="css" href="style.css" media-type="text/css"/>
<item id="cover-img" href="cover.jpg" media-type="image/jpeg" properties="cover-image"/>
{chr(10).join(items)}
</manifest>
<spine>
{chr(10).join(spine)}
</spine>
</package>
"""
    with zipfile.ZipFile(path, "w") as z:
        z.writestr(zipfile.ZipInfo("mimetype"), "application/epub+zip", compress_type=zipfile.ZIP_STORED)
        z.writestr("META-INF/container.xml", """<?xml version="1.0" encoding="utf-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
<rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles>
</container>""", compress_type=zipfile.ZIP_DEFLATED)
        z.writestr("OEBPS/content.opf", opf, compress_type=zipfile.ZIP_DEFLATED)
        z.writestr("OEBPS/style.css", CSS, compress_type=zipfile.ZIP_DEFLATED)
        z.write(cover, "OEBPS/cover.jpg", compress_type=zipfile.ZIP_DEFLATED)
        for name, body in files.items():
            z.writestr(f"OEBPS/{name}", body, compress_type=zipfile.ZIP_DEFLATED)


if __name__ == "__main__":
    build()
