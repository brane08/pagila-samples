export interface Film {
  filmId: number;
  title: string;
  description: string;
  releaseYear: string;
  language: string;
  originalLanguage: string;
  rentalDuration: number;
  rentalRate: number;
  length: number;
  replacementCost: number;
  rating: string;
  lastUpdate: Date;
  specialFeatures: string[];
  categories: string[];
  actors: string;
}

export interface FilmView {
  filmId: number;
  title: string;
  description: string;
  category: string;
  language: string;
  price: number;
  length: number;
  rating: string;
  lastUpdate: Date;
  actors: string;
}

export interface NicerFilmView {
  fid: number;
  title: string;
  description: string;
  category: string;
  price: number;
  length: number;
  rating: string;
  actors: string;
}

export interface SalesByFilmCategory {
  category: string;
  totalSales: number;
}

export interface FilmDetail {
  filmId: number;
  title: string;
  description: string;
  releaseYear: number;
  language: string;
  originalLanguage: string | null;
  rentalDuration: number;
  rentalRate: number;
  length: number;
  replacementCost: number;
  rating: string;
  lastUpdate: string;
  specialFeatures: string[];
  categories: string[];
}

export interface ActorInfo {
  actorId: number;
  firstName: string;
  lastName: string;
}
