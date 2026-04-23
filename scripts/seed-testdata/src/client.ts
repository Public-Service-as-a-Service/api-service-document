export type HttpError = Error & { status: number; url: string; body: string };

export type ApiClient = {
  get<T>(path: string): Promise<T>;
  postJson<T>(path: string, body: unknown): Promise<{ status: number; location?: string; body: T | undefined }>;
  postMultipartVoid(path: string, parts: MultipartPart[]): Promise<{ status: number; location?: string }>;
  putMultipartVoid(path: string, parts: MultipartPart[]): Promise<{ status: number }>;
};

export type MultipartPart = {
  name: string;
  filename: string;
  contentType: string;
  body: Uint8Array | string;
};

export function createClient(baseUrl: string): ApiClient {
  async function request<T>(method: string, path: string, init: RequestInit = {}): Promise<Response> {
    const url = `${baseUrl}${path}`;
    const response = await fetch(url, { method, ...init });
    if (!response.ok) {
      const body = await response.text().catch(() => "<unreadable>");
      const err = new Error(`${method} ${url} → ${response.status} ${response.statusText}: ${truncate(body, 500)}`) as HttpError;
      err.status = response.status;
      err.url = url;
      err.body = body;
      throw err;
    }
    return response;
  }

  function toFormData(parts: MultipartPart[]): FormData {
    const form = new FormData();
    for (const part of parts) {
      // Cast through BlobPart — Node's Uint8Array is typed as Uint8Array<ArrayBufferLike>,
      // but Blob's constructor wants ArrayBufferView<ArrayBuffer>. The cast is safe here
      // because we only ever produce Uint8Arrays backed by regular ArrayBuffers.
      const blob = new Blob([part.body as BlobPart], { type: part.contentType });
      form.append(part.name, blob, part.filename);
    }
    return form;
  }

  return {
    async get<T>(path: string): Promise<T> {
      const res = await request("GET", path, { headers: { Accept: "application/json" } });
      return (await res.json()) as T;
    },

    async postJson<T>(path: string, body: unknown) {
      const res = await request("POST", path, {
        headers: { Accept: "application/json", "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });
      const location = res.headers.get("location") ?? undefined;
      const text = await res.text();
      return {
        status: res.status,
        location,
        body: text.length > 0 ? (JSON.parse(text) as T) : undefined,
      };
    },

    async postMultipartVoid(path: string, parts: MultipartPart[]) {
      const res = await request("POST", path, { body: toFormData(parts) });
      return { status: res.status, location: res.headers.get("location") ?? undefined };
    },

    async putMultipartVoid(path: string, parts: MultipartPart[]) {
      const res = await request("PUT", path, { body: toFormData(parts) });
      return { status: res.status };
    },
  };
}

function truncate(value: string, max: number): string {
  return value.length <= max ? value : `${value.slice(0, max)}…`;
}
