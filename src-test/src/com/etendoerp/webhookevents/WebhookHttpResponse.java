package com.etendoerp.webhookevents;

public class WebhookHttpResponse {
  private int statusCode;
  private String message;

  public WebhookHttpResponse(int statusCode, String message) {
    this.statusCode = statusCode;
    this.message = message;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public String getMessage() {
    return message;
  }
}
