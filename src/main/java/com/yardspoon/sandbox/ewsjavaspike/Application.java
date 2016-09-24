package com.yardspoon.sandbox.ewsjavaspike;

import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import microsoft.exchange.webservices.data.core.enumeration.misc.TraceFlags;
import microsoft.exchange.webservices.data.credential.ExchangeCredentials;
import microsoft.exchange.webservices.data.credential.WebCredentials;
import microsoft.exchange.webservices.data.misc.ITraceListener;
import microsoft.exchange.webservices.data.property.complex.EmailAddress;

import java.io.FileInputStream;
import java.net.URI;
import java.nio.channels.Pipe;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Properties;

public class Application {
    public static void main(String[] args) throws Exception {
        System.out.println("Running!");

        Properties properties = new Properties();
        properties.load(new FileInputStream("ews.properties"));

        ExchangeService service = new ExchangeService();
        ExchangeCredentials credentials = new WebCredentials(properties.getProperty("username"), properties.getProperty("password"));
        service.setCredentials(credentials);
        service.setUrl(new URI(properties.getProperty("url")));
        service.setTraceEnabled(true);
        service.setTraceFlags(EnumSet.allOf(TraceFlags.class));
        service.setTraceListener((traceType, traceMessage) -> System.out.println("Type:" + traceType + " Message:" + traceMessage));

        Collection<EmailAddress> rooms = service.getRooms(new EmailAddress(properties.getProperty("rooms_address")));

        System.out.println("Found rooms:");
        for (EmailAddress room : rooms) {
            System.out.println(" - " + room.getAddress());
        }

        System.out.println("Stopped.");
    }
}
