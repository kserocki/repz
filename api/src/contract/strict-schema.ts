type JsonSchema = Record<string, unknown>;

const COMPONENT_REF_PREFIX = "#/components/schemas/";

function isJsonSchema(value: unknown): value is JsonSchema {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function resolveRef(ref: string, components: Record<string, unknown>): JsonSchema {
  if (!ref.startsWith(COMPONENT_REF_PREFIX)) {
    throw new Error(`Unsupported $ref (expected "${COMPONENT_REF_PREFIX}<Name>"): ${ref}`);
  }
  const name = ref.slice(COMPONENT_REF_PREFIX.length);
  const resolved = components[name];
  if (!isJsonSchema(resolved)) {
    throw new Error(`$ref points at an unknown component schema: ${name}`);
  }
  return resolved;
}

/** Merges strict `allOf` branches (already strict) into one object schema. */
function mergeAllOf(branches: JsonSchema[]): JsonSchema {
  const properties: Record<string, unknown> = {};
  const required: string[] = [];
  for (const branch of branches) {
    if (isJsonSchema(branch.properties)) {
      Object.assign(properties, branch.properties);
    }
    if (Array.isArray(branch.required)) {
      for (const key of branch.required) {
        if (!required.includes(key)) {
          required.push(key);
        }
      }
    }
  }
  return { type: "object", properties, required, additionalProperties: false };
}

function isObjectSchema(node: JsonSchema): boolean {
  return node.type === "object" || isJsonSchema(node.properties);
}

/**
 * Builds a strict copy of an OpenAPI 3.1 schema for the contract test: every `$ref` inlined, the
 * one `allOf` of objects (`AuthSession`) merged, and `additionalProperties: false` added to every
 * object schema that does not already set it. The source schemas and the runtime stay untouched;
 * this only tightens what the fixtures are checked against.
 */
export function toStrictSchema(schema: unknown, components: Record<string, unknown>): object {
  if (!isJsonSchema(schema)) {
    throw new Error(`Expected a schema object, got: ${JSON.stringify(schema)}`);
  }

  if (typeof schema.$ref === "string") {
    return toStrictSchema(resolveRef(schema.$ref, components), components);
  }

  if (Array.isArray(schema.allOf)) {
    const branches = schema.allOf.map((branch) => toStrictSchema(branch, components) as JsonSchema);
    return mergeAllOf(branches);
  }

  if (Array.isArray(schema.oneOf)) {
    return { ...schema, oneOf: schema.oneOf.map((branch) => toStrictSchema(branch, components)) };
  }

  if (Array.isArray(schema.anyOf)) {
    return { ...schema, anyOf: schema.anyOf.map((branch) => toStrictSchema(branch, components)) };
  }

  const result: JsonSchema = { ...schema };

  if (isJsonSchema(schema.items)) {
    result.items = toStrictSchema(schema.items, components);
  }

  if (isObjectSchema(schema)) {
    if (isJsonSchema(schema.properties)) {
      result.properties = Object.fromEntries(
        Object.entries(schema.properties).map(([key, value]) => [
          key,
          toStrictSchema(value, components),
        ]),
      );
    }
    if (!("additionalProperties" in result)) {
      result.additionalProperties = false;
    }
  }

  return result;
}
