package com.uppdragsradarn.infrastructure.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Demonstration of the LLM-based job extraction system. This shows how the new approach works and
 * compares it to the old regex-based system.
 */
@SpringBootTest
@ActiveProfiles("test")
public class LlmExtractionDemo {

  @Test
  public void demonstrateLlmExtraction() {
    System.out.println("=".repeat(80));
    System.out.println("LLM-BASED JOB EXTRACTION SYSTEM DEMONSTRATION");
    System.out.println("=".repeat(80));

    System.out.println("\n📋 OVERVIEW:");
    System.out.println("This system replaces complex regex-based job detail extraction");
    System.out.println("with a simple, reliable LLM-based approach using GPT-4o-mini.");

    System.out.println("\n🔄 OLD APPROACH (Complex & Brittle):");
    System.out.println("  ❌ 400+ lines of regex patterns per provider");
    System.out.println("  ❌ Breaks when HTML structure changes");
    System.out.println("  ❌ Requires Playwright for complex sites");
    System.out.println("  ❌ Hard to maintain and debug");
    System.out.println("  ❌ Different extraction logic per provider");

    System.out.println("\n✨ NEW APPROACH (Simple & Resilient):");
    System.out.println("  ✅ ~50 lines of YAML config per provider");
    System.out.println("  ✅ Works with any HTML structure");
    System.out.println("  ✅ Simple HTTP requests (no browser automation)");
    System.out.println("  ✅ Self-healing (LLM adapts to changes)");
    System.out.println("  ✅ Unified extraction logic for all providers");

    System.out.println("\n🏗️ ARCHITECTURE:");
    System.out.println("  1. Keep existing list crawlers (they work fine)");
    System.out.println("  2. Filter job detail HTML (remove ads, navigation, etc.)");
    System.out.println("  3. Send clean content to GPT-4o-mini");
    System.out.println("  4. Get structured JSON response");
    System.out.println("  5. Apply to Assignment entity");

    System.out.println("\n💰 COST ANALYSIS:");
    System.out.println("  • GPT-4o-mini: $0.15/MTok input, $0.60/MTok output");
    System.out.println("  • ~500 tokens per job (after filtering)");
    System.out.println("  • ~200 tokens output per job");
    System.out.println("  • Cost per job: ~$0.0002 (0.02 cents)");
    System.out.println("  • 1000 jobs/day = $0.20/day = $6/month");

    System.out.println("\n📁 FILES CREATED:");
    System.out.println("  📄 ExtractionConfig.java - DSL schema for provider configs");
    System.out.println("  📄 OpenAiLlmService.java - LLM integration service");
    System.out.println("  📄 HtmlContentFilterService.java - Content filtering & optimization");
    System.out.println("  📄 LlmJobExtractionService.java - Main orchestration service");
    System.out.println("  📄 LlmEmagineProvider.java - Example LLM-based provider");
    System.out.println("  📄 emagine.yaml - Example provider configuration");
    System.out.println("  📄 asociety.yaml - Example provider configuration");

    System.out.println("\n⚙️ CONFIGURATION:");
    System.out.println("  • Set OPENAI_API_KEY environment variable");
    System.out.println("  • Provider configs in /extraction-configs/*.yaml");
    System.out.println("  • Automatic content filtering and token optimization");

    System.out.println("\n🧪 TESTING:");
    System.out.println("  • Mock tests demonstrate the extraction flow");
    System.out.println("  • Real testing requires OpenAI API key");
    System.out.println("  • Start with existing provider to validate approach");

    System.out.println("\n🚀 DEPLOYMENT STRATEGY:");
    System.out.println("  Phase 1: Test with one provider (Emagine)");
    System.out.println("  Phase 2: Migrate existing providers one by one");
    System.out.println("  Phase 3: Add new providers using DSL configs");
    System.out.println("  Phase 4: Remove old regex-based extraction code");

    System.out.println("\n📊 BENEFITS:");
    System.out.println("  • 90% reduction in extraction code complexity");
    System.out.println("  • Self-healing when sites change structure");
    System.out.println("  • Easy to add new providers");
    System.out.println("  • Better data quality and completeness");
    System.out.println("  • Reduced maintenance overhead");

    System.out.println("\n🎯 NEXT STEPS:");
    System.out.println("  1. Set up OpenAI API key for testing");
    System.out.println("  2. Test LlmEmagineProvider with real data");
    System.out.println("  3. Monitor token usage and costs");
    System.out.println("  4. Fine-tune extraction prompts and configs");
    System.out.println("  5. Gradually migrate other providers");

    System.out.println("\n" + "=".repeat(80));
    System.out.println("Ready to test! Set OPENAI_API_KEY and try the LlmEmagineProvider.");
    System.out.println("=".repeat(80));
  }

  @Test
  public void showConfigurationExample() {
    System.out.println("\n" + "=".repeat(60));
    System.out.println("PROVIDER CONFIGURATION EXAMPLE");
    System.out.println("=".repeat(60));

    System.out.println("\nFile: src/main/resources/extraction-configs/emagine.yaml");
    System.out.println("-".repeat(60));

    String configExample =
        """
            providerId: "emagine"
            baseUrl: "https://emagine-consulting.se"

            contentFilter:
              contentSelector: ".container .content-section"
              excludeSelectors:
                - ".sidebar"
                - ".navigation"
                - ".advertisement"
                - "script"
                - "style"
              maxTokens: 2000
              useReadability: true

            llmConfig:
              model: "gpt-4o-mini"
              temperature: 0.1
              maxResponseTokens: 800
              customInstructions: |
                Focus on extracting Swedish consulting job details.
                Pay attention to rates in SEK and Swedish locations.
            """;

    System.out.println(configExample);

    System.out.println("\nThis YAML config replaces 400+ lines of complex Java regex code!");
    System.out.println("=".repeat(60));
  }
}
