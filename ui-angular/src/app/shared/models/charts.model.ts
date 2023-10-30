export interface Stacked {
  label: string;
  data: number[];
  backgroundColor: string;
}

export function randomArray(count: number = 4, min: number = 17, max: number = 97): number[] {
  let arr = [];
  for (let i = 0; i < count; i++) {
    arr.push(Math.floor(Math.random() * (max - min) + min));
  }
  return arr;
}
