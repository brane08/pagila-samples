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
