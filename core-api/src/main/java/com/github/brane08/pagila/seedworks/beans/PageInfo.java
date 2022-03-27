package com.github.brane08.pagila.seedworks.beans;

public record PageInfo(int page, int size, String order) {

    public int offset() {
        return (page - 1) * size;
    }
}
