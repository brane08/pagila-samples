import {Actor} from "./actor.model";
import {Selectable} from "./common.model";

export interface Film extends Selectable {
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
