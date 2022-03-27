alter table public.film
    add rating_txt varchar(10) default '';

update public.film
set rating_txt=rating::varchar;

drop view public.film_list;

CREATE VIEW public.film_list AS
SELECT film.film_id                                                              AS fid,
       film.title,
       film.description,
       category.name                                                             AS category,
       film.rental_rate                                                          AS price,
       film.length,
       film.rating,
       film.rating_txt,
       public.group_concat(((actor.first_name || ' '::text) || actor.last_name)) AS actors
FROM ((((public.category
    LEFT JOIN public.film_category ON ((category.category_id = film_category.category_id)))
    LEFT JOIN public.film ON ((film_category.film_id = film.film_id)))
    JOIN public.film_actor ON ((film.film_id = film_actor.film_id)))
    JOIN public.actor ON ((film_actor.actor_id = actor.actor_id)))
GROUP BY film.film_id, film.title, film.description, category.name, film.rental_rate, film.length, film.rating;


ALTER TABLE public.film_list
    OWNER TO postgres;

drop view public.nicer_but_slower_film_list;

CREATE VIEW public.nicer_but_slower_film_list AS
SELECT film.film_id                                                                                                 AS fid,
       film.title,
       film.description,
       category.name                                                                                                AS category,
       film.rental_rate                                                                                             AS price,
       film.length,
       film.rating,
       film.rating_txt,
       public.group_concat((((upper("substring"(actor.first_name, 1, 1)) || lower("substring"(actor.first_name, 2))) ||
                             upper("substring"(actor.last_name, 1, 1))) ||
                            lower("substring"(actor.last_name, 2))))                                                AS actors
FROM ((((public.category
    LEFT JOIN public.film_category ON ((category.category_id = film_category.category_id)))
    LEFT JOIN public.film ON ((film_category.film_id = film.film_id)))
    JOIN public.film_actor ON ((film.film_id = film_actor.film_id)))
    JOIN public.actor ON ((film_actor.actor_id = actor.actor_id)))
GROUP BY film.film_id, film.title, film.description, category.name, film.rental_rate, film.length, film.rating;

ALTER TABLE public.nicer_but_slower_film_list
    OWNER TO postgres;
