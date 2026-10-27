---
name: NutriTrack
description: Reference-led nutrition tracking portal
colors:
  green: "#216d4f"
  ink: "#202722"
  muted: "#626860"
  line: "#e0e3dc"
  soft: "#f4f5f2"
  white: "#fff"
  green-hover: "#18563e"
  nav-hover: "#e6ebe4"
  secondary-hover: "#edf1e9"
  progress-track: "#e4e6e0"
  chart-off: "#ced1c9"
  notice-background: "#edf3ed"
  notice-border: "#b8cdbc"
  selection: "#d3e8d8"
typography:
  display:
    fontFamily: '"Segoe UI", Arial, sans-serif'
    fontSize: "64px"
    fontWeight: 650
    lineHeight: 1.3
    letterSpacing: "-2px"
  headline:
    fontFamily: '"Segoe UI", Arial, sans-serif'
    fontSize: "25px"
    fontWeight: 700
    lineHeight: 1.2
    letterSpacing: "-.6px"
  title:
    fontFamily: '"Segoe UI", Arial, sans-serif'
    fontSize: "14px"
    fontWeight: 650
    lineHeight: 1.55
  body:
    fontFamily: '"Segoe UI", Arial, sans-serif'
    fontSize: "14px"
    fontWeight: 400
    lineHeight: 1.55
  label:
    fontFamily: '"Segoe UI", Arial, sans-serif'
    fontSize: "12px"
    fontWeight: 600
    lineHeight: 1.55
rounded:
  control: "5px"
  panel: "8px"
  login: "9px"
spacing:
  compact-gap: "10px"
  form-gap: "14px"
  metric-gap: "16px"
  panel-gap: "18px"
  section-gap: "20px"
  panel-padding: "22px"
components:
  button-primary:
    backgroundColor: "{colors.green}"
    textColor: "{colors.white}"
    typography: "{typography.label}"
    rounded: "{rounded.control}"
    padding: "9px 15px"
  button-primary-hover:
    backgroundColor: "{colors.green-hover}"
  button-secondary:
    backgroundColor: "{colors.white}"
    textColor: "{colors.ink}"
    rounded: "{rounded.control}"
    padding: "9px 15px"
  card:
    backgroundColor: "{colors.white}"
    rounded: "{rounded.panel}"
    padding: "22px"
  input:
    backgroundColor: "{colors.white}"
    textColor: "{colors.ink}"
    rounded: "{rounded.control}"
    padding: "9px 10px"
  nav-active:
    backgroundColor: "{colors.green}"
    textColor: "{colors.white}"
    rounded: "{rounded.control}"
    padding: "9px 11px"
---

# Design System: NutriTrack

## Overview

**Creative North Star: "The NutriTrack reference portal"**

Preserve the supplied NutriTrack report's interface: a pale grey sidebar, white panels, dark green controls, compact tables, nutrient cards and weekly bars. The visual system is quiet and utilitarian, with borders and tonal surfaces organizing dense nutritional data.

**Key Characteristics:**

- Grey navigation and recommendation surfaces
- Green actions and progress indicators
- Compact system typography and tabular numbers

Source authority: user-supplied report, with pages 7–9 rendered outside the deliverable at task `work/page-7.png` through `page-9.png`; durable commitments in `PRODUCT.md`; surface direction in `.impeccable/surfaces/portal.md`. Exact implemented values are extracted from `src/main/webapp/assets/app.css`. This is a documentation pass, not a redesign.

Evidence: `screenshots/desktop.png` (1264 × 807) and `screenshots/mobile.png` (390 × 1469) were visually inspected for this document. These are application captures, not design assets. The surface record also identifies `log-meal.png` and `weekly.png` as local captures. No generated raster assets ship. The parent implementation review reports mobile grid sizing and table totals fixes complete. Automated design detector verification was unavailable because its helper engine was not installed.

## Colors

### Primary

Forest green identifies primary actions, the active navigation item, progress fills, the score and positive nutrient contributions. Its darker hover color supplies immediate interaction feedback.

### Neutral

Ink carries body copy. Muted grey carries labels, units and supporting text. White is the page and card surface; pale grey distinguishes navigation and the gap panel. Fine grey borders separate regions. Grey chart bars identify off-target values. Notice colors are a pale green background and muted green border.

**The Reference Rule.** Preserve the grey, white and green palette established by the supplied report.

## Typography

Segoe UI with Arial and sans-serif fallbacks is the portal's system stack. The score uses the display role; page headings use the headline role. Metric numbers are 28px with −1px tracking, reduced to 26px on mobile. Panel headings use the title role; labels and buttons use the label role.

Metric and section captions use uppercase 10px text with 1px tracking. Tables use 12px text and tabular numerals; column headings use uppercase 10px with .6px tracking. Supporting copy ranges from 10px to 12px. The brand is 21px at weight 750 with −.6px tracking. Login introductory copy alone uses Georgia at 15px/1.6; source-code references use Consolas at 11px/1.7. No downloaded webfont is required.

## Layout

The desktop sidebar is fixed at 210px wide, padded 28px vertically and 18px horizontally; the main area offsets by its width. The header is 100px high with 24px 36px padding. Content is centered with a 1500px maximum width and 28px 36px padding.

The dashboard has four equal nutrient tracks, then a 1:2 score/recommendation split. Panels use the documented padding and gaps. Main content, cards and grid containers have zero minimum width to prevent intrinsic content overflow.

At widths up to 1100px the sidebar and main offset become 180px, content padding is 24px, metric padding is 16px and metric gaps are 10px. Meal forms use two columns and admin forms three.

At widths up to 700px the sidebar joins normal document flow with 18px padding, navigation becomes a horizontally scrollable row, and the main offset disappears. The header is 85px high with 22px 18px padding; content uses 18px. Metrics occupy two `minmax(0,1fr)` tracks, and the lower dashboard stacks into one. Panel padding is 17px. Forms use two columns with the food selector spanning both; inline control rows wrap. Tables retain horizontal scrolling; the project-review table has a 760px minimum width. The footer stacks.

## Elevation & Depth

Borders and pale surface fills create hierarchy. Cards have no shadow. The selected login role is the sole shadow treatment: `0 1px 3px #0002`. There are no implemented transitions or animations.

## Shapes

Controls and navigation use gently rounded corners from the control token. Cards use the panel radius; the login card uses its dedicated radius. Borders are generally 1px. Progress bars are 5px high with rounded ends. Weekly bars round only their top corners and have a minimum visible height of 2px.

## Components

### Buttons

Primary and secondary actions use compact label typography and the control radius. Primary hover darkens green; secondary hover adds a pale green-grey surface. Small actions use 11px type and 6px 10px padding. Ghost logout uses muted text and no filled surface. Disabled buttons use .45 opacity and a not-allowed cursor.

### Cards and nutrient indicators

White bordered metric cards place an uppercase label above a bold number, adjacent unit/goal, slim progress track and supporting status. The score uses the display role with a smaller muted denominator. Recommendation panels use the pale grey surface, numbered rows with dividers, and right-aligned green contributions.

### Inputs and feedback

White text and select fields use a fine border, control radius and 13px type. Labels stack with a 6px gap. Placeholders use muted grey-green and carets use green. Focus-visible receives a 2px green outline; interactive controls use a 4px outline offset. Notices use a pale green fill, muted green border and 13px type. No separate CSS error variant is implemented.

### Navigation

Links use 13px type and compact padding. Hover adds a pale grey-green background; active state uses white text on green at weight 600. Navigation remains reachable by horizontal scrolling on mobile. Logout sits at the bottom on desktop and top right on mobile.

### Tables

Numerical columns align right and use tabular figures. Meal-group rows use the pale grey background with semibold text. Footer totals have a stronger 2px divider and 14px 10px cell padding. Empty states are centered, muted and padded 34px.

### Weekly chart and login roles

Weekly bars use green for on-target days and grey otherwise, with a dashed goal line, value labels above and day labels below. The chart is 260px high. Bars are 48px wide on desktop and 32px on mobile. The login role toggle uses two radio choices; selection creates a white inset surface and the documented small shadow. Keyboard focus outlines the visible role surface.

## Do's and Don'ts

### Do:

- Do preserve the user's reference palette and restrained panel layout.
- Do keep numeric columns right-aligned and totals visibly grouped.
- Do use flexible minimum-zero grid tracks and horizontally scrollable tables on narrow screens.

### Don't:

- Don't introduce a new visual identity, decorative illustrations or generated image assets.
- Don't replace the mobile scrollable navigation with a different interaction without a product decision.
- Don't claim automated detector verification; its helper engine was unavailable.


## Custom entry extension

The Project review navigation/page is removed. Log meal now includes an expandable custom-entry panel using the existing green/grey style. A four-column form becomes two columns on phones. Nutrition is labeled as consumed totals. The optional JPG/PNG photo input supports preview; saved thumbnails link to an authenticated photo route. Evidence: screenshots/custom-entry.png and screenshots/custom-mobile.png.
