package com.github.brane08.pagila.pages.layout;

import com.github.brane08.pagila.pages.film.FilmListPage;
import com.github.brane08.pagila.pages.home.HomePage;
import org.apache.wicket.markup.html.panel.Panel;
import org.wicketstuff.lambda.components.ComponentFactory;

import java.io.Serial;

public class HeaderPanel extends Panel {

    @Serial
    private static final long serialVersionUID = 1L;

    public HeaderPanel(String id) {
        super(id);
        add(ComponentFactory.link("appHome", (newlink) -> {
            setResponsePage(HomePage.class);
        }));
        add(ComponentFactory.link("appFilms", (newlink) -> {
            setResponsePage(FilmListPage.class);
        }));
        add(ComponentFactory.link("appActors", (newlink) -> {
            setResponsePage(FilmListPage.class);
        }));
    }
}
