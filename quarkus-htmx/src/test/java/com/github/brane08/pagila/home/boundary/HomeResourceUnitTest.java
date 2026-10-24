package com.github.brane08.pagila.home.boundary;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeResourceUnitTest {

    @Mock
    Template template;
    @Mock
    TemplateInstance instance;

    @Test
    void homeReturnsInstanceFromInjectedTemplate() {
        HomeResource resource = new HomeResource();
        // Field is package-private and set by CDI in production; set it directly here.
        resource.home = template;
        when(template.instance()).thenReturn(instance);

        assertSame(instance, resource.home());
        verify(template).instance();
    }
}
