/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.accounts.common;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Base64;

import javax.enterprise.context.ApplicationScoped;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.ServletContext;

/**
 * Converts an username/password into a token.
 *
 * @author Juraci Paixão Kröhling
 */
@ApplicationScoped
public class UsernamePasswordConverter {
    private String clientId;
    private String secret;
    private String baseUrl;
    private String realm;
    private String tokenUrl;

    public String getTokenFromUsernameAndPassword(ServletContext servletContext, String username, String password)
    throws UsernamePasswordConversionException, Exception {
        if (baseUrl == null || baseUrl.isEmpty()) {
            loadConfigurationFile(servletContext);
        }

        if (username == null || username.isEmpty()) {
            throw new UsernamePasswordConversionException("Username is not provided.");
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(tokenUrl).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setDoInput(true);
        connection.setDoOutput(true);

        String credentials = clientId + ":" + secret;
        String authorizationHeader = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
        connection.setRequestProperty("Authorization", authorizationHeader);

        String urlParameters = "grant_type=password&username=" + URLEncoder.encode(username, "UTF-8");
        urlParameters += "&password=" + URLEncoder.encode(password, "UTF-8");

        try (PrintWriter out = new PrintWriter(connection.getOutputStream())) {
            out.print(urlParameters);
        }

        StringBuilder response = new StringBuilder();

        int statusCode;
        try {
            statusCode = connection.getResponseCode();
        } catch (SocketTimeoutException timeoutException) {
            throw new UsernamePasswordConversionException("Timed out when trying to contact the Keycloak server.");
        }

        InputStream inputStream;
        if (statusCode < 300) {
            inputStream = connection.getInputStream();
        } else {
            inputStream = connection.getErrorStream();
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
            for (String line; (line = reader.readLine()) != null;) {
                response.append(line);
            }
            inputStream.close();
        }

        String sResponse = response.toString();
        JsonReader jsonReader = Json.createReader(new StringReader(sResponse));
        JsonObject object = jsonReader.readObject();
        if (object.get("error") != null) {
            String error = object.getString("error");
            throw new UsernamePasswordConversionException("Error from Keycloak server: " + error);
        }

        return object.getString("refresh_token");
    }

    private void loadConfigurationFile(ServletContext servletContext) throws Exception {
        String keycloakAdapterConfig = servletContext.getInitParameter("org.keycloak.json.adapterConfig");
        JsonReader jsonReader = Json.createReader(new StringReader(keycloakAdapterConfig));
        JsonObject object = jsonReader.readObject();
        JsonObject credentials = object.getJsonObject("credentials");

        baseUrl = object.getString("auth-server-url-for-backend-requests");
        realm = object.getString("realm");
        clientId = object.getString("resource");
        secret = credentials.getString("secret");
        tokenUrl = baseUrl + "/realms/" + URLEncoder.encode(realm, "UTF-8") + "/protocol/openid-connect/token";

        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = object.getString("auth-server-url");
        }
        jsonReader.close();

        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalStateException("Couldn't parse the base URL for authentication purposes.");
        }
    }
}
