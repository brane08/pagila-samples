export type BaseType = string | boolean | number

export interface Page {
  num: number;
  size: number;
  sort?: string[];
}

export interface Selectable {
  selected?: boolean;
}

export type BasicType = string | number | boolean | object;

export type ColDef = {
  type: string,
  header: string,
  name: string,
  sort?: string
};


export function defaultPage(): Page {
  return {num: 1, size: 10, sort: undefined}
}
