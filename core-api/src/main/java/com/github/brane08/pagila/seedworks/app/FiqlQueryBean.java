package com.github.brane08.pagila.seedworks.app;

import com.github.brane08.pagila.seedworks.beans.PageInfo;

public class FiqlQueryBean {

    static final int MAX_SIZE = 500;

    public int page;
    public int size;
    public String sort;
    public String qry;
    public int direction;

    public static FiqlQueryBean build(String qry, int page, int size) {
        return build(qry, page, size, "id", 1);
    }

    public static FiqlQueryBean build(String qry, int page, int size, String sort) {
        return build(qry, page, size, sort, 1);
    }

    public static FiqlQueryBean build(String qry, int page, int size, String sort, int direction) {
        FiqlQueryBean bean = new FiqlQueryBean();
        bean.qry = qry;
        bean.page = page;
        bean.size = size;
        bean.direction = direction;
        bean.sort = sort;
        return bean;
    }

    public PageInfo pageInfo() {
        return new PageInfo(Math.max(page, 1), Math.min(Math.max(size, 1), MAX_SIZE), sort);
    }
}
