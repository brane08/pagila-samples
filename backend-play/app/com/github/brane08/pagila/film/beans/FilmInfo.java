package com.github.brane08.pagila.film.beans;


import com.github.brane08.pagila.actor.beans.ActorInfo;

import java.util.List;
import java.util.Set;

public class FilmInfo {

	public Long filmId;
	public String title;
	public String description;
	public Short releaseYear;
	public String language;
	public String originalLanguage;
	public Long rentalDuration;
	public Double rentalRate;
	public Long length;
	public Double replacementCost;
	public String rating;
	public java.time.Instant lastUpdate;
	public Set<String> specialFeatures;
	public List<String> categories;
	public List<ActorInfo> actors;
}
