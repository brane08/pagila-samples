import {BaseType, defaultPage, Page} from "./common.model";
import {HttpParams} from "@angular/common/http";

export interface PageRequest {
  current: Page;
  filters?: Record<string, BaseType>;
}

export function defaultPageRequest(): PageRequest {
  return {current: defaultPage(), filters: {}};
}

export function toHttpParams(request: PageRequest): HttpParams {
  let num = (request.current.num > 0) ? request.current.num - 1 : request.current.num;
  let params = new HttpParams().set("size", request.current.size).set("number", num);
  if (request.current.sort) {
    params = params.set("sort", request.current.sort.join(","));
  }
  if (request.filters) {
    for (let key of Object.keys(request.filters)) {
      params = params.set(key, request.filters[key]);
    }
  }
  return params;
}
