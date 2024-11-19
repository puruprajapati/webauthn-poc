package com.bofa.webauthn.filter;

import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class LoggingFilter implements jakarta.servlet.Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
    throws IOException, ServletException {

    if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      HttpServletResponse httpResponse = (HttpServletResponse) response;

      // Skip logging for /actuator and /health endpoints
      String uri = httpRequest.getRequestURI();
      if (uri.startsWith("/actuator") || uri.startsWith("/health-check")) {
        chain.doFilter(request, response);
        return;  // Skip further processing
      }

      ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(httpRequest);
      ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(httpResponse);
      try {
        // Proceed with the filter chain and wrap the response
        chain.doFilter(wrappedRequest, wrappedResponse);

        // Log the response after processing
        logRequestResponse(wrappedRequest, wrappedResponse);

        // Copy response back to the client
        wrappedResponse.copyBodyToResponse();

      } catch (Exception ex) {
        // Log the exception if it occurs during request processing
        logException(wrappedRequest, ex);
        throw ex; // Rethrow the exception after logging it
      }
    } else {
      chain.doFilter(request, response);
    }
  }

  private void logRequestResponse(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response) throws IOException {
    createLogsDirectory(); // Ensure logs directory exists

    String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
    String method = request.getMethod();
    String uri = request.getRequestURI();
    String filename = String.format("logs/%s_%s_%s.log", method, uri.replaceAll("[^a-zA-Z0-9]", "_"), timestamp);

    try (FileWriter fileWriter = new FileWriter(filename, true)) {
      String requestBody = new String(request.getContentAsByteArray(), StandardCharsets.UTF_8);
      String responseBody = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);

      fileWriter.write("Request Body: " + requestBody + System.lineSeparator());
      fileWriter.write("Response Status: " + response.getStatus() + System.lineSeparator());
      fileWriter.write("Response Body: " + responseBody + System.lineSeparator());
    }
  }

  private void logException(ContentCachingRequestWrapper request, Exception ex) throws IOException {
    createLogsDirectory(); // Ensure logs directory exists

    String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
    String method = request.getMethod();
    String uri = request.getRequestURI();
    String filename = String.format("logs/%s_%s_%s_EXCEPTION.log", method, uri.replaceAll("[^a-zA-Z0-9]", "_"), timestamp);


    try (FileWriter fileWriter = new FileWriter(filename, true)) {
      String requestBody = new String(request.getContentAsByteArray(), StandardCharsets.UTF_8);

      fileWriter.write("Request URI: " + request.getRequestURI() + System.lineSeparator());
      fileWriter.write("Method: " + method + System.lineSeparator());
      fileWriter.write("Request Body: " + requestBody + System.lineSeparator());
      fileWriter.write("Exception: " + ex.getMessage() + System.lineSeparator());
      fileWriter.write("Stack Trace: " + System.lineSeparator());
      for (StackTraceElement element : ex.getStackTrace()) {
        fileWriter.write(element.toString() + System.lineSeparator());
      }
    }
  }

  private void createLogsDirectory() {
    File logsDir = new File("logs");
    if (!logsDir.exists()) {
      logsDir.mkdirs();
    }
  }
}
