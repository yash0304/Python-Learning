# The Office Playbook: e-book + fail-fast business plan

Files in this folder:

| File | What it is |
|---|---|
| `the-office-playbook.epub` | KDP-ready EPUB 3 (passes EPUBCheck 5.1 with 0 errors / 0 warnings), about 17,000 words |
| `cover.jpg` | 1600 × 2560 cover (KDP's recommended size) |
| `build_ebook.py` | Rebuilds the book, cover and in-app copy from `people.json`. Change `AUTHOR`, `TITLE` and the chapter intros here |

The same book is in the app: **People Skills at Work → 📖 Read the e-book**. There you can also listen to it, share it, or save it as an EPUB. For Amazon, upload the files in this folder: they include the cover.

---

## Brutally honest assessment

**Can you sell it on Amazon? Yes, it's legal and allowed. Will it make real money as it is? Almost certainly not.**

1. **The content is AI-generated and generic.** It's correct and useful, but anyone can ask an AI for "100 workplace communication scripts" in an afternoon. Thousands already have. Kindle is full of such books, and readers increasingly spot and avoid them.
2. **There is no author authority.** People buy soft-skills books from people they trust: a known manager, coach or HR leader. A first-time author with no audience and no stories gets close to zero organic sales. Expect a handful of copies a month, or none, unless you bring the traffic yourself.
3. **It's short.** About 17,000 words is a "short read". That's fine at a ₹149 / $2.99 price, but it won't support a premium price.
4. **You can't protect it.** In the US, purely AI-generated text generally isn't copyrightable. Only your own human writing and editing is. If it sells, it can be copied.
5. **You must declare AI content.** KDP asks at upload whether the text or images are AI-generated. This book is, unless you rewrite it substantially. Declare it honestly; hiding it risks your account.
6. **The real asset is the niche, not the book.** "People skills for Indian technical professionals" is a real, under-served pain, and you *are* the target reader. That's the only genuine edge here, and it only works if *your* experience is in the book.

**Verdict:** publish it as a cheap **experiment** to learn the KDP process, not as a business. The business, if there is one, is *you* becoming a credible voice on this topic. The book then becomes your calling card.

---

## Fail-fast plan (about 3 weeks, ₹0–3,000)

### Step 1: Test demand before Amazon (days 1–5, ₹0)
- Post 5 lessons from the book on LinkedIn, one a day. Pick the strongest: *Saying no to your manager*, *Delivering bad news*, *Asking for a promotion*, *SBI feedback*, *Talking to senior leaders*. Write them in your own voice and add one real story from your work each time.
- **Measure:** saves, comments, DMs and "where can I get more?" replies.
- **Kill or continue:** if 5 posts get almost no engagement from your own network, a stranger on Amazon won't buy either. Stop here; you've lost nothing. If people ask for more, continue.

### Step 2: Make it yours (days 6–10)
- Add **one real story per chapter** (10 in total): a time you got it wrong and what you'd say now. This is what makes the book worth buying, and it's human-authored, so you can protect it.
- Read all 100 scripts. Delete or rewrite any that don't sound like your workplace.
- Edit `build_ebook.py`: set `AUTHOR` (your name or a pen name), add your stories to the chapter intros (`INTROS`), and run `python3 ebook/build_ebook.py`.
- Optional: improve the cover in Canva (free), keeping it 1600 × 2560.

### Step 3: Publish on KDP (day 11, about 1 hour, ₹0)
1. Create an account at **kdp.amazon.com** with your Amazon login.
2. **Tax interview:** you're an Indian resident, so you fill the W-8BEN form and give your PAN as your foreign tax ID. The India–US treaty cuts US withholding on royalties from 30% to 15%.
3. **Payments:** add your Indian bank account.
4. Click **Create → Kindle eBook**. Fill in:
   - **Title:** *The Office Playbook*. **Subtitle:** *100 People-Skills Scripts for Indian Professionals*.
   - **Description:** 150–200 words on the pain ("great at your job, but meetings, managers and office politics drain you?") plus what's inside.
   - **7 keywords**, for example: workplace communication, office politics, soft skills for engineers, how to talk to your manager, corporate etiquette India, career growth, professional communication.
   - **Categories:** Business Communication, plus Careers or Workplace Culture.
   - **AI content question:** answer **Yes** for text (and for the cover, if you used AI for it).
   - Don't buy an ISBN; KDP doesn't need one for ebooks.
5. Upload the EPUB and `cover.jpg`. Check every chapter in the online previewer.
6. **Rights & pricing:**
   - Price at **$2.99 / ₹149**. $2.99 is the lowest US price that earns the **70%** royalty.
   - **Enrol in KDP Select** (90 days of ebook exclusivity on Amazon). On Amazon.in the 70% royalty needs Select, and Select also pays you for Kindle Unlimited page reads.
7. Publish. It usually goes live within 72 hours.

### Step 4: Launch (days 12–21, up to ₹3,000)
- Post the book to the network that engaged in Step 1.
- Ask readers for **honest** reviews. Amazon forbids paid or swapped reviews, and reviews from close family.
- Optional ad test: an Amazon Ads **automatic** campaign at about ₹150–200 a day for 10 days. Stop it if clicks don't turn into sales.

### Step 5: Decide (day 21)

| Signal after 3 weeks | Decision |
|---|---|
| Fewer than 10 sales, little engagement | **Kill.** Keep the book live (it costs nothing), stop spending time and money on it |
| 10–50 sales, or good reviews | **Iterate:** write a better second book in the same niche, or a paperback version |
| Strong LinkedIn engagement, people DM asking for help | **Pivot up:** the real money is in workshops, mentoring calls or corporate training. The book becomes your free or cheap lead magnet |

**Rule:** don't spend more than 3 weeks or ₹3,000 before Step 5. That's what "fail fast" means here.
