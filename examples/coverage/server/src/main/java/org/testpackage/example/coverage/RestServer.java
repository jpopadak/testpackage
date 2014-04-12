package org.testpackage.example.coverage;

import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.simple.container.SimpleServerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.io.IOException;

/**
 * @author rnorth
 */
public class RestServer {

    private static RestServerDummyService service;

    @Path("/testA")
    public static class TestA {
        @GET
        @Produces("text/html")
        public String getMessage() {
            return "A";
        }
    }

    @Path("/testB")
    public static class TestB {
        @GET
        @Produces("text/html")
        public String getMessage() {
            service.doSomethingElse();
            return "B" + service.doSomething();
        }
    }

    @Path("/testC")
    public static class TestC {
        @GET
        @Produces("text/html")
        public String getMessage() {
            return service.doSomething();
        }
    }


    public static void main(String[] args) throws IOException {

        service = new RestServerDummyService();
        ResourceConfig resourceConfig = new DefaultResourceConfig(TestA.class, TestB.class, TestC.class);
        SimpleServerFactory.create("http://localhost:5555", resourceConfig);
    }
}
