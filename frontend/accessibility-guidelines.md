# Swedish Web Accessibility Guidelines

This document summarizes key accessibility guidelines for frontend development in Sweden, with a focus on interactive elements like buttons and forms.

## Button Placement and Styling

### Primary vs Secondary Actions

Based on research of Swedish accessibility guidelines and WCAG standards:

- While there is no explicit requirement to position primary buttons on the left or right, consistency is key
- Primary actions should be visually distinct from secondary actions
- The Swedish convention typically follows:
  - Primary action buttons should be more prominent (filled background)
  - Secondary action buttons should be less prominent (outlined, ghost, or text-only)
  - Maintain reasonable spacing between buttons when grouped together (at least 8px)

### Button Accessibility Requirements

- **Size**: Clickable areas should be at least 24x24 pixels (WCAG 2.5.8)
- **Contrast**: Maintain a contrast ratio of at least 3:1 between the button and its surroundings
- **Text contrast**: Text on buttons should have 4.5:1 contrast ratio with the button background
- **Focus state**: All interactive elements must have a visible focus indicator
- **Naming**: Use clear action verbs on buttons (e.g., "Save", "Cancel")
- **Consistency**: Maintain consistent button styling and placement throughout the application

## Form Design Guidelines

- Buttons that advance to the next step should be clearly marked
- Avoid "clear" buttons unless necessary (risk of accidental data loss)
- Form navigation buttons should be labeled "Previous" and "Next"
- Cancel buttons should terminate the entire flow, not just return to the previous page

## Language and Text

- Button text should match the machine-readable name (for voice control and screen readers)
- Use verbs to clearly indicate the action (e.g., "Save Changes" instead of "Changes")
- Text should be concise but descriptive

## Layout and Responsive Design

- Content and functions should work regardless of screen orientation
- Create flexible layouts that adapt to screen size and zoom levels
- Ensure adequate spacing between interactive elements

## Implementation for Our Application

For our application, we follow these conventions:

1. **Primary action** (like "Save Changes"): 
   - Filled button with accent color background
   - Positioned on the right in button groups

2. **Secondary action** (like "Back to Search"): 
   - Outlined button with accent color border
   - Positioned on the left in button groups

3. **Destructive action** (like "Delete"):
   - Use warning/error color 
   - Consider additional confirmation

4. **Button grouping**:
   - Use consistent spacing between buttons (16px)
   - Align left for page/section actions
   - Align center for modal/dialog actions
   - Align right for form submission actions

## References

- [DIGG (Swedish Agency for Digital Government)](https://www.digg.se/webbriktlinjer)
- [Web Content Accessibility Guidelines (WCAG) 2.2](https://www.w3.org/TR/WCAG22/)
- European Accessibility Act and EN 301 549 standards