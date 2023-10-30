package com.github.brane08.pagila.pages.film;

import com.github.brane08.pagila.film.beans.FilmViewInfo;
import com.github.brane08.pagila.pages.layout.GeneralSitePage;
import com.github.brane08.pagila.seedworks.app.BootstrapDataTable;
import jakarta.inject.Inject;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.Model;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

public class FilmListPage extends GeneralSitePage {

    @Serial
    private static final long serialVersionUID = 1L;

    @Inject
    FilmsController controller;

    public FilmListPage() {
        super();
        ISortableDataProvider<FilmViewInfo, String> listDataProvider = controller.viewInfoProvider();
        List<IColumn<FilmViewInfo, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<>(new Model<>("ID"), "filmId", "filmId"));
        columns.add(new PropertyColumn<>(new Model<>("Title"), "title", "title"));
        columns.add(new PropertyColumn<>(new Model<>("Description"), "description", "description"));
        columns.add(new PropertyColumn<>(new Model<>("Category"), "category", "category"));
        columns.add(new PropertyColumn<>(new Model<>("Price"), "price", "price"));
        columns.add(new PropertyColumn<>(new Model<>("Length"), "length", "length"));
        BootstrapDataTable<FilmViewInfo, String> dataTable = new BootstrapDataTable<>("filmsDataTable", columns, listDataProvider, 20);
        dataTable.bordered().striped().hover();
        add(dataTable);
    }
}
