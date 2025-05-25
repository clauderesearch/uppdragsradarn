package com.uppdragsradarn.crawler.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;
import reactor.netty.http.client.HttpClient;

@Slf4j
public class TestASociety {
  public static void main(String[] args) {
    // Set this to true to try analyzing a different URL structure
    boolean tryDirectJobsPage = true;
    try {
      log.info("Testing A Society crawler with WebClient...");

      String url = "https://www.asocietygroup.com/sv/uppdrag";
      String userAgent =
          "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:138.0) Gecko/20100101 Firefox/138.0";

      // Create WebClient with proper configuration
      HttpClient httpClient =
          HttpClient.create().responseTimeout(Duration.ofSeconds(30)).followRedirect(true);

      WebClient webClient =
          WebClient.builder()
              .clientConnector(new ReactorClientHttpConnector(httpClient))
              .defaultHeader("User-Agent", userAgent)
              .defaultHeader(
                  "Accept",
                  "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
              .defaultHeader("Accept-Language", "en-US,en;q=0.5")
              .defaultHeader("Accept-Encoding", "gzip, deflate, br")
              .defaultHeader("DNT", "1")
              .defaultHeader("Connection", "keep-alive")
              .defaultHeader("Cache-Control", "no-cache")
              .defaultHeader("Pragma", "no-cache")
              .build();

      log.info("Connecting to {} ...", url);

      // Request the content as byte array to handle binary data
      byte[] responseBytes =
          webClient
              .get()
              .uri(url)
              .accept(MediaType.TEXT_HTML, MediaType.APPLICATION_XHTML_XML, MediaType.ALL)
              .header("Sec-Fetch-Dest", "document")
              .header("Sec-Fetch-Mode", "navigate")
              .header("Sec-Fetch-Site", "none")
              .header("Sec-Fetch-User", "?1")
              .header("Upgrade-Insecure-Requests", "1")
              .retrieve()
              .bodyToMono(byte[].class)
              .block(Duration.ofSeconds(30));

      if (responseBytes == null) {
        throw new RuntimeException("No response body received");
      }

      log.info("Response received successfully");
      log.info("Response size: {} bytes", responseBytes.length);

      // Check for binary content
      int nullBytes = 0;
      for (int i = 0; i < Math.min(100, responseBytes.length); i++) {
        if (responseBytes[i] == 0) {
          nullBytes++;
        }
      }

      if (nullBytes > 5) {
        log.info("Response contains {} null bytes in first 100 bytes - likely binary", nullBytes);
      }

      // Try different character encodings
      String htmlUtf8 = new String(responseBytes, StandardCharsets.UTF_8);
      String htmlAscii = new String(responseBytes, StandardCharsets.US_ASCII);
      String htmlIso = new String(responseBytes, StandardCharsets.ISO_8859_1);

      log.info("UTF-8 content length: {}", htmlUtf8.length());
      log.info("ASCII content length: {}", htmlAscii.length());
      log.info("ISO-8859-1 content length: {}", htmlIso.length());

      log.info("UTF-8 contains 'requisition_': {}", htmlUtf8.contains("requisition_"));
      log.info("ASCII contains 'requisition_': {}", htmlAscii.contains("requisition_"));
      log.info("ISO-8859-1 contains 'requisition_': {}", htmlIso.contains("requisition_"));

      // Choose the best encoding based on content inspection
      String html;
      if (htmlAscii.contains("requisition_") && !htmlUtf8.contains("requisition_")) {
        log.info("Using ASCII encoding as it contains the requisition marker");
        html = htmlAscii;
      } else if (htmlIso.contains("requisition_") && !htmlUtf8.contains("requisition_")) {
        log.info("Using ISO-8859-1 encoding as it contains the requisition marker");
        html = htmlIso;
      } else {
        log.info("Using UTF-8 encoding (default)");
        html = htmlUtf8;
      }

      // Parse with Jsoup
      Document document;
      try {
        document = Jsoup.parse(html, url);
        log.info("Successfully parsed document. Title: {}", document.title());
      } catch (Exception e) {
        log.warn("Failed to parse document: {}", e.getMessage());
        document = new Document(url);
        document.html(html);
        log.info("Created emergency document with raw HTML");
      }

      // Save the raw content to a file for analysis
      File outputDir = new File("target/asociety-test");
      outputDir.mkdirs();

      File rawFile = new File(outputDir, "raw_response.bin");
      try (FileOutputStream fos = new FileOutputStream(rawFile)) {
        fos.write(responseBytes);
      }
      log.info("Saved raw response to: {}", rawFile.getAbsolutePath());

      // Save each encoding version
      File utf8File = new File(outputDir, "utf8_content.html");
      Files.write(utf8File.toPath(), htmlUtf8.getBytes(StandardCharsets.UTF_8));
      log.info("Saved UTF-8 content to: {}", utf8File.getAbsolutePath());

      File asciiFile = new File(outputDir, "ascii_content.txt");
      Files.write(asciiFile.toPath(), htmlAscii.getBytes(StandardCharsets.UTF_8));
      log.info("Saved ASCII content to: {}", asciiFile.getAbsolutePath());

      File isoFile = new File(outputDir, "iso_content.txt");
      Files.write(isoFile.toPath(), htmlIso.getBytes(StandardCharsets.UTF_8));
      log.info("Saved ISO-8859-1 content to: {}", isoFile.getAbsolutePath());

      // Save the final selected encoding
      File selectedFile = new File(outputDir, "selected_content.html");
      Files.write(selectedFile.toPath(), html.getBytes(StandardCharsets.UTF_8));
      log.info("Saved selected content to: {}", selectedFile.getAbsolutePath());

      // Additional content checks
      log.info("Content contains 'jobs' string? {}", html.contains("jobs"));
      log.info("Content contains 'abstract_id' string? {}", html.contains("abstract_id"));

      // Try direct API request
      try {
        log.info("\nAttempting to connect to potential JSON API endpoint...");
        String jsonUrl = "https://www.asocietygroup.com/api/jobs";

        String jsonResponse =
            webClient
                .get()
                .uri(jsonUrl)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(10));

        if (jsonResponse != null) {
          log.info("JSON API response received, length: {}", jsonResponse.length());
          File jsonFile = new File(outputDir, "api_response.json");
          Files.write(jsonFile.toPath(), jsonResponse.getBytes(StandardCharsets.UTF_8));
          log.info("Saved JSON API response to: {}", jsonFile.getAbsolutePath());
        }
      } catch (Exception e) {
        log.error("JSON API request failed: {}", e.getMessage());
      }

      // Try direct jobs API
      if (tryDirectJobsPage) {
        try {
          log.info("\nAttempting to connect to careers/jobs page...");
          String[] potentialUrls = {
            "https://www.asocietygroup.com/careers",
            "https://www.asocietygroup.com/jobs",
            "https://careers.asocietygroup.com",
            "https://jobs.asocietygroup.com",
            "https://www.asocietygroup.com/sv/lediga-tjanster",
            "https://www.asocietygroup.com/en/jobs",
            "https://www.asocietygroup.com/en/careers"
          };

          for (String potentialUrl : potentialUrls) {
            try {
              log.info("Trying URL: {}", potentialUrl);

              byte[] careerResponse =
                  webClient
                      .get()
                      .uri(potentialUrl)
                      .retrieve()
                      .bodyToMono(byte[].class)
                      .block(Duration.ofSeconds(10));

              if (careerResponse != null) {
                String careerHtml = new String(careerResponse, StandardCharsets.UTF_8);
                File careerFile =
                    new File(
                        outputDir,
                        "career_page_" + potentialUrl.replaceAll("[^a-zA-Z0-9]", "_") + ".html");
                Files.write(careerFile.toPath(), careerHtml.getBytes(StandardCharsets.UTF_8));
                log.info("Saved career/jobs page to: {}", careerFile.getAbsolutePath());
                log.info("Response size: {}", careerHtml.length());

                // Check for job data
                if (careerHtml.contains("job")
                    || careerHtml.contains("position")
                    || careerHtml.contains("career")
                    || careerHtml.contains("vacancy")) {
                  log.info("Found potential job-related content!");
                }
              }
            } catch (Exception e) {
              log.warn("Failed for URL {}: {}", potentialUrl, e.getMessage());
            }
          }
        } catch (Exception e) {
          log.error("Careers/jobs pages check failed: {}", e.getMessage());
        }
      }

      // Look for requisition JSON
      String jobData = extractJobListings(html);
      if (jobData != null) {
        log.info("Found job data: {} bytes", jobData.length());
        log.info("First 200 chars: {}", jobData.substring(0, Math.min(200, jobData.length())));

        // Save the JSON data
        File jsonFile = new File(outputDir, "job_listings.json");
        Files.write(jsonFile.toPath(), jobData.getBytes(StandardCharsets.UTF_8));
        log.info("Saved job data to: {}", jsonFile.getAbsolutePath());

        // Check if we can extract individual job listings
        Pattern jobPattern =
            Pattern.compile("\\{\"requisition_.*?\"abstract_id\":\"[^\"]*\".*?\\}", Pattern.DOTALL);
        Matcher jobMatcher = jobPattern.matcher(jobData);

        int count = 0;
        while (jobMatcher.find()) {
          count++;
          String job = jobMatcher.group(0);
          log.info(
              "Job {} (first 50 chars): {}...",
              count,
              job.substring(0, Math.min(50, job.length())));
        }

        log.info("Found {} individual job listings", count);
      } else {
        log.info("No job data found");

        // Try alternative regex approach for debugging
        log.info("Trying fallback regex extraction...");

        String[] regexPatterns = {
          "\\[\\{.*?\"requisition_name\".*?\\}\\]",
          "\\[\\{.*?\"abstract_id\".*?\\}\\]",
          "\\{\"requisition_.*?\"abstract_id\":\"[^\"]*\".*?\\}"
        };

        for (String pattern : regexPatterns) {
          log.info("Trying pattern: {}", pattern);
          Pattern p = Pattern.compile(pattern, Pattern.DOTALL);
          Matcher m = p.matcher(html);

          if (m.find()) {
            String match = m.group(0);
            log.info("Found match with length: {}", match.length());
            log.info("First 100 chars: {}", match.substring(0, Math.min(100, match.length())));
            break;
          } else {
            log.info("No match found with this pattern");
          }
        }
      }
    } catch (Exception e) {
      log.error("Error in A Society crawler test", e);
    }
  }

  private static String extractJobListings(String html) {
    log.info("Searching for job listings in HTML...");

    // First check if we have a complete JSON array already
    if (html.contains("[{\"requisition_") && html.contains("abstract_id")) {
      log.info("Found direct JSON pattern in HTML");

      // Try to extract using regex for full JSON array
      try {
        Pattern fullJsonPattern = Pattern.compile("\\[\\{.*?requisition_.*?\\}\\]", Pattern.DOTALL);
        Matcher fullJsonMatcher = fullJsonPattern.matcher(html);

        if (fullJsonMatcher.find()) {
          String potentialJson = fullJsonMatcher.group(0);

          // Validate that this looks like job listings JSON
          if (potentialJson.contains("requisition_name") && potentialJson.contains("abstract_id")) {
            log.info("Found complete JSON array: {} bytes", potentialJson.length());
            return potentialJson;
          }
        }
      } catch (Exception e) {
        log.error("Error trying to extract complete JSON array: {}", e.getMessage());
      }
    }

    // Try searching for known patterns
    String[] patterns = {
      "[{\"requisition_",
      "[{\\\"requisition_",
      "document.write(\"[{",
      "\"requisition_name\":",
      "jobs = [",
      "var jobs =",
      "jobs=",
      "jsonData="
    };

    for (String patternStr : patterns) {
      int index = html.indexOf(patternStr);
      if (index >= 0) {
        log.info("Found pattern: {} at position {}", patternStr, index);

        // Find a reasonable chunk of text
        int start = Math.max(0, index);
        int end = Math.min(html.length(), index + 50000); // Increased buffer size
        String chunk = html.substring(start, end);

        // Look for proper JSON array pattern
        if (chunk.startsWith("[{") || chunk.contains("[{\"requisition_")) {
          log.info("Found JSON array start pattern");
          String jsonData = findBalancedJson(chunk);

          if (jsonData != null) {
            return jsonData;
          }
        } else if (patternStr.contains("jobs") || patternStr.contains("jsonData")) {
          // Look for assignment after variable declarations
          int jsonStart = chunk.indexOf("[{");
          if (jsonStart >= 0) {
            String jsonChunk = chunk.substring(jsonStart);
            String jsonData = findBalancedJson(jsonChunk);

            if (jsonData != null) {
              log.info("Successfully extracted job listings from variable");
              return jsonData;
            }
          }
        }
      }
    }

    // As a last resort, try extracting individual jobs and creating an array manually
    try {
      Pattern jobPattern =
          Pattern.compile("\\{\"requisition_.*?\"abstract_id\":\"[^\"]*\".*?\\}", Pattern.DOTALL);
      Matcher jobMatcher = jobPattern.matcher(html);

      StringBuilder jobArray = new StringBuilder("[");
      boolean foundAny = false;

      while (jobMatcher.find()) {
        if (foundAny) {
          jobArray.append(",");
        }
        jobArray.append(jobMatcher.group(0));
        foundAny = true;
      }

      if (foundAny) {
        jobArray.append("]");
        String jsonData = jobArray.toString();
        log.info("Built job array from individual job objects: {} bytes", jsonData.length());
        return jsonData;
      }
    } catch (Exception e) {
      log.error("Error trying to extract individual jobs: {}", e.getMessage());
    }

    log.info("No job listings data found using any method");
    return null;
  }

  private static String findBalancedJson(String text) {
    int bracketDepth = 0;
    int squareDepth = 0;
    boolean inQuotes = false;
    boolean escaped = false;
    int endPos = -1;
    int startPos = -1;

    // Find array start if needed
    if (!text.startsWith("[")) {
      int possibleStart = text.indexOf("[{");
      if (possibleStart < 0) {
        log.info("No array start found in text");
        return null;
      }
      startPos = possibleStart;
      text = text.substring(startPos);
    }

    // If text doesn't start with array marker now, something's wrong
    if (!text.startsWith("[")) {
      log.info("Text doesn't start with array marker after preprocessing");
      return null;
    }

    // Initialize depths based on the first character
    squareDepth = 1;

    for (int i = 1; i < text.length(); i++) {
      char c = text.charAt(i);

      if (escaped) {
        escaped = false;
        continue;
      }

      if (c == '\\') {
        escaped = true;
        continue;
      }

      if (c == '"' && !escaped) {
        inQuotes = !inQuotes;
        continue;
      }

      if (!inQuotes) {
        if (c == '{') bracketDepth++;
        else if (c == '}') bracketDepth--;
        else if (c == '[') squareDepth++;
        else if (c == ']') squareDepth--;

        // End of the JSON array - we started with depth 1 so we end at depth 0
        if (squareDepth == 0) {
          // Make sure we have at least 1 complete object
          if (bracketDepth == 0) {
            endPos = i;
            break;
          } else {
            log.warn("JSON array ended but object depth is unbalanced: {}", bracketDepth);
          }
        }
      }
    }

    if (endPos > 0) {
      // Include the closing bracket
      String result = text.substring(0, endPos + 1);

      // Verify basic JSON array structure
      if (result.startsWith("[") && result.endsWith("]")) {
        // Check if this has job data
        if (result.contains("requisition_")
            && (result.contains("abstract_id") || result.contains("requisition_name"))) {
          log.info("Found balanced JSON with job data, length: {}", result.length());
          return result;
        } else {
          log.info("Found balanced JSON but it doesn't appear to contain job data");
        }
      } else {
        log.info("Extracted content doesn't have array brackets");
      }
    }

    // As a fallback, try basic pattern matching
    if (text.contains("abstract_id") || text.contains("requisition_name")) {
      log.info("Attempting fallback extraction since text contains job data markers");

      // Try to find a closing bracket preceded by a closing brace
      int fallbackEnd = text.indexOf("}]");
      if (fallbackEnd > 0) {
        String fallbackJson = text.substring(0, fallbackEnd + 2);
        log.info(
            "Extracted possible JSON using fallback method, length: {}", fallbackJson.length());

        // Basic validation
        if (fallbackJson.startsWith("[{") && fallbackJson.endsWith("}]")) {
          return fallbackJson;
        }
      }
    }

    log.info("Could not find balanced JSON in chunk");
    return null;
  }
}
