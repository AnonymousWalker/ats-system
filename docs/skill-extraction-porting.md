# Skill extraction with priority — porting guide

Language-agnostic specification of the ATS skill extractor so it can be reimplemented in another language.

Source of truth in this repo:

- `src/main/java/com/example/atssystem/skill/extraction/SkillExtractor.java`
- `src/main/java/com/example/atssystem/skill/extraction/TextNormalizer.java`
- `src/main/java/com/example/atssystem/skill/extraction/CatalogTerm.java`
- `src/main/java/com/example/atssystem/skill/extraction/ExtractedSkill.java`
- `src/main/java/com/example/atssystem/skill/domain/SkillPriority.java`
- Tests: `src/test/java/com/example/atssystem/skill/extraction/SkillExtractorTest.java`

The algorithm is **deterministic**: longest-term-first complete matching with alias occupancy, evidence snippets, and JD required/preferred suggestion from section headings.

---

## Data models

### Catalog term (input)

One searchable alias that resolves to a canonical skill. A skill may have many aliases.

```text
CatalogTerm {
  skillId: UUID (or stable string id)
  canonicalName: string   // e.g. "JavaScript"
  alias: string           // e.g. "js", "javascript"
}
```

### Skill priority

```text
REQUIRED | PREFERRED
```

### Extracted skill (output)

One skill after deduplication (at most one row per `skillId` per document).

```text
ExtractedSkill {
  skillId: UUID
  canonicalName: string
  matchedTerm: string     // catalog alias that produced the kept hit (as stored, not the doc substring)
  evidence: string | null // snippet from ORIGINAL document text
  priority: REQUIRED | PREFERRED | null
  priorityAmbiguous: bool
}
```

### Entry points

| Call | `classifyPriority` | Priority fields |
|------|--------------------|-----------------|
| Resume extraction | `false` | `priority = null`, `priorityAmbiguous = false` |
| Job-description extraction | `true` | filled / may be ambiguous |

Blank or null input text → empty list.

---

## 1. Alias normalize

Used when preparing catalog aliases (not the full document map — see §3).

```text
normalize(value):
  if value is null → ""
  return lowercase(value)          // Unicode / Locale.ROOT case fold
         .replaceAll(\s+, " ")
         .trim()
```

---

## 2. Prepare catalog (once at construction)

For each `CatalogTerm`:

1. `normalizedAlias = normalize(alias)`
2. Build a **complete-term** regex against **normalized document text**:

   ```text
   (?<!\p{Alnum}) <Pattern.quote(normalizedAlias)> (?!\p{Alnum})
   ```

   Flags: case-insensitive + Unicode case.

   Boundaries are “not a Unicode alphanumeric” (or string edges). That preserves `C++`, `C#`, `.NET`, and multi-word aliases. Combined with longest-first occupancy, `"java"` does not steal characters inside `"javascript"`.

3. Store prepared term: `{ skillId, canonicalName, displayAlias: alias, normalizedAlias, pattern }`

4. Sort prepared terms by:

   - `len(normalizedAlias)` **descending** (longest first)
   - then `normalizedAlias` ascending (stable tie-break)

---

## 3. Document normalize (matching + evidence index map)

Walk the **original** string by Unicode code point. Produce:

- `normalized`: lowercase, whitespace collapsed to single spaces, trimmed
- `normToOriginal[]`: for each index in `normalized`, the start index in `original` of the code point that produced it

```text
NormalizedText.of(original):
  normalized = ""
  mapping = []          // list of original indices, one per normalized char
  lastWasSpace = false

  for each codepoint cp at original index i:
    if isWhitespace(cp):
      if not lastWasSpace and normalized is not empty:
        append ' ' to normalized
        append i to mapping
        lastWasSpace = true
    else:
      lower = lowercase(cp)   // may be more than one UTF-16/UTF-8 unit
      append lower to normalized
      for each char unit of lower:
        append i to mapping
      lastWasSpace = false

  while normalized ends with ' ':
    remove last char from normalized
    pop last from mapping

  return { original, normalized, normToOriginal: mapping }
```

### `mapToOriginal(normalizedIndex)`

```text
if normalizedIndex <= 0 → 0
if normalizedIndex >= len(normToOriginal) → len(original)
else → normToOriginal[normalizedIndex]
```

---

## 4. Match skills (core)

```text
extract(text, classifyPriority):
  if text is null or blank → []

  nt = NormalizedText.of(text)
  occupied = boolean array of length len(nt.normalized), all false
  firstHitBySkill = empty map   // skillId → MatchHit

  for term in preparedTerms:                    // longest alias first
    for each regex match of term.pattern in nt.normalized:
      start, end = match span                   // [start, end)
      if isOccupied(occupied, start, end):
        continue
      markOccupied(occupied, start, end)

      if skillId not in firstHitBySkill:
        evidence = evidenceSnippet(nt, start, end)
        firstHitBySkill[skillId] = MatchHit{ term, evidence, start, end }

  priorities = classifyPriority
    ? classifyPriorities(text, firstHitBySkill)
    : {}

  results = []
  for hit in firstHitBySkill.values():
    skillId = hit.term.skillId
    priority = classifyPriority ? priorities.get(skillId) : null
    ambiguous = classifyPriority and priority is null
    results.append(ExtractedSkill{
      skillId,
      canonicalName: hit.term.canonicalName,
      matchedTerm: hit.term.displayAlias,
      evidence: hit.evidence,
      priority,
      priorityAmbiguous: ambiguous
    })

  sort results by:
    canonicalName ascending (case-insensitive),
    then matchedTerm ascending (case-insensitive)

  return results
```

### Occupancy

```text
isOccupied(occupied, start, end):
  for i in [start, end):
    if occupied[i] → true
  return false

markOccupied(occupied, start, end):
  for i in [start, end):
    occupied[i] = true
```

A span is blocked if **any** index in `[start, end)` is already taken. Claiming marks **all** of those indices.

### Deduplication

Only the **first** hit per `skillId` is stored. Later matches for the same skill still consume occupancy when their spans are free, but do not replace the stored hit. “First” means first success in longest-term scan order.

---

## 5. Evidence snippet

Given a match on normalized text `[normStart, normEnd)`:

1. `originalStart = mapToOriginal(normStart)`
2. `originalEnd = mapToOriginal(normEnd)`
3. Expand window: `snippetStart = max(0, originalStart - 40)`, `snippetEnd = min(len(original), originalEnd + 40)`
4. Expand to word boundaries (stop when previous/next char is whitespace), but do not expand more than **60** characters beyond the match on each side
5. Take `original[snippetStart:snippetEnd]`, replace `\s+` with a single space, trim
6. If `snippetStart > 0`, prefix Unicode ellipsis `…` (U+2026)
7. If `snippetEnd < len(original)`, suffix `…`

---

## 6. Priority classification (job descriptions only)

Only runs when `classifyPriority == true`.

### 6.1 Heading line regex

Full-line match, multiline + case-insensitive (`(?im)`):

```text
^[ \t]*(?:#{1,6}\s*)?(?:\*\*|__)?\s*
(required(?:\s+skills?|\s+qualifications?)?|requirements|must[- ]haves?|
 minimum\s+qualifications?|what\s+you.?ll\s+need|
 preferred(?:\s+skills?|\s+qualifications?)?|nice[- ]to[- ]haves?|
 optional(?:\s+skills?)?|bonus(?:\s+points?)?|pluses?)
\s*(?:\*\*|__)?\s*:?\s*$
```

Capture group 1 is the label. Then: collapse whitespace `\s+` → space, trim.

Supports markdown-ish headings (`#`, `**`, `__`) and optional trailing `:`.

### 6.2 Label → priority

```text
REQUIRED_HEADING  = (?i)^(required|requirements|must[- ]have|minimum\s+qualification|what\s+you.?ll\s+need)
PREFERRED_HEADING = (?i)^(preferred|nice[- ]to[- ]have|optional|bonus|plus)

toPriority(label):
  n = lowercase(label)
  if REQUIRED_HEADING finds in n → REQUIRED
  if PREFERRED_HEADING finds in n → PREFERRED
  else → null
```

### 6.3 Parse sections

```text
parseSections(text):
  headings = []
  for each HEADING_LINE match:
    label = collapseWhitespace(group1).trim()
    headings.append({ start, end, priority: toPriority(label) })

  if headings is empty:
    return [ Section{ priority: null, body: text } ]

  sections = []
  if headings[0].start > 0:
    sections.append(Section{ priority: null, body: text[0 : headings[0].start] })

  for i, heading in headings:
    bodyStart = heading.end
    bodyEnd = headings[i+1].start if i+1 < len(headings) else len(text)
    sections.append(Section{ priority: heading.priority, body: text[bodyStart:bodyEnd] })

  return sections
```

Preamble before the first heading has `priority: null` and is ignored for classification.

### 6.4 Classify each matched skill

```text
classifyPriorities(originalText, firstHitBySkill):
  sections = parseSections(originalText)
  priorities = {}

  for skillId in firstHitBySkill.keys():
    seen = empty set of SkillPriority
    for section in sections:
      if section.priority is null:
        continue
      # Re-run extraction on section body WITHOUT priority classification
      sectionHits = extract(section.body, classifyPriority=false)
      if any hit with skillId:
        seen.add(section.priority)

    if len(seen) == 1:
      priorities[skillId] = the single priority
    # len 0 or 2 → leave unset → ambiguous

  return priorities
```

### Ambiguity rules

| Situation | `priority` | `priorityAmbiguous` |
|-----------|------------|---------------------|
| Skill only under Required-like heading | `REQUIRED` | `false` |
| Skill only under Preferred-like heading | `PREFERRED` | `false` |
| Skill under both Required and Preferred | `null` | `true` |
| Skill only in preamble / unlabeled body / no headings | `null` | `true` |

Resume path never sets ambiguous.

---

## Porting notes

1. **Java `\p{Alnum}`** — Unicode letter or digit. In other languages, `\w` often includes `_`; prefer an explicit “not letter and not digit” lookaround equivalent to Java’s `\p{Alnum}`.
2. **Always escape aliases** as literals (`Pattern.quote` / `\Q...\E` / `re.escape`). Aliases include `c++`, `c#`, `.net`.
3. **Do not skip occupancy.** Longest-first alone is not enough if a short alias could still match a free span incorrectly; occupancy + longest-first together disambiguate nested aliases.
4. **Priority classification re-calls `extract(..., false)`** on each labeled section body (no nested priority recursion).
5. **`matchedTerm`** is the catalog `alias` string as stored, not the exact casing from the document.
6. Catalog in this app is small (~40 skills / ~70 aliases). Complexity is roughly `O(aliases × text_length)` plus section re-extracts for JDs.
7. Ellipsis character in evidence is Unicode `…` (U+2026), not three ASCII dots.

---

## Conformance tests

Mirror these cases from `SkillExtractorTest` (minimal catalog fixture listed there):

| Case | Expect |
|------|--------|
| `"Used Postgres for OLTP workloads."` | One skill: PostgreSQL; `matchedTerm` = `postgres`; evidence contains `Postgres` |
| `"Built APIs in Java and frontends in JavaScript."` | Both Java and JavaScript |
| `"Strong JavaScript and React experience."` | JavaScript + React; **not** Java |
| `"Five years of Java backend work."` | Java; **not** JavaScript |
| `"Experience with C++, C#, and .NET microservices."` | C++, C#, .NET |
| `"Skills: JAVA, React.js, DOCKER; spring boot."` | Java, React (`.` is a non-alnum boundary after `react`), Docker, Spring Boot |
| `"Java, java, JAVA — and more Java services."` | Exactly one result: Java |
| JD with `Required skills:` / `Preferred:` sections | Skills under Required → `REQUIRED`; under Preferred → `PREFERRED`; not ambiguous |
| JD with no headings (`"Looking for Java and Docker experience."`) | All hits: `priority == null`, `priorityAmbiguous == true` |
| JD with Java in both Required and Preferred; Docker only Preferred | Java ambiguous; Docker `PREFERRED` |
| `"Familiar with JS tooling."` | JavaScript via alias `js`; not Java |

### Minimal catalog fixture (from tests)

```text
Java          → java
JavaScript    → javascript, js
PostgreSQL    → postgresql, postgres
C++           → c++, cpp
C#            → c#
.NET          → .net, dotnet
Spring Boot   → spring boot
React         → react
Docker        → docker
```

---

## Pseudocode summary

```text
function SkillExtractor(catalogTerms):
  terms = prepareAndSort(catalogTerms)

function extractFromResume(text) = extract(text, false)
function extractFromJobDescription(text) = extract(text, true)

function extract(text, classifyPriority):
  // normalize → occupy → longest-term match → evidence
  // if classifyPriority: section headings → re-extract sections → unique priority or ambiguous
  // sort and return
```

That is the full server-side skill extraction + priority logic.
