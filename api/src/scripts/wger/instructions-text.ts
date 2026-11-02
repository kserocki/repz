const KNOWN_TAGS = new Set(["p", "li", "ol", "ul", "strong", "em"]);
const INLINE_TAGS = new Set(["strong", "em"]);
const BLOCK_TAGS = new Set(["p", "ol", "ul"]);

/** Any `<...>` span, however malformed, so a tag with attributes or upper case is caught and reported, never silently passed through or silently dropped. */
const TAG_LIKE_PATTERN = /<\/?[^>]*>/g;
/** A *strictly* well-formed known tag: lower case, no attributes, exactly `<tag>` or `</tag>`. */
const STRICT_TAG_PATTERN = /^<(\/?)([a-z]+)>$/;

const ENTITY_PATTERN = /&(#x[0-9a-fA-F]+|#\d+|[a-zA-Z]+);/g;
const MAX_CODE_POINT = 0x10ffff;
const SURROGATE_RANGE_START = 0xd800;
const SURROGATE_RANGE_END = 0xdfff;

const NAMED_ENTITIES: Readonly<Record<string, string>> = {
  amp: "&",
  lt: "<",
  gt: ">",
  quot: '"',
  apos: "'",
  nbsp: " ",
};

type Token =
  | { kind: "open"; tag: string }
  | { kind: "close"; tag: string }
  | { kind: "text"; value: string };

export type InstructionsTextResult =
  | { ok: true; text: string | null }
  | { ok: false; reason: string };

function isValidCodePoint(codePoint: number): boolean {
  return (
    Number.isInteger(codePoint) &&
    codePoint >= 0 &&
    codePoint <= MAX_CODE_POINT &&
    !(codePoint >= SURROGATE_RANGE_START && codePoint <= SURROGATE_RANGE_END)
  );
}

/** Decodes every entity in `text`, or fails on the first one it cannot resolve (invalid numeric value or unknown name) — never a silent pass-through of raw `&…;` text. */
function decodeEntities(text: string): { ok: true; text: string } | { ok: false; entity: string } {
  let failedEntity: string | null = null;
  const decoded = text.replace(ENTITY_PATTERN, (match, entity: string) => {
    if (failedEntity) {
      return match;
    }
    if (entity.startsWith("#")) {
      const codePoint = entity.startsWith("#x")
        ? Number.parseInt(entity.slice(2), 16)
        : Number.parseInt(entity.slice(1), 10);
      if (isValidCodePoint(codePoint)) {
        return String.fromCodePoint(codePoint);
      }
      failedEntity = match;
      return match;
    }
    const named = NAMED_ENTITIES[entity];
    if (named === undefined) {
      failedEntity = match;
      return match;
    }
    return named;
  });
  return failedEntity ? { ok: false, entity: failedEntity } : { ok: true, text: decoded };
}

function collapseWhitespace(text: string): string {
  return text.replace(/\s+/g, " ").trim();
}

/** Validates every tag-like span up front, then splits `html` into a flat token stream. */
function tokenize(html: string): { ok: true; tokens: Token[] } | { ok: false; reason: string } {
  const tokens: Token[] = [];
  let lastIndex = 0;

  for (const match of html.matchAll(TAG_LIKE_PATTERN)) {
    const raw = match[0];
    const strict = STRICT_TAG_PATTERN.exec(raw);
    if (!strict) {
      return { ok: false, reason: `malformed or non-lowercase tag ${raw}` };
    }
    const [, closingSlash, tagName] = strict;
    if (!tagName || !KNOWN_TAGS.has(tagName)) {
      return { ok: false, reason: `unknown tag ${raw}` };
    }

    if (match.index > lastIndex) {
      tokens.push({ kind: "text", value: html.slice(lastIndex, match.index) });
    }
    tokens.push({ kind: closingSlash ? "close" : "open", tag: tagName });
    lastIndex = match.index + raw.length;
  }
  if (lastIndex < html.length) {
    tokens.push({ kind: "text", value: html.slice(lastIndex) });
  }

  return { ok: true, tokens };
}

/**
 * Converts wger's exercise-description HTML to plain text for the app's `Text` composable (no
 * markdown engine, so HTML in would be a needless XSS-shaped surface). Only `p`, `li`, `ol`,
 * `ul`, `strong`, `em` are known, each exactly lower case and attribute-free; anything else —
 * an unknown tag, a tag with attributes, upper case, a block nested inside a paragraph or list
 * item, text outside a block, or an entity that doesn't resolve — is a problem, never a silent
 * drop and never a thrown exception.
 *
 * `<p>` becomes a paragraph; `<li>` becomes one line, numbered `"1. "` inside `<ol>` and
 * bulleted `"• "` inside `<ul>`; paragraphs and lists are joined with a blank line between them.
 * An input with no block content (or only whitespace) yields `null`.
 */
export function htmlToPlainText(html: string): InstructionsTextResult {
  const tokenized = tokenize(html);
  if (!tokenized.ok) {
    return tokenized;
  }

  const blocks: string[] = [];
  /** Tag names currently open, outermost first; also doubles as the parsing context. */
  const stack: string[] = [];
  let inlineBuffer = "";
  let listItems: string[] = [];
  let listItemCount = 0;

  function currentContext(): string | undefined {
    return stack.at(-1);
  }

  function appendNormalisedLine(onto: (line: string) => void): InstructionsTextResult | null {
    const decoded = decodeEntities(inlineBuffer);
    if (!decoded.ok) {
      return { ok: false, reason: `entity that does not resolve: ${decoded.entity}` };
    }
    const line = collapseWhitespace(decoded.text);
    if (line) {
      onto(line);
    }
    inlineBuffer = "";
    return null;
  }

  for (const token of tokenized.tokens) {
    if (token.kind === "text") {
      const context = currentContext();
      if (context === "p" || context === "li" || INLINE_TAGS.has(context ?? "")) {
        inlineBuffer += token.value;
      } else if (token.value.trim().length > 0) {
        return {
          ok: false,
          reason: context ? `text directly inside <${context}>` : "text outside a block",
        };
      }
      continue;
    }

    if (token.kind === "open") {
      const context = currentContext();
      if (BLOCK_TAGS.has(token.tag)) {
        if (context !== undefined) {
          return { ok: false, reason: `nested <${token.tag}> inside <${context}>` };
        }
        stack.push(token.tag);
        if (token.tag === "ol" || token.tag === "ul") {
          listItems = [];
          listItemCount = 0;
        }
      } else if (token.tag === "li") {
        if (context !== "ol" && context !== "ul") {
          return { ok: false, reason: "<li> outside <ol>/<ul>" };
        }
        stack.push("li");
      } else if (INLINE_TAGS.has(token.tag)) {
        if (context !== "p" && context !== "li") {
          return { ok: false, reason: `<${token.tag}> outside a paragraph or list item` };
        }
        stack.push(token.tag);
      }
      continue;
    }

    // token.kind === "close"
    const context = currentContext();
    if (context !== token.tag) {
      return { ok: false, reason: `mismatched closing tag </${token.tag}>` };
    }
    stack.pop();

    if (token.tag === "p") {
      const problem = appendNormalisedLine((line) => blocks.push(line));
      if (problem) {
        return problem;
      }
    } else if (token.tag === "li") {
      const parent = currentContext();
      const prefix = parent === "ol" ? `${++listItemCount}. ` : "• ";
      const problem = appendNormalisedLine((line) => listItems.push(`${prefix}${line}`));
      if (problem) {
        return problem;
      }
    } else if (token.tag === "ol" || token.tag === "ul") {
      if (listItems.length > 0) {
        blocks.push(listItems.join("\n"));
      }
      listItems = [];
    }
    // strong/em: inline text already accumulated into inlineBuffer by the surrounding p/li; nothing else to do.
  }

  if (stack.length > 0) {
    return { ok: false, reason: `unclosed tag <${stack.at(-1)}>` };
  }

  const text = blocks.join("\n\n").trim();
  // Post-condition: the converted text must never contain anything tag-shaped.
  if (/<\/?[a-zA-Z]/.test(text)) {
    return { ok: false, reason: "converted text still contains markup" };
  }

  return { ok: true, text: text.length > 0 ? text : null };
}
