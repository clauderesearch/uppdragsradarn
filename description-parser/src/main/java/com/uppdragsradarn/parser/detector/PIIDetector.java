package com.uppdragsradarn.parser.detector;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects potential Personally Identifiable Information (PII) in text.
 * This includes names, email addresses, phone numbers, and other sensitive data.
 */
@Slf4j
public class PIIDetector {
    
    // Email pattern - exclude corporate domains ework, verama, etc.
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"
    );
    
    // Known corporate domains that should be excluded from PII
    private static final List<String> EXCLUDED_DOMAINS = List.of(
        "@eworkgroup", "@verama", "@uppdragsgivare", "@kund", "@client", "@customer"
    );
    
    // Phone number patterns - Swedish phone numbers only
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        // Swedish mobile with country code
        "(?:\\+46|0046)[\\s-]?(?:0)?[\\s-]?7[0236][\\s-]?\\d{3}[\\s-]?\\d{2}[\\s-]?\\d{2}\\b|" +
        // Swedish mobile without country code (07xx xxx xx xx)
        "\\b0[7][0236]\\d{7}\\b|" +
        "\\b0[7][0236][\\s-]?\\d{3}[\\s-]?\\d{2}[\\s-]?\\d{2}\\b|" +
        // Swedish landline (0xx-xxx xx xx)
        "\\b0[1-9]\\d[\\s-]?\\d{3}[\\s-]?\\d{2}[\\s-]?\\d{2}\\b"
    );
    
    // Pattern to match dates/years to exclude from phone detection
    private static final Pattern DATE_YEAR_PATTERN = Pattern.compile(
        "\\b(?:19|20)\\d{2}(?:-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\\d|3[01]))?\\b|" + // Dates YYYY-MM-DD or years
        "\\b\\d{4}(?:-\\d{2})?(?:-\\d{2})?\\b" // Generic date patterns
    );
    
    // Swedish personal number pattern - must have the 4 digit suffix with dash
    private static final Pattern PERSONAL_NUMBER_PATTERN = Pattern.compile(
        "\\b(?:19|20)?\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])-\\d{4}\\b"
    );
    
    // Common name patterns - very specific contexts only
    private static final Pattern NAME_PATTERN = Pattern.compile(
        // Full names with explicit role context (must have full name)
        "(?:contact\\s+person)\\s*:?\\s*([A-Z][a-z]+\\s+[A-Z][a-z]+)\\b|" +
        "(?:project\\s+manager)\\s*:?\\s*([A-Z][a-z]+\\s+[A-Z][a-z]+)\\b|" +
        "(?:kontaktperson)\\s*:?\\s*([A-ZÅÄÖ][a-zåäö]+\\s+[A-ZÅÄÖ][a-zåäö]+)\\b|" +
        // Formal titles with names
        "(?:Mr\\.?|Mrs\\.?|Ms\\.?|Dr\\.?|Prof\\.)\\s+([A-Z][a-z]+\\s+[A-Z][a-z]+)\\b",
        Pattern.CASE_INSENSITIVE
    );
    
    // Credit card patterns (basic detection)
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile(
        "\\b(?:\\d{4}[\\s-]?){3}\\d{4}\\b"
    );
    
    // Social media handles - need word boundary at start to avoid email parts
    private static final Pattern SOCIAL_MEDIA_PATTERN = Pattern.compile(
        "(?<!\\w)@[A-Za-z0-9_]+"
    );
    
    // Physical addresses (simplified Swedish pattern)
    private static final Pattern ADDRESS_PATTERN = Pattern.compile(
        "(?:[A-ZÅÄÖ][a-zåäö]+(?:gatan|vägen|plats|torg)\\s+\\d+)|" +
        "(?:Box\\s+\\d+)|" +
        "(?:\\d{3}\\s*\\d{2}\\s+[A-ZÅÄÖ][a-zåäö]+)",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Detects potential PII in the given text.
     * 
     * @param text The text to analyze
     * @return Detection result with details about any PII found
     */
    public PIIDetectionResult detect(String text) {
        if (text == null || text.isEmpty()) {
            return new PIIDetectionResult(false, new ArrayList<>());
        }
        
        List<PIIMatch> matches = new ArrayList<>();
        
        // Check for email addresses
        Matcher emailMatcher = EMAIL_PATTERN.matcher(text);
        while (emailMatcher.find()) {
            String email = emailMatcher.group();
            // Skip company emails (eworkgroup, verama, etc.)
            if (!isExcludedEmail(email)) {
                matches.add(new PIIMatch("Email", email, emailMatcher.start(), emailMatcher.end()));
            }
        }
        
        // Check for phone numbers
        Matcher phoneMatcher = PHONE_PATTERN.matcher(text);
        while (phoneMatcher.find()) {
            String phone = phoneMatcher.group();
            matches.add(new PIIMatch("Phone", phone, phoneMatcher.start(), phoneMatcher.end()));
        }
        
        // Check for personal numbers
        Matcher personalNumberMatcher = PERSONAL_NUMBER_PATTERN.matcher(text);
        while (personalNumberMatcher.find()) {
            matches.add(new PIIMatch("PersonalNumber", personalNumberMatcher.group(), 
                personalNumberMatcher.start(), personalNumberMatcher.end()));
        }
        
        // Check for names
        Matcher nameMatcher = NAME_PATTERN.matcher(text);
        while (nameMatcher.find()) {
            // Try each capturing group since we have multiple alternatives
            for (int i = 1; i <= nameMatcher.groupCount(); i++) {
                String name = nameMatcher.group(i);
                if (name != null && !name.isEmpty()) {
                    matches.add(new PIIMatch("Name", name, nameMatcher.start(i), nameMatcher.end(i)));
                    break; // Only take the first non-null group for each match
                }
            }
        }
        
        // Check for credit cards
        Matcher creditCardMatcher = CREDIT_CARD_PATTERN.matcher(text);
        while (creditCardMatcher.find()) {
            String potentialCard = creditCardMatcher.group();
            // Basic Luhn check could be added here for higher accuracy
            if (potentialCard.replaceAll("[\\s-]", "").length() == 16) {
                matches.add(new PIIMatch("CreditCard", potentialCard, 
                    creditCardMatcher.start(), creditCardMatcher.end()));
            }
        }
        
        // Check for social media handles
        Matcher socialMatcher = SOCIAL_MEDIA_PATTERN.matcher(text);
        while (socialMatcher.find()) {
            String handle = socialMatcher.group();
            // Skip company social handles
            if (!isExcludedSocialHandle(handle)) {
                matches.add(new PIIMatch("SocialMedia", handle, 
                    socialMatcher.start(), socialMatcher.end()));
            }
        }
        
        // Check for addresses
        Matcher addressMatcher = ADDRESS_PATTERN.matcher(text);
        while (addressMatcher.find()) {
            matches.add(new PIIMatch("Address", addressMatcher.group(), 
                addressMatcher.start(), addressMatcher.end()));
        }
        
        boolean hasPII = !matches.isEmpty();
        
        if (hasPII) {
            log.warn("Detected potential PII in text. Found {} matches", matches.size());
        }
        
        return new PIIDetectionResult(hasPII, matches);
    }
    
    /**
     * Checks if an email belongs to a known company domain
     */
    private boolean isExcludedEmail(String email) {
        String lowerEmail = email.toLowerCase();
        return lowerEmail.endsWith("@eworkgroup.com") || 
               lowerEmail.endsWith("@verama.com") ||
               lowerEmail.endsWith("@uppdragsgivare.com");
    }
    
    /**
     * Checks if a social media handle is a company handle
     */
    private boolean isExcludedSocialHandle(String handle) {
        String lowerHandle = handle.toLowerCase();
        return EXCLUDED_DOMAINS.stream()
            .anyMatch(excluded -> lowerHandle.equalsIgnoreCase(excluded));
    }
    
    
    /**
     * Anonymizes detected PII in text by replacing it with placeholder text.
     * 
     * @param text The text to anonymize
     * @param detectionResult The PII detection result
     * @return Anonymized text
     */
    public String anonymize(String text, PIIDetectionResult detectionResult) {
        if (!detectionResult.isHasPII() || text == null) {
            return text;
        }
        
        StringBuilder anonymized = new StringBuilder(text);
        
        // Sort matches by position (descending) to avoid offset issues when replacing
        List<PIIMatch> sortedMatches = new ArrayList<>(detectionResult.getMatches());
        sortedMatches.sort((a, b) -> Integer.compare(b.getStart(), a.getStart()));
        
        for (PIIMatch match : sortedMatches) {
            String placeholder = String.format("[%s REMOVED]", match.getType().toUpperCase());
            anonymized.replace(match.getStart(), match.getEnd(), placeholder);
        }
        
        return anonymized.toString();
    }
}