# Design, motion and localization

Brand: 听见 / Castflow. Approved icon is the red glass podcast broadcaster, diagonal lower-right to upper-left, elevated 3D viewpoint. Source/export images and design notes are in design/icon-concepts; the bundled full-bleed image is used by the adaptive launcher icon. Generated with an image tool, then integrated as Android resources; no third-party app icon files were copied.

Content is readable and spacious; navigation/control layers use Haze backdrop blur, translucent tint, rounded surfaces and restrained shadows. Share button feedback rather than introducing stock rectangular ripple artifacts. Full-player transitions are nonlinear without bounce. Artwork remains visible while moving to the mini-player position. MotionTokens contains project-tuned parameters, not official vendor specifications.

Appearance follows the system by default. Settings also allows a manual light, dark-gray, or OLED-black choice. Light keeps the existing Castflow palette; both dark variants use fixed project colors rather than Material You dynamic color. Manual changes use a restrained 520 ms diagonal color sweep from the upper-left, swap the composed page underneath the fully covered surface, then fade an accent-tinted incoming-color veil over 480 ms. Persisted UI state keys remain stable and theme preferences stay local to the installed app.

Tabs own separate lazy-list state, and page content is gated by its navigation key so a new tab cannot inherit the previous tab's rows or scroll offset. Tab changes avoid vertical translation; deeper show/category layers retain a short settle.

Page-depth changes use a restrained fade and vertical settle, including search categories, show details, Home and Discovery. Main scrolling surfaces draw edge to edge behind the transparent status bar; keep the initial safe top as scrolling content padding instead of a fixed white inset.

Long full-player titles stay at two lines when they fit. Overflow titles use a one-line slow marquee instead of permanently hiding the remainder. Artwork responds to playback state with a restrained center-anchored scale, and the title/action row uses optical rather than equal-gap centering.

Read RoundedCorner radii and navigation/safe-drawing insets; keep bottom controls away from large physical corners even with a hidden gesture indicator. Status-bar appearance must observe actual layer coverage, with Compose state read during composition or snapshotFlow. Do not read animation state only inside SideEffect and assume it triggers recomposition.

English defaults: values/strings.xml. Chinese: values-zh and values-b+zh+Hant. Traditional locale resources currently contain the same Chinese wording as the simplified set. Keep all keys synchronized. LocalizedComponents/UiStrings preserve persisted navigation IDs. Do not translate publisher names or episode titles. Test long English labels and accessibility descriptions. Language-scoped cache prevents stale Chinese headings after switching to English.

Research references (study only):
- https://github.com/Zennmn/AM-plus-plus
- https://github.com/NEORUAA/MeiloX
- https://github.com/NEORUAA/Glasmio
- https://github.com/youshen2/MeloX
- https://developer.android.com/develop/ui/views/layout/insets/rounded-corners
- https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back

Vendor demonstrations are not a published set of reusable timing constants. Compatibility with every navigation mode, font scale and device remains a test obligation.
