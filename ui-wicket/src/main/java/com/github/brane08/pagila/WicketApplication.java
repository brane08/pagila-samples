package com.github.brane08.pagila;

import com.github.brane08.pagila.pages.film.FilmListPage;
import com.github.brane08.pagila.pages.home.HomePage;
import de.agilecoders.wicket.core.Bootstrap;
import org.apache.wicket.cdi.CdiConfiguration;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WicketApplication extends WebApplication {

    private static final Logger LOG = LoggerFactory.getLogger(WicketApplication.class);

    @Override
    protected void init() {
        super.init();
        new CdiConfiguration().configure(this);

        getCspSettings().blocking().disabled();
        mountPackage("films", FilmListPage.class);
        Bootstrap.install(this);
    }

    @Override
    public Class<? extends WebPage> getHomePage() {
        return HomePage.class;
    }
}
