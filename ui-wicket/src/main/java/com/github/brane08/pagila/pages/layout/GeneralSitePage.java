package com.github.brane08.pagila.pages.layout;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome6CssReference;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebPage;

public class GeneralSitePage extends WebPage {

    public GeneralSitePage() {
        add(new HeaderPanel("headerPanel"));
        add(new FooterPanel("footerPanel"));
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(FontAwesome6CssReference.instance()));
    }
}
