import type pino from "pino";

/** An in-memory pino destination for tests: collects each log line as parsed JSON. */
export class MemoryDestination implements pino.DestinationStream {
  readonly lines: string[] = [];

  write(msg: string): void {
    this.lines.push(msg);
  }

  get entries(): Array<Record<string, unknown>> {
    return this.lines.map((line) => JSON.parse(line) as Record<string, unknown>);
  }
}
