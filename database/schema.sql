-- Prerequisites (run once after base Sakila schema is loaded)
CREATE EXTENSION IF NOT EXISTS vector;

CREATE SCHEMA IF NOT EXISTS langgraph;

-- LangGraph checkpoint tables are created automatically by AsyncPostgresSaver.setup()
-- at genai-assistant startup using search_path=langgraph.

-- Patch 1: rating_txt column for Ebean ORM (cannot natively handle mpaa_rating enum)
alter table public.film
    add if not exists rating_txt varchar(10) default '';

update public.film
set rating_txt=rating::varchar;

drop view if exists public.film_list;

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

drop view if exists public.nicer_but_slower_film_list;

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

ALTER table public.language alter column name type varchar(20);

-- Store views for ORM-based queries (replaces raw SQL in StoreRepository)

CREATE OR REPLACE VIEW public.store_view AS
SELECT s.store_id,
       st.first_name || ' ' || st.last_name AS manager,
       a.address,
       a.district,
       c.city
FROM public.store s
         JOIN public.staff st ON s.manager_staff_id = st.staff_id
         JOIN public.address a ON s.address_id = a.address_id
         JOIN public.city c ON a.city_id = c.city_id;

CREATE OR REPLACE VIEW public.store_inventory_view AS
SELECT i.store_id,
       f.film_id,
       f.title,
       c.name                                                                          AS category,
       f.rating_txt                                                                    AS rating,
       f.rental_rate,
       COUNT(i.inventory_id)::int                                                      AS total_copies,
       SUM(CASE WHEN r.rental_id IS NULL OR r.return_date IS NOT NULL THEN 1 ELSE 0 END)::int AS available_copies
FROM public.film f
         JOIN public.film_category fc ON f.film_id = fc.film_id
         JOIN public.category c ON fc.category_id = c.category_id
         JOIN public.inventory i ON f.film_id = i.film_id
         LEFT JOIN public.rental r ON i.inventory_id = r.inventory_id AND r.return_date IS NULL
GROUP BY i.store_id, f.film_id, f.title, c.name, f.rating_txt, f.rental_rate;

CREATE OR REPLACE VIEW public.store_rental_view AS
SELECT i.store_id,
       r.rental_id,
       f.title,
       cu.first_name || ' ' || cu.last_name            AS customer,
       TO_CHAR(r.rental_date, 'YYYY-MM-DD')            AS rental_date,
       COALESCE(TO_CHAR(r.return_date, 'YYYY-MM-DD'), '—') AS return_date,
       (r.return_date IS NULL)                          AS outstanding
FROM public.rental r
         JOIN public.inventory i ON r.inventory_id = i.inventory_id
         JOIN public.film f ON i.film_id = f.film_id
         JOIN public.customer cu ON r.customer_id = cu.customer_id;

CREATE OR REPLACE VIEW public.store_customer_view AS
SELECT i.store_id,
       cu.customer_id,
       cu.first_name || ' ' || cu.last_name AS customer,
       cu.email,
       COUNT(r.rental_id)::int              AS rental_count,
       COALESCE(SUM(p.amount), 0)           AS total_spent
FROM public.customer cu
         JOIN public.rental r ON cu.customer_id = r.customer_id
         JOIN public.inventory i ON r.inventory_id = i.inventory_id
         LEFT JOIN public.payment p ON r.rental_id = p.rental_id
GROUP BY i.store_id, cu.customer_id, cu.first_name, cu.last_name, cu.email;

-- Film facet views for ORM-based queries (replaces raw SQL in FilmsRepository)

CREATE OR REPLACE VIEW public.film_category_facet AS
SELECT category AS key, COUNT(category)::int AS value
FROM public.film_list
GROUP BY category;

CREATE OR REPLACE VIEW public.film_rating_facet AS
SELECT rating_txt AS key, COUNT(rating_txt)::int AS value
FROM public.film_list
GROUP BY rating_txt;

CREATE OR REPLACE VIEW public.film_price_facet AS
SELECT 1                                                                          AS id,
       SUM(CASE WHEN price BETWEEN 0 AND 1 THEN 1 ELSE 0 END)::int               AS price_0_1,
       SUM(CASE WHEN price BETWEEN 1 AND 3 THEN 1 ELSE 0 END)::int               AS price_1_3,
       SUM(CASE WHEN price BETWEEN 3 AND 10 THEN 1 ELSE 0 END)::int              AS price_3_9
FROM public.film_list;