package com.yardspoon.sandbox.ewsjavaspike;

import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import microsoft.exchange.webservices.data.core.enumeration.misc.TraceFlags;
import microsoft.exchange.webservices.data.credential.ExchangeCredentials;
import microsoft.exchange.webservices.data.credential.WebCredentials;
import microsoft.exchange.webservices.data.misc.ITraceListener;
import microsoft.exchange.webservices.data.property.complex.EmailAddress;
import okhttp3.*;

import java.io.FileInputStream;
import java.io.IOException;
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

        System.out.println("Found rooms via EWS Client:");
        for (EmailAddress room : rooms) {
            System.out.println(" - " + room.getName() + " - " + room.getAddress() + " - " + room.getSearchString());
        }

        System.out.println("------------------------------------------------------------------");

        MediaType XML = MediaType.parse("application/xml; charset=utf-8");

        OkHttpClient client = (new OkHttpClient.Builder()).authenticator(new Authenticator() {
            @Override
            public Request authenticate(Route route, Response response) throws IOException {
                return null;
            }
        }).build();

        RequestBody body = RequestBody.create(XML, capturedXMLSoapRequestForRooms);
        Request request = new Request.Builder()
                .url(properties.getProperty("url"))
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        String okhttpRoomsResponse = response.body().string();

        System.out.println("OKHttp response for rooms: ");
        System.out.println(response.code());
        System.out.println(okhttpRoomsResponse);

        System.out.println("Stopped.");
    }

    private static final String capturedXMLSoapRequestForRooms = "<?xml version=\"1.0\" encoding=\"utf-8\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:m=\"http://schemas.microsoft.com/exchange/services/2006/messages\" xmlns:t=\"http://schemas.microsoft.com/exchange/services/2006/types\"><soap:Header><t:RequestServerVersion Version=\"Exchange2010_SP2\"></t:RequestServerVersion></soap:Header><soap:Body><m:GetRooms><m:RoomList><t:EmailAddress>_USA-MO-ASYNCHRONY@wwt.com</t:EmailAddress></m:RoomList></m:GetRooms></soap:Body></soap:Envelope>";

    static class NTLMAuthenticator implements Authenticator {
        final NTLMEngineImpl engine = new NTLMEngineImpl();
        private final String domain;
        private final String username;
        private final String password;
        private final String ntlmMsg1;

        public NTLMAuthenticator(String username, String password, String domain) {
            this.domain = domain;
            this.username = username;
            this.password = password;
            String localNtlmMsg1 = null;
            try {
                localNtlmMsg1 = engine.generateType1Msg(null, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            ntlmMsg1 = localNtlmMsg1;
        }

        @Override
        public Request authenticate(Proxy proxy, Response response) throws IOException {
            final List<String> WWWAuthenticate = response.headers().values("WWW-Authenticate");
            if (WWWAuthenticate.contains("NTLM")) {
                return response.request().newBuilder().header("Authorization", "NTLM " + ntlmMsg1).build();
            }
            String ntlmMsg3 = null;
            try {
                ntlmMsg3 = engine.generateType3Msg(username, password, domain, "android-device", WWWAuthenticate.get(0).substring(5));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return response.request().newBuilder().header("Authorization", "NTLM " + ntlmMsg3).build();
        }

        @Override
        public Request authenticateProxy(Proxy proxy, Response response) throws IOException {
            return null;
        }
    }
}


