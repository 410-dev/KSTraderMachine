package me.hysong.apis.kstrader.v1.utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class CurlEmulator {
    /**
     * Sends an HTTP request to the given URL.
     *
     * @param url     the target URL
     * @param method  HTTP method (GET, POST, PUT, DELETE, PATCH, etc.)
     * @param headers request headers as lines of "Name: Value" separated by \n
     * @param data    request body (for POST/PUT/PATCH), or null/empty
     * @return the response body (or error body if status >= 400)
     */
    public static String curl(String url, String method, String headers, String data) {
        HttpURLConnection connection = null;
        try {
            // Open connection
            URL requestUrl = new URL(url);
            connection = (HttpURLConnection) requestUrl.openConnection();
            connection.setRequestMethod(method.toUpperCase());

            // Parse and set headers
            if (headers != null && !headers.trim().isEmpty()) {
                String[] lines = headers.split("\\r?\\n");
                for (String line : lines) {
                    int idx = line.indexOf(':');
                    if (idx > 0) {
                        String name  = line.substring(0, idx).trim();
                        String value = line.substring(idx + 1).trim();
                        connection.setRequestProperty(name, value);
                    }
                }
            }

            // Send body if applicable
            if (data != null && !data.isEmpty()
                    && ("POST".equalsIgnoreCase(method)
                    || "PUT".equalsIgnoreCase(method)
                    || "PATCH".equalsIgnoreCase(method)))
            {
                connection.setDoOutput(true);
                byte[] out = data.getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(out.length);
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(out);
                }
            }

            // Print request data
            System.out.println("Request URL: " + url);
            System.out.println("Request Method: " + method);
            System.out.println("Request Headers: " + headers);
            System.out.println("Request Body: " + data);

            // Trigger the request and get status
            int status = connection.getResponseCode();

            // Choose input (error) stream
            InputStream is = (status >= HttpURLConnection.HTTP_BAD_REQUEST)
                    ? connection.getErrorStream()
                    : connection.getInputStream();

            // Read the response
            if (is == null) {
                return "";
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8)))
            {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append(System.lineSeparator());
                }
                return sb.toString().trim();
            }

        } catch (IOException e) {
            // Fail fast with context
            throw new RuntimeException("HTTP request failed: " + e.getMessage(), e);

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
