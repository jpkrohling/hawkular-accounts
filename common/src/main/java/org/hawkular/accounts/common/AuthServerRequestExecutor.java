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
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Base64;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * @author Juraci Paixão Kröhling
 */
@ApplicationScoped
public class AuthServerRequestExecutor {
    @Inject @RealmResourceName
    private String clientId;

    @Inject @RealmResourceSecret
    private String secret;

    public String execute(String url, String method) throws Exception {
        return execute(url, null, clientId, secret, method);
    }

    public String execute(String url, String urlParameters, String method) throws Exception {
        return execute(url, urlParameters, clientId, secret, method);
    }

    public String execute(String url, String clientId, String secret, String method) throws Exception {
        return execute(url, null, clientId, secret, method);
    }

    public String execute(String url, String urlParameters, String clientId, String secret, String method) throws
            Exception {

        HttpURLConnection connection;
        if ("POST".equalsIgnoreCase(method)) {
            connection =  (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoInput(true);
            connection.setDoOutput(true);

            if (null != urlParameters) {
                try (PrintWriter out = new PrintWriter(connection.getOutputStream())) {
                    out.print(urlParameters);
                }
            }
        } else {
            connection =  (HttpURLConnection) new URL(url + "?" + urlParameters).openConnection();
            connection.setRequestMethod(method);
        }

        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        String credentials = clientId + ":" + secret;
        String authorizationHeader = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
        connection.setRequestProperty("Authorization", authorizationHeader);

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

        return response.toString();

    }

}
