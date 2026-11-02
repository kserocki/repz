import { Ajv2020 } from "ajv/dist/2020.js";
import addFormatsDefault from "ajv-formats";

/**
 * `ajv-formats` 3.0.1 ships only a default export, and its ambient CommonJS `.d.ts` does not
 * interop cleanly under `moduleResolution: nodenext` (the default import resolves to the whole
 * module namespace, which has no call signature). `Ajv2020` itself has no such issue: it is also
 * exported by name, so it is imported directly above. The real function is recovered here with
 * its own declared type, once, so every caller gets a properly typed `addFormats`.
 */
type AddFormatsFn = typeof import("ajv-formats")["default"];
const addFormats = addFormatsDefault as unknown as AddFormatsFn;

/** One Ajv2020 instance per caller (validator state is not meant to be shared): strict mode,
 * every error collected, `ajv-formats`' formats registered, and the OpenAPI-only `example`
 * keyword allow-listed so a schema carrying it does not trip `strict: true`. */
export function createContractAjv(): Ajv2020 {
  const ajv = new Ajv2020({ allErrors: true, strict: true });
  addFormats(ajv);
  ajv.addKeyword("example");
  return ajv;
}
