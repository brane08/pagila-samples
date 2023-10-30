package com.github.brane08.pagila;

import jakarta.servlet.DispatcherType;
import org.apache.wicket.protocol.http.WicketFilter;
import org.eclipse.jetty.ee10.cdi.CdiDecoratingListener;
import org.eclipse.jetty.ee10.cdi.CdiServletContainerInitializer;
import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.glassfish.jersey.servlet.ServletContainer;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public class JettyMain {

    static {
        // Wire up java.util.logging (used by weld) to slf4j.
        org.slf4j.bridge.SLF4JBridgeHandler.removeHandlersForRootLogger();
        org.slf4j.bridge.SLF4JBridgeHandler.install();
    }

    private final int port;
    private final Server server;

    public JettyMain(int port) {
        this.port = port;
        this.server = new Server(port);
    }

    public void configureServer() throws Exception {
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        ResourceFactory resourceFactory = ResourceFactory.of(context);
        Resource manifestResources = resourceFactory.newResource(findResources(getClass().getClassLoader()));
        context.setBaseResource(manifestResources);
        context.setWelcomeFiles(new String[]{"index.html"});
        server.setDefaultHandler(new DefaultHandler());

        context.setInitParameter(CdiServletContainerInitializer.CDI_INTEGRATION_ATTRIBUTE, CdiDecoratingListener.MODE);
        context.addServletContainerInitializer(new CdiServletContainerInitializer());
        context.addServletContainerInitializer(new org.jboss.weld.environment.servlet.EnhancedListener());

        FilterHolder filterHolder = context.addFilter(WicketFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
        filterHolder.setInitParameter("applicationClassName", WicketApplication.class.getName());
        filterHolder.setInitParameter("filterMappingUrlPattern", "/*");

        ServletHolder servletHolder = context.addServlet(ServletContainer.class, "/rest/*");
        servletHolder.setInitOrder(1);
        servletHolder.setInitParameter("jersey.config.server.provider.packages", "com.github.brane08.pagila.rest");

        ServletHolder staticHolder = new ServletHolder("default", DefaultServlet.class);
        context.addServlet(staticHolder, "/");

        JakartaWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) -> {
//            wsContainer.addEndpoint();
        });

        server.setHandler(context);

        server.start();
        server.join();
    }

    private List<URI> findResources(ClassLoader classLoader) throws IOException {
        return Collections.list(classLoader.getResources("webroot"))
                .stream()
                .map(JettyMain::toURI)
                .filter(Objects::nonNull)
                .toList();
    }

    protected static URI toURI(URL url) {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        try {
            new JettyMain(8001).configureServer();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
