const ALLOWED_TAGS = new Set(["p", "li", "ol", "ul", "strong", "em"]);
const TAG_PATTERN = /<\/?([a-zA-Z][a-zA-Z0-9]*)[^>]*>/g;
const BLOCK_PATTERN = /<(p|ol|ul)>([\s\S]*?)<\/\1>/g;
const LIST_ITEM_PATTERN = /<li>([\s\S]*?)<\/li>/g;
const INLINE_TAG_PATTERN = /<\/?(strong|em)>/g;
const ENTITY_PATTERN = /&(#x[0-9a-fA-F]+|#\d+|[a-zA-Z]+);/g;

const NAMED_ENTITIES: Readonly<Record<string, string>> = {
  amp: "&",
  lt: "<",
  gt: ">",
  quot: '"',
  apos: "'",
  nbsp: " ",
};

function decodeEntities(text: string): string {
  return text.replace(ENTITY_PATTERN, (match, entity: string) => {
    if (entity.startsWith("#x")) {
      return String.fromCodePoint(Number.parseInt(entity.slice(2), 16));
    }
    if (entity.startsWith("#")) {
      return String.fromCodePoint(Number.parseInt(entity.slice(1), 10));
    }
    return NAMED_ENTITIES[entity] ?? match;
  });
}

function collapseWhitespace(text: string): string {
  return text.replace(/\s+/g, " ").trim();
}

/** Unwraps `<strong>`/`<em>` (keeps the inner text), decodes entities and collapses whitespace. */
function normaliseLine(fragment: string): string {
  return collapseWhitespace(decodeEntities(fragment.replace(INLINE_TAG_PATTERN, "")));
}

export type InstructionsTextResult =
  | { ok: true; text: string | null }
  | { ok: false; unknownTag: string };

/**
 * Converts wger's exercise-description HTML to plain text for the app's `Text` composable (no
 * markdown engine, so HTML in would be a needless XSS-shaped surface). Only `p`, `li`, `ol`,
 * `ul`, `strong`, `em` are known (verified against every EN description in the live API,
 * 2026-11-02); any other tag is a problem, so a new upstream tag never silently loses structure
 * or lets markup leak into the stored text.
 *
 * `<p>` becomes a paragraph; `<li>` becomes one line, numbered `"1. "` inside `<ol>` and
 * bulleted `"• "` inside `<ul>`; paragraphs and lists are joined with a blank line between them.
 * An input with no block content (or only whitespace) yields `null`.
 */
export function htmlToPlainText(html: string): InstructionsTextResult {
  for (const match of html.matchAll(TAG_PATTERN)) {
    const tag = (match[1] ?? "").toLowerCase();
    if (!ALLOWED_TAGS.has(tag)) {
      return { ok: false, unknownTag: tag };
    }
  }

  const blocks: string[] = [];
  for (const match of html.matchAll(BLOCK_PATTERN)) {
    const [, tagName, inner] = match;
    if (tagName === "p") {
      const line = normaliseLine(inner ?? "");
      if (line) {
        blocks.push(line);
      }
      continue;
    }

    const prefix = tagName === "ol" ? (index: number) => `${index + 1}. ` : () => "• ";
    const items = [...(inner ?? "").matchAll(LIST_ITEM_PATTERN)]
      .map((item) => normaliseLine(item[1] ?? ""))
      .filter((line) => line.length > 0);
    if (items.length > 0) {
      blocks.push(items.map((line, index) => `${prefix(index)}${line}`).join("\n"));
    }
  }

  const text = blocks.join("\n\n").trim();
  return { ok: true, text: text.length > 0 ? text : null };
}
