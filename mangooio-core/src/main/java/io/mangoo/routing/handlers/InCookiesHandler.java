package io.mangoo.routing.handlers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Splitter;

import io.mangoo.authentication.Authentication;
import io.mangoo.routing.RequestAttachment;
import io.mangoo.routing.bindings.Flash;
import io.mangoo.routing.bindings.Session;
import io.mangoo.utils.CookieParser;
import io.mangoo.utils.RequestUtils;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;

/**
 *
 * @author svenkubiak
 *
 */
public class InCookiesHandler implements HttpHandler {
    private static final int TOKEN_LENGTH = 16;
    private RequestAttachment requestAttachment;

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        this.requestAttachment = exchange.getAttachment(RequestUtils.REQUEST_ATTACHMENT);
        this.requestAttachment.setSession(getSessionCookie(exchange));
        this.requestAttachment.setAuthentication(getAuthenticationCookie(exchange));
        this.requestAttachment.setFlash(getFlashCookie(exchange));

        exchange.putAttachment(RequestUtils.REQUEST_ATTACHMENT, this.requestAttachment);
        nextHandler(exchange);
    }

    /**
     * Retrieves the current session from the HttpServerExchange
     *
     * @param exchange The Undertow HttpServerExchange
     */
    private Session getSessionCookie(HttpServerExchange exchange) {
        Session session = null;

        final CookieParser cookieParser = CookieParser
                .create(exchange,
                        this.requestAttachment.getConfig().getSessionCookieName(),
                        this.requestAttachment.getConfig().getApplicationSecret(),
                        this.requestAttachment.getConfig().isSessionCookieEncrypt());

        if (cookieParser.hasValidSessionCookie()) {
            session = new Session(cookieParser.getSessionValues(),
                    cookieParser.getAuthenticityToken(),
                    cookieParser.getExpiresDate());
        } else {
            session = new Session(new HashMap<>(),
                    RandomStringUtils.randomAlphanumeric(TOKEN_LENGTH),
                    LocalDateTime.now().plusSeconds(this.requestAttachment.getConfig().getSessionExpires()));
        }


        return session;
    }

    /**
     * Retrieves the current authentication from the HttpServerExchange
     *
     * @param exchange The Undertow HttpServerExchange
     */
    private Authentication getAuthenticationCookie(HttpServerExchange exchange) {
        Authentication authentication = null;

        final CookieParser cookieParser = CookieParser
                .create(exchange,
                        this.requestAttachment.getConfig().getAuthenticationCookieName(),
                        this.requestAttachment.getConfig().getApplicationSecret(), this.requestAttachment.getConfig().isAuthenticationCookieEncrypt());

        if (cookieParser.hasValidAuthenticationCookie()) {
            authentication = new Authentication(cookieParser.getExpiresDate(), cookieParser.getAuthenticatedUser());
        } else {
            authentication = new Authentication(LocalDateTime.now().plusSeconds(this.requestAttachment.getConfig().getAuthenticationExpires()), null);
        }

        return authentication;
    }

    /**
     * Retrieves the flash cookie from the current
     *
     * @param exchange The Undertow HttpServerExchange
     */
    private Flash getFlashCookie(HttpServerExchange exchange) {
        Flash flash = null;
        final Cookie cookie = exchange.getRequestCookies().get(this.requestAttachment.getConfig().getFlashCookieName());
        if (cookie != null){
            final String cookieValue = cookie.getValue();
            if (StringUtils.isNotEmpty(cookieValue) && !("null").equals(cookieValue)) {
                final Map<String, String> values = new HashMap<>();
                for (final Map.Entry<String, String> entry : Splitter.on("&").withKeyValueSeparator(":").split(cookie.getValue()).entrySet()) {
                    values.put(entry.getKey(), entry.getValue());
                }

                flash = new Flash(values);
                flash.setDiscard(true);
            }
        }

        if (flash == null) {
            flash = new Flash();
        }

        return flash;
    }

    /**
     * Handles the next request in the handler chain
     *
     * @param exchange The HttpServerExchange
     * @throws Exception Thrown when an exception occurs
     */
    @SuppressWarnings("all")
    private void nextHandler(HttpServerExchange exchange) throws Exception {
        new FormHandler().handleRequest(exchange);
    }
}