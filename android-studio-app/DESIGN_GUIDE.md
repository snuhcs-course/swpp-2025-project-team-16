# FitQuest Android Design Guide

## Core Concept: RPG Growth System

Transform bodyweight training into an RPG-style character progression experience where every exercise earns XP, unlocks levels, and builds your hero.

## Color Palette

### Primary Colors
```xml
<!-- colors.xml -->
<color name="deep_blue">#0D47A1</color>          <!-- Primary background, trust, focus -->
<color name="gold">#FFD700</color>                <!-- Rewards, achievements, XP -->
<color name="electric_cyan">#00E5FF</color>       <!-- Accents, HUD effects, highlights -->

<!-- Supporting Colors -->
<color name="slate_900">#0f172a</color>           <!-- Dark backgrounds -->
<color name="slate_800">#1e293b</color>           <!-- Card backgrounds -->
<color name="slate_700">#334155</color>           <!-- Borders, dividers -->
<color name="blue_950">#172554</color>            <!-- Deep blue backgrounds -->
```

### Gradients
- **Primary Action:** Deep Blue → Electric Cyan
- **Rewards/XP:** Gold → Yellow
- **Success:** Green → Emerald
- **Danger:** Orange → Red
- **Background:** Slate 900 → Blue 950 → Slate 900

## Typography

### Text Hierarchy
- **Hero Title (h1):** 32sp, White, Bold
- **Section Title (h2):** 24sp, White, SemiBold  
- **Card Title (h3):** 18sp, White, Medium
- **Body Text (h4):** 16sp, White/Cyan 400, Regular
- **Caption:** 14sp, Slate 400/Cyan 400, Regular
- **Small Text:** 12sp, Slate 500, Regular

### Text Colors
- Primary: White (#FFFFFF)
- Secondary: Cyan 400 (#22D3EE)
- Tertiary: Slate 400 (#94A3B8)
- Accent: Gold (#FFD700)

## Component Styles

### Cards
```kotlin
// Standard Card
- Background: Slate 800/50 with backdrop blur
- Border: 2dp, Cyan 500/20
- Corner Radius: 16dp
- Padding: 16dp
- Shadow: Medium, Cyan 500/10

// Highlighted Card  
- Background: Gradient (Blue 600 → Cyan 500)
- Border: 2dp, Cyan 400
- Corner Radius: 16dp
- Shadow: Large, Cyan 500/30
```

### Buttons

```kotlin
// Primary Button
- Background: Gradient (Blue 600 → Cyan 500)
- Text: White, 16sp, SemiBold
- Height: 48dp
- Corner Radius: 12dp
- Shadow: Medium, Cyan 500/20

// Secondary Button
- Background: Slate 800/50
- Border: 2dp, Cyan 500/30
- Text: Cyan 400, 16sp, Medium
- Height: 48dp
- Corner Radius: 12dp

// Success Button
- Background: Gradient (Green 600 → Emerald 500)
- Text: White, 16sp, SemiBold
```

### Progress Bars
```kotlin
// XP Progress Bar
- Height: 8dp
- Background: Slate 800
- Fill: Gradient (Cyan 500 → Blue 600)
- Corner Radius: 4dp
- Animated glow effect

// Form Quality Bar
- Height: 8dp
- Background: Slate 800
- Fill: Based on score
  - 90-100%: Green → Emerald
  - 70-89%: Yellow → Gold
  - Below 70%: Orange → Red
```

### Badges
```kotlin
// Level Badge
- Background: Gradient (Blue 600 → Cyan 500)
- Text: White, 12sp, Bold
- Padding: 4dp horizontal, 2dp vertical
- Corner Radius: 8dp

// XP Badge
- Background: Gold/20
- Border: 1dp, Gold/50
- Text: Gold, 12sp, Medium
- Icon: Trophy (Gold)

// Streak Badge
- Background: Orange/20
- Border: 1dp, Orange/50
- Text: Orange, 12sp, Medium
- Icon: Flame
```

## Screen-Specific Design

### Login Flow
- **Background:** Dark gradient with animated cyan/blue orbs
- **Cards:** Slate 800/90 with backdrop blur
- **Inputs:** Slate 900/50 bg, Slate 600 border, Cyan 500 focus
- **Icons:** Cyan 400
- **Class Selection:** Cards with icon + name + description, selected card glows

### Journey Screen (Quest Path)
- **Header:** Blue 900/50 gradient with border
- **Path:** Vertical cyan glowing line (1dp width, shadow glow)
- **Waypoints:** 
  - Circular markers (32dp)
  - Today: Cyan gradient with pulse animation
  - Future: Blue with cyan border
  - Past: Slate with muted border
- **Quest Cards:** Slate 800/50, cyan border, rounded

### Profile Screen (Hero Stats)
- **Compact Header:**
  - Avatar: 64dp, gradient background
  - Level + XP badges inline
  - Streak indicator (flame icon + count)
  - XP progress bar below
  - 4-column stats grid (compact)
- **Main Content (80%):** Victory road
  - Gold glowing path line
  - Trophy markers for completed workouts
  - Cards show: date, exercises (emojis), XP, score
  - Alternating left/right layout

### Schedule Screen (Training Planner)
- **Calendar:** Custom theme with cyan selection
- **Quick Actions:** 2-column grid
  - AI Generate: Sparkles icon, blue/cyan gradient
  - Custom: Edit icon, slate with cyan border
- **Exercise Library:** Grid layout (2 columns)
  - Cards with emoji + name
  - Slate background, green border on hover
- **Scheduled List:** Vertical cards with emoji icon

### AI Coach Screen (Training System)
- **Camera View:** Full aspect ratio with dark overlay
- **HUD Elements:**
  - Rep Counter: Gold/yellow card, top-left, large number
  - XP Counter: Blue/cyan gradient, below rep counter
  - Form Bar: Bottom overlay, progress indicator
  - Recording: Top-right, pulsing red dot
- **Feedback Card:** Blue/cyan gradient, coach icon
- **Controls:** Full-width buttons at bottom

## Animations

### Standard Transitions
- Screen transitions: Fade + slight horizontal slide (300ms)
- Card appearance: Scale up + fade in (200ms, staggered)
- Button press: Scale down to 0.95 (100ms)

### Special Effects
- **XP Gain:** Scale pulse (1 → 1.1 → 1) on increment
- **Level Up:** Particle burst effect, card glow
- **Streak Counter:** Flame flicker animation
- **Waypoint Markers:** Rotate + scale in on load
- **Recording Indicator:** Opacity pulse (1 → 0.5 → 1, infinite)
- **Progress Bars:** Shimmer/glow effect on fill

## Icons

Use Material Icons with these specific variants:
- **Journey:** Map / Navigation
- **Schedule:** Calendar / Event
- **AI Coach:** Activity / Zap
- **Profile:** User / Shield
- **XP/Rewards:** Trophy / Star / Award
- **Streak:** Flame / Fire
- **Training:** Dumbbell / Fitness Center
- **AI:** Brain / Cpu / Activity

## Shadows & Elevation

```kotlin
// Card elevation
- Standard: 4dp
- Highlighted: 8dp with colored shadow
- Floating: 12dp

// Shadow colors
- Standard: Black/10
- Cyan glow: Cyan 500/20
- Gold glow: Gold/30
```

## Implementation Notes

1. **Material Design 3:** Use MD3 components as base, heavily customize colors
2. **Dark Theme:** All screens use dark theme by default
3. **Gradients:** Implement using GradientDrawable or XML drawables
4. **Animations:** Use MotionLayout for complex transitions
5. **Glows:** Implement using elevation with colored shadows or custom drawables
6. **Backdrop Blur:** Use RenderEffect (API 31+) or fallback to semi-transparent overlays

## Accessibility

- Minimum contrast ratio: 4.5:1 for text
- Touch targets: Minimum 48dp
- Semantic labels for all interactive elements
- Support for TalkBack
- Adjustable text sizes (sp units)
