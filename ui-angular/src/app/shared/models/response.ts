export interface ApiResult<T> {
  success: boolean;
  data: T;
}

export interface PageResult<T> extends ApiResult<Array<T>> {
  totalCount: number;
}
