import { Ajv2020 } from "ajv/dist/2020.js";
import addFormats from "ajv-formats";

// ajv-formats is CJS with an ESM-style .d.ts: under nodenext the callable is .default.

/** One Ajv2020 instance per caller (validator state is not meant to be shared): strict mode,
 * every error collected, `ajv-formats`' formats registered, and the OpenAPI-only `example`
 * keyword allow-listed so a schema carrying it does not trip `strict: true`. */
export function createContractAjv(): Ajv2020 {
  const ajv = new Ajv2020({ allErrors: true, strict: true });
  addFormats.default(ajv);
  ajv.addKeyword("example");
  return ajv;
}
