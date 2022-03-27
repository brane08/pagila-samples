import {defaultPage, Page} from "./common.model";

export interface ApiResponse<T> {
  success: boolean;
  data: T;
}

export interface PageResponse<T> extends ApiResponse<Array<T>> {
  total: number;
  current: Page;
  empty?: boolean;
}


export function emptyPageResponse(): PageResponse<object> {
  return {
    current: defaultPage(), data: [], success: true, total: 0, empty: true
  };
}
