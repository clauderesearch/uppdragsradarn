/**
 * Security Headers Middleware
 * 
 * This middleware adds important security headers to all responses
 * to protect against common web vulnerabilities.
 */
export default defineEventHandler((event) => {
  // Content Security Policy
  // Customize this policy based on your specific needs
  const cspHeader = [
    "default-src 'self'",
    "script-src 'self' 'unsafe-inline' 'unsafe-eval'", // Consider restricting 'unsafe-inline' and 'unsafe-eval' in production
    "style-src 'self' 'unsafe-inline'",
    "img-src 'self' data: blob:",
    "font-src 'self'",
    "connect-src 'self' http://localhost https://*.amazonaws.com https://dev.uppdragsradarn.se", // Allow API connections and AWS for authentication
    "frame-src 'none'",
    "object-src 'none'"
  ].join("; ");

  // Set security headers
  setResponseHeaders(event, {
    // Prevents MIME-sniffing (where browser tries to guess the content type)
    'X-Content-Type-Options': 'nosniff',

    // Controls how much information the browser includes with referer header
    'Referrer-Policy': 'strict-origin-when-cross-origin',

    // Helps prevent XSS attacks by blocking reflected XSS
    'X-XSS-Protection': '1; mode=block',

    // Controls which features/APIs can be used in the browser
    'Permissions-Policy': "camera=(), microphone=(), geolocation=()",

    // Prevents clickjacking attacks by ensuring page can only be displayed in appropriate frames
    'X-Frame-Options': 'SAMEORIGIN',

    // Content Security Policy - customized based on your specific requirements
    'Content-Security-Policy': cspHeader,

    // Enables HTTP Strict Transport Security - uncomment for production
    // 'Strict-Transport-Security': 'max-age=31536000; includeSubDomains',

    // Remove X-Powered-By header to avoid leaking technology information
    'X-Powered-By': null
  });
});