package com.google.cache;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Random;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class CacheServlet extends HttpServlet {
  private DateFormat df =
      SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.FULL, SimpleDateFormat.FULL);

  private static final String CHARS =
      "\nabcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890";

  Random RANDOM = new Random();

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    // Time to delay (sleep this thread) before returning a response as an easy
    // way to distinguish the original request from a cached response
    String delayText = req.getParameter("delay");
    if (delayText != null) {
      try {
        Thread.sleep(Integer.parseInt(delayText) * 1000L);
      } catch (InterruptedException ignore) {
      }
    }

    Date now = new Date();

    // Time to allow this request to be cached by the browser, HTTP/1.0 proxies
    // and HTTP/1.1 proxies
    String cacheText = req.getParameter("cache");
    if (cacheText != null) {
      int age = Integer.parseInt(cacheText);

      resp.setDateHeader("Date", now.getTime());

      if (age <= 0) {
        // HTTP/1.0
        resp.setDateHeader("Expires", 0);
        resp.setHeader("Pragma", "no-cache");

        // HTTP/1.1
        resp.setHeader("Cache-Control", "no-cache, must-revalidate");
      } else {
        // HTTP/1.0
        resp.setDateHeader("Expires", now.getTime() + 1000L * age);

        // HTTP/1.1
        resp.setHeader("Cache-Control", "public, s-maxage=" + age);
      }
    }

    resp.setContentType("text/html");
    PrintWriter writer = resp.getWriter();
    writer.println("<html><head><title>Cache Servlet</title></head><body>");

    // Timestamp which identifies when this request was served
    writer.println(df.format(now) + "<br>");
    writer.println("System.nanoTime(): " + System.nanoTime() + "<br>");

    // Dump request headers if 'headers' is among the request parameters
    if (req.getParameter("headers") != null) {
      writer.println("<br><b>Request Headers:</b><br>");
      for (
          @SuppressWarnings("unchecked")
      Enumeration<String> e = req.getHeaderNames(); e.hasMoreElements();) {
        String key = e.nextElement();
        writer.println("- <b>" + key + ":</b> " + req.getHeader(key) + "<br>");
      }
    }

    String sizeText = req.getParameter("size");
    if (sizeText != null) {
      int size = Integer.parseInt(sizeText);
      for (int i = 0; i < size; i++) {
        writer.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
      }
    }

    // If not parameters are specified, suggest a few examples
    if (req.getQueryString() == null) {
      String baseUrl = req.getRequestURL().toString().replace(req.getServletPath(), "");
      writer.println("<br><b>Examples:</b><ul>");
      writer.println(
          "<li>" + makeUrl(baseUrl) + " (default cache headers for a dynamic request)</li>");
      writer.println("<li>" + makeUrl(baseUrl + "/static.html")
          + " (default cache headers for a static request)</li>");
      writer.println("<li>" + makeUrl(baseUrl + "?cache=0") + " (do not cache)</li>");
      writer.println("<li>" + makeUrl(baseUrl + "?delay=10&cache=600")
          + " (sleep for 10 seconds, then respond with 10 minute (600 seconds) cachable result)</li>");
      writer.println("<li>" + makeUrl(baseUrl + "?delay=10&cache=600&headers=true")
          + " (echo back HTTP request headers received)</li>");
      writer.println("<li>" + makeUrl(baseUrl + "?delay=10&cache=600&size=1000")
          + " (response includes 1000 randomish characters)</li>");
      writer.println("</ul>");

      writer.println("<p><a href='static.html'>Static HTML (browser cache) test</a></p>");
    }

  }

  private String makeUrl(String url) {
    return "<a href='" + url + "'>" + url + "</a>";
  }

}
