package io.mangoo.managers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketClient;
import org.eclipse.jetty.websocket.WebSocketClientFactory;
import org.junit.Test;
import org.mockito.Mockito;

import io.mangoo.configuration.Config;
import io.mangoo.core.Application;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;

/**
 *
 * @author svenkubiak
 *
 */
public class WebSocketManagerTest {
    private static String eventData;
    private static final String COOKIE_NAME = "TEST-AUTH";
    private static final String VALID_COOKIE_VALUE = "3372c6783fa8d223c700e9903b4e8037db710b4b60ee2ca129465fa0a12e0a0b1860019962ae04e4b329e4da03ce09eb347c97b5598085cc8530213b9b82f91f|2999-11-11T11:11:11.111|0#mangooio";
    private static final String INVALID_COOKIE_VALUE = "3372c6783fa8d223c700e9903b4e8037db710b4b60ee2ca129465fa0a12e0a0b1860019962ae04e4b329e4da03ce09eb347c97b5598085cc8530213b9b82f91f|2999-11-11T11:11:11.111|0#mangooiO";

    @Test
    public void testAddChannel() {
        //given
        final WebSocketManager webSocketManager = Application.getInstance(WebSocketManager.class);
        final WebSocketChannel channel = Mockito.mock(WebSocketChannel.class);
        when(channel.getUrl()).thenReturn("/websocket");

        //when
        webSocketManager.removeChannels("/websocket");
        webSocketManager.addChannel(channel);

        //then
        assertThat(webSocketManager.getChannels("/websocket"), not(nullValue()));
        assertThat(webSocketManager.getChannels("/websocket").size(), equalTo(1));
    }

    @Test
    public void testRemoveChannel() {
        //given
        final WebSocketManager webSocketManager = Application.getInstance(WebSocketManager.class);
        final WebSocketChannel channel = Mockito.mock(WebSocketChannel.class);
        when(channel.getUrl()).thenReturn("/websocket");

        //when
        webSocketManager.addChannel(channel);
        webSocketManager.removeChannels("/websocket");

        //then
        assertThat(webSocketManager.getChannels("/websocket"), not(nullValue()));
        assertThat(webSocketManager.getChannels("/websocket").size(), equalTo(0));
    }

    @Test
    public void testCloseChannel() throws Exception {
        //given
        final Config config = Application.getInstance(Config.class);
        final WebSocketManager webSocketManager = Application.getInstance(WebSocketManager.class);
        webSocketManager.removeChannels("/websocket");
        final WebSocketClientFactory factory = new WebSocketClientFactory();
        factory.start();
        final String url = "ws://" + config.getConnectorHttpHost() + ":" + config.getConnectorHttpPort() + "/websocket";

        //when
        final WebSocketClient client = new WebSocketClient(factory);
        client.open(new URI(url), new WebSocket.OnTextMessage() {
            @Override
            public void onOpen(Connection connection) {
                // intentionally left blank
            }

            @Override
            public void onClose(int closeCode, String message) {
                // intentionally left blank
            }

            @Override
            public void onMessage(String data) {
                // intentionally left blank
            }
        }).get(5, TimeUnit.SECONDS);

        webSocketManager.close("/websocket");

        //then
        assertThat(webSocketManager.getChannels("/websocket"), not(nullValue()));
        assertThat(webSocketManager.getChannels("/websocket").size(), equalTo(0));
    }

    @Test
    public void testSendData() throws Exception {
        //given
        final Config config = Application.getInstance(Config.class);
        final WebSocketManager webSocketManager = Application.getInstance(WebSocketManager.class);
        webSocketManager.removeChannels("/websocket");
        final WebSocketClientFactory factory = new WebSocketClientFactory();
        factory.start();
        final String url = "ws://" + config.getConnectorHttpHost() + ":" + config.getConnectorHttpPort() + "/websocket";
        final String data = "Server sent data FTW!";
        eventData = null;

        //when
        new WebSocketClient(factory).open(new URI(url), new WebSocket.OnTextMessage() {
            @Override
            public void onOpen(Connection connection) {
                // intentionally left blank
            }

            @Override
            public void onClose(int closeCode, String message) {
                // intentionally left blank
            }

            @Override
            public void onMessage(String data) {
                eventData = data;
            }
        }).get(5, TimeUnit.SECONDS);
        Thread.sleep(500);
        webSocketManager.getChannels("/websocket").forEach(channel -> {
            try {
                if (channel.isOpen()) {
                    WebSockets.sendTextBlocking(data, channel);
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
         });
        Thread.sleep(500);

        //then
        assertThat(eventData, not(nullValue()));
        assertThat(eventData, equalTo(data));
    }

    @Test
    public void testSendDataWithValidAuthentication() throws Exception {
        //given
        final WebSocketManager webSocketManager = Application.getInstance(WebSocketManager.class);
        final Config config = Application.getInstance(Config.class);
        webSocketManager.removeChannels("/websocketauth");
        final WebSocketClientFactory factory = new WebSocketClientFactory();
        factory.start();
        final String url = "ws://" + config.getConnectorHttpHost() + ":" + config.getConnectorHttpPort() + "/websocketauth";
        final String data = "Server sent data with authentication FTW!";
        eventData = null;

        //when
        final WebSocketClient client = new WebSocketClient(factory);
        client.getCookies().put(COOKIE_NAME, VALID_COOKIE_VALUE);
        client.open(new URI(url), new WebSocket.OnTextMessage() {
            @Override
            public void onOpen(Connection connection) {
                // intentionally left blank
            }

            @Override
            public void onClose(int closeCode, String message) {
                // intentionally left blank
            }

            @Override
            public void onMessage(String data) {
                if (StringUtils.isBlank(eventData)) {
                    eventData = data;
                }
            }
        }).get(5, TimeUnit.SECONDS);
        Thread.sleep(500);
        webSocketManager.getChannels("/websocketauth").forEach(channel -> {
            try {
                if (channel.isOpen()) {
                    WebSockets.sendTextBlocking(data, channel);
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
         });
        Thread.sleep(500);

        //then
        assertThat(eventData, not(nullValue()));
        assertThat(eventData, equalTo(data));
    }

    @Test
    public void testSendDataWithInvalidAuthentication() throws Exception {
        //given
        final WebSocketManager webSocketManager = Application.getInstance(WebSocketManager.class);
        final Config config = Application.getInstance(Config.class);
        webSocketManager.removeChannels("/websocketauth");
        final WebSocketClientFactory factory = new WebSocketClientFactory();
        factory.start();
        final String url = "ws://" + config.getConnectorHttpHost() + ":" + config.getConnectorHttpPort() + "/websocketauth";
        final String data = "Server sent data with authentication FTW!";
        eventData = null;

        //when
        final WebSocketClient client = new WebSocketClient(factory);
        client.getCookies().put(COOKIE_NAME, INVALID_COOKIE_VALUE);
        client.open(new URI(url), new WebSocket.OnTextMessage() {
            @Override
            public void onOpen(Connection connection) {
                // intentionally left blank
            }

            @Override
            public void onClose(int closeCode, String message) {
                // intentionally left blank
            }

            @Override
            public void onMessage(String data) {
                if (StringUtils.isBlank(eventData)) {
                    eventData = data;
                }
            }
        }).get(5, TimeUnit.SECONDS);
        Thread.sleep(500);
        webSocketManager.getChannels(url).forEach(channel -> {
            try {
                if (channel.isOpen()) {
                    WebSockets.sendTextBlocking(data, channel);
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
         });
        Thread.sleep(500);

        //then
        assertThat(eventData, nullValue());
        assertThat(eventData, not(equalTo(data)));
    }
}