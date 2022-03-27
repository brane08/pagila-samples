import {Actor} from "./actor.model";

export interface Film {
  releaseYear: number;
  language: Language;
  categories: Category[];
  lastUpdate: Date;
  actors: Actor[];
}

export interface Language {
  name: string;
}

export interface Category {
  name: string;
}
