package com.github.brane08.pagila.film.beans;


import java.io.Serializable;

public record CategoryInfo(Long categoryId, String name) implements Serializable {
}
