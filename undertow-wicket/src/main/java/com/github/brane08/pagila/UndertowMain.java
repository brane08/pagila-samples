package com.github.brane08.pagila;

import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.servlet.api.DeploymentInfo;
import jakarta.servlet.DispatcherType;
import org.apache.wicket.protocol.http.WicketFilter;
import org.jboss.resteasy.core.ResteasyDeploymentImpl;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.weld.environment.servlet.Listener;

import static io.undertow.servlet.Servlets.filter;
import static io.undertow.servlet.Servlets.listener;

public class UndertowMain {
    public static final String WICKET_APP = WicketApplication.class.getName();


    public static void main(final String[] args) {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        jaxrsServer();
    }

    static void jaxrsServer() {
        UndertowJaxrsServer server = new UndertowJaxrsServer();

        ResteasyDeployment deployment = new ResteasyDeploymentImpl();
        deployment.setApplicationClass(ResteasyApplication.class.getName());
        deployment.setInjectorFactoryClass("org.jboss.resteasy.cdi.CdiInjectorFactory");

        DeploymentInfo deploymentInfo = server.undertowDeployment(deployment, "/rest");
        deploymentInfo.setClassLoader(UndertowMain.class.getClassLoader());
        deploymentInfo.setDeploymentName("Minimal Undertow RESTeasy and Weld CDI Setup");
        deploymentInfo.setContextPath("/");
        deploymentInfo.addListener(listener(Listener.class));
        deploymentInfo.setResourceManager(new ClassPathResourceManager(UndertowMain.class.getClassLoader()));
        deploymentInfo.setDeploymentName("wicket.war");
        deploymentInfo.addFilter(filter("WicketFilter", WicketFilter.class)
                .addInitParam("applicationClassName", WICKET_APP)
                .addInitParam("filterMappingUrlPattern", "/*"));
        deploymentInfo.addFilterUrlMapping("WicketFilter", "/*", DispatcherType.REQUEST);

        server.deploy(deploymentInfo);

        Undertow.Builder builder = Undertow.builder()
                .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                .addHttpListener(8001, "localhost");
        server.start(builder);
    }
}
