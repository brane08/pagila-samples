package com.github.brane08.pagila.seedworks.app;

import de.agilecoders.wicket.core.markup.html.bootstrap.table.TableBehavior;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.table.toolbars.BootstrapHeadersToolbar;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.table.toolbars.BootstrapNavigationToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;

import java.io.Serial;
import java.util.List;

public class BootstrapDataTable<T, S> extends DataTable<T, S> {

    @Serial
    private static final long serialVersionUID = 1L;

    private final TableBehavior tableBehavior;

    public BootstrapDataTable(String id, List<? extends IColumn<T, S>> iColumns, ISortableDataProvider<T, S> dataProvider,
                              long rowsPerPage) {
        super(id, iColumns, dataProvider, rowsPerPage);
        add(tableBehavior = new TableBehavior());
        addTopToolbar(new BootstrapHeadersToolbar<>(this, dataProvider));
        addBottomToolbar(new BootstrapNavigationToolbar(this));
    }

    public BootstrapDataTable<T, S> striped() {
        tableBehavior.striped();

        return this;
    }

    /**
     * adds the "sm" style to table
     *
     * @return this instance for chaining
     */
    public BootstrapDataTable<T, S> sm() {
        tableBehavior.sm();

        return this;
    }

    /**
     * adds the "bordered" style to table
     *
     * @return this instance for chaining
     */
    public BootstrapDataTable<T, S> bordered() {
        tableBehavior.bordered();

        return this;
    }

    /**
     * adds the "hover" flag to table
     *
     * @return this instance for chaining
     */
    public BootstrapDataTable<T, S> hover() {
        tableBehavior.hover();

        return this;
    }

    /**
     * adds the "dark" flag to table
     *
     * @return this instance for chaining
     */
    public BootstrapDataTable<T, S> dark() {
        tableBehavior.dark();

        return this;
    }

    public BootstrapDataTable<T, S> light() {
        tableBehavior.light();

        return this;
    }
}
