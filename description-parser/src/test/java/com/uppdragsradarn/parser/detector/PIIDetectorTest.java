package com.uppdragsradarn.parser.detector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PIIDetectorTest {
    
    private PIIDetector detector;
    
    @BeforeEach
    void setUp() {
        detector = new PIIDetector();
    }
    
    @Test
    void testDetectsPrivateEmail() {
        String text = "Contact me at john.doe@gmail.com";
        PIIDetectionResult result = detector.detect(text);
        
        // Debug to see what was detected
        result.getMatches().forEach(match -> {
            System.out.println("Found: " + match.getType() + " = " + match.getValue());
        });
        
        assertTrue(result.isHasPII());
        assertEquals(1, result.getMatches().size());
        assertEquals("Email", result.getMatches().get(0).getType());
        assertEquals("john.doe@gmail.com", result.getMatches().get(0).getValue());
    }
    
    @Test
    void testExcludesCompanyEmails() {
        String text = "Contact us at support@eworkgroup.com or info@verama.com";
        PIIDetectionResult result = detector.detect(text);
        
        // Debug
        result.getMatches().forEach(match -> {
            System.out.println("Found: " + match.getType() + " = " + match.getValue());
        });
        
        assertFalse(result.isHasPII());
        assertEquals(0, result.getMatches().size());
    }
    
    @Test
    void testDetectsPhoneNumbers() {
        String text = "Call me at 0701234567";
        PIIDetectionResult result = detector.detect(text);
        
        assertTrue(result.isHasPII());
        assertEquals(1, result.getMatches().size());
        assertEquals("Phone", result.getMatches().get(0).getType());
        assertEquals("0701234567", result.getMatches().get(0).getValue());
    }
    
    @Test
    void testExcludesDatesAsPhoneNumbers() {
        String text = "Start date is 2025-05-26 and it ends 2026-12-31. The year 2025 is important.";
        PIIDetectionResult result = detector.detect(text);
        
        assertFalse(result.isHasPII());
        assertEquals(0, result.getMatches().size());
    }
    
    @Test
    void testExcludesCompanySocialHandles() {
        String text = "Follow us at @eworkgroup and @verama";
        PIIDetectionResult result = detector.detect(text);
        
        assertFalse(result.isHasPII());
        assertEquals(0, result.getMatches().size());
    }
    
    @Test
    void testDetectsPrivateSocialHandle() {
        String text = "Follow me at @johndoe123";
        PIIDetectionResult result = detector.detect(text);
        
        assertTrue(result.isHasPII());
        assertEquals(1, result.getMatches().size());
        assertEquals("SocialMedia", result.getMatches().get(0).getType());
        assertEquals("@johndoe123", result.getMatches().get(0).getValue());
    }
    
    @Test
    void testDetectsNames() {
        String text = "Contact person: John Smith for more information";
        PIIDetectionResult result = detector.detect(text);
        
        // Debug
        result.getMatches().forEach(match -> {
            System.out.println("Found: " + match.getType() + " = " + match.getValue());
        });
        
        assertTrue(result.isHasPII());
        assertEquals(1, result.getMatches().size());
        assertEquals("Name", result.getMatches().get(0).getType());
        assertEquals("John Smith", result.getMatches().get(0).getValue());
    }
    
    @Test
    void testDetectsPersonalNumbers() {
        String text = "Personal number: 19900101-1234";
        PIIDetectionResult result = detector.detect(text);
        
        assertTrue(result.isHasPII());
        assertEquals(1, result.getMatches().size());
        assertEquals("PersonalNumber", result.getMatches().get(0).getType());
        assertEquals("19900101-1234", result.getMatches().get(0).getValue());
    }
    
    @Test
    void testMultiplePIITypes() {
        String text = "Contact person: John Smith at john@gmail.com or call 0701234567";
        PIIDetectionResult result = detector.detect(text);
        
        // Debug
        result.getMatches().forEach(match -> {
            System.out.println("Found " + match.getType() + ": " + match.getValue());
        });
        
        assertTrue(result.isHasPII());
        assertEquals(3, result.getMatches().size());
        
        // Should detect name, email, and phone
        long emailCount = result.getMatches().stream()
            .filter(m -> m.getType().equals("Email"))
            .count();
        long phoneCount = result.getMatches().stream()
            .filter(m -> m.getType().equals("Phone"))
            .count(); 
        long nameCount = result.getMatches().stream()
            .filter(m -> m.getType().equals("Name"))
            .count();
            
        assertEquals(1, emailCount);
        assertEquals(1, phoneCount);
        assertEquals(1, nameCount);
    }
}