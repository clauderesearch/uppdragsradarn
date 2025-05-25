package com.uppdragsradarn.infrastructure.crawler.util;

/**
 * Centralized prompts for job description extraction and formatting using LLMs. These prompts
 * ensure consistent extraction across different job sites and formats.
 */
public class JobDescriptionPrompts {

  /**
   * Main prompt for extracting and formatting job descriptions from HTML/text content. This prompt
   * instructs the LLM to: - Extract only relevant job description content - Remove navigation,
   * headers, footers, and other non-content elements - Format output consistently with proper
   * spacing and structure - Handle various input formats uniformly
   */
  public static final String EXTRACTION_PROMPT =
      """
        You are a job description extraction specialist. Your task is to extract and format job description content from various HTML or text inputs.

        ## Instructions:

        1. **Extract only the main content**: Focus solely on the actual job description, including:
           - Job responsibilities and duties
           - Required qualifications and skills
           - Nice-to-have qualifications
           - Company/team description (if part of the job posting)
           - Benefits and compensation information
           - Work conditions (remote/onsite, hours, etc.)
           - Application instructions

        2. **Remove all non-content elements**:
           - Navigation menus
           - Headers/footers
           - Social media links
           - Cookie notices
           - Related job listings
           - Advertisement sections
           - UI elements like buttons or form fields
           - Meta information (posted date, job ID, etc. unless specifically part of the description)

        3. **Format the output consistently**:
           - Use clear paragraph breaks between different sections
           - Maintain logical grouping of related information
           - Preserve bullet points and numbered lists where appropriate
           - Use consistent spacing (one blank line between major sections)
           - Start each major section on a new line

        4. **Handle various input formats**:
           - HTML with various tag structures
           - Plain text with different formatting styles
           - Mixed content with formatting artifacts
           - Incomplete or partially loaded content

        5. **Preserve important information while cleaning**:
           - Keep all relevant job details
           - Maintain the original meaning and context
           - Fix obvious typos or encoding issues
           - Remove duplicate content
           - Clean up excessive whitespace or formatting characters

        ## Output Format:

        Return the extracted job description as clean, well-formatted text with:
        - Clear section breaks
        - Proper paragraph spacing
        - Preserved list structures
        - No HTML tags or formatting artifacts
        - No extraneous content

        ## Special Instructions:

        1. If the input contains no clear job description content, return "No job description content found."
        2. If the content is in a non-English language, preserve the original language
        3. Always prioritize readability and logical flow in the output
        4. When in doubt about whether content belongs to the job description, include it

        Extract and format the following content:
        """;

  /** Simplified prompt for cases where content is already mostly clean */
  public static final String SIMPLE_CLEANING_PROMPT =
      """
        Clean and format the following job description text. Remove any HTML tags,
        fix spacing issues, and ensure proper paragraph breaks. Preserve all actual
        job content while removing formatting artifacts:
        """;

  /** Prompt for extracting specific sections from job descriptions */
  public static final String SECTION_EXTRACTION_PROMPT =
      """
        Extract the %s section from the following job description.
        Return only the content related to %s, formatted as clean text:
        """;

  private JobDescriptionPrompts() {
    // Utility class, prevent instantiation
  }
}
