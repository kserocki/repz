/**
 * Compares two strings by UTF-16 code unit, the same ordering `Array.prototype.sort` uses by
 * default. Exported (rather than relying on that default) so every sort that must be
 * deterministic across machines says so explicitly: `String.prototype.localeCompare` depends on
 * the ICU/locale available at runtime and must never be used where reproducibility matters (the
 * exercise catalog import, the seeder's content diff).
 */
export function compareCodePoints(a: string, b: string): number {
  if (a < b) {
    return -1;
  }
  if (a > b) {
    return 1;
  }
  return 0;
}
