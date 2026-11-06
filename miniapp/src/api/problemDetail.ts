import type { ProblemDetail } from "./types";

export class ApiError extends Error {
  readonly problem: ProblemDetail;
  readonly status: number;

  constructor(problem: ProblemDetail, status: number) {
    super(problem.detail ?? problem.title ?? `Request failed with status ${status}`);
    this.problem = problem;
    this.status = status;
  }
}

/** Never shows raw JSON/stack traces — always parses into the ProblemDetail shape. */
export async function parseProblemDetail(response: Response): Promise<ProblemDetail> {
  try {
    const body: unknown = await response.json();
    if (body && typeof body === "object") {
      return body as ProblemDetail;
    }
  } catch {
    // body wasn't JSON — fall through to the generic shape below.
  }
  return { status: response.status, title: response.statusText };
}
