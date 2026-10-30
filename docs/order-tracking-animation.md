# RushIt order-tracking animation

This feature keeps the original RushIt tracking layout (cream background, coral actions, demo
notice, rounded illustration container, horizontal progress, status card, and bottom action row)
and replaces the previous route-map / procedural-rider travel scenes with layered artwork from the
supplied references.

## Scene mapping

| Order status | Scene | Progress |
| --- | --- | --- |
| PENDING | Waiting for confirmation | — |
| CONFIRMED | Order confirmed | Confirmed |
| PREPARING | Packing your order | Preparing |
| READY_FOR_PICKUP | Rider ready (demo) / waiting for rider (real) | Preparing |
| PICKED_UP | Order picked up | On the way |
| OUT_FOR_DELIVERY | On the way | On the way |
| ARRIVING | Almost there | Almost there |
| DELIVERED | Delivered | Delivered |
| CANCELLED / FAILED | Terminal message | hidden |

`OrderStatus` remains the single source of truth. Animations never write status. Demo preview is
the only local status driver; real orders cannot be advanced or cancelled from this screen.

## Animation architecture

- `OrderJourneyScreen` owns the shared layout and a hoisted `TravelMotionState`.
- `rememberTravelMotion` advances a normalized distance with `withFrameNanos` only while the
  screen is resumed and a travel scene is active. On the Way → Almost There reuses the same
  distance/phase and eases speed to 45%.
- `RiderTravelScene` draws parallax strips (skyline 0.18×, trees 0.45×, road 1×), rotates the
  supplied wheel bitmap around the rear/front hub pivots, then draws the rider/scooter body with
  transparent hub holes so the wheels sit behind the exhaust/fork occluders.
- `RiderGeometry` and `TravelMotionTest` keep pivots and rolling distance testable.
- Confirmation, packing, pickup, and delivered one-shots are keyed by order id and snap to their
  final state when reduced motion is enabled.
- The action row lives outside `AnimatedContent`, so crossfades never duplicate buttons or block
  taps.

## Asset provenance

Supplied files inspected:

- `20d1950fe6c823aac2351cb9680ed19f917d1fb047b1553a4b21ecc7b148f21a.png` — RGBA, real alpha
  (0–254), 1536×1024 rider/scooter board.
- `file_000000000b648211a31c4c3c56386f4b.png` — RGB storyboard, 1536×1024.

Prepared lossless WebP assets in `app/src/main/res/drawable-nodpi/`:

- `order_rider_static`, `order_rider_travel`, `order_wheel` — extracted from the top-left rider and
  the standalone wheel on the supplied board. The travel variant removes only the hub discs and
  preserves tyre rings, exhaust, and fork occluders.
- `order_confirmation_bg`, `order_confirmation_groceries_left/right` — original confirmation
  illustration with the checkmark and grocery groups separated; background gaps inpainted.
- `order_packing`, `order_packing_bag`, `order_packing_item_*` — original packing illustration with
  bag and floating grocery discs separated; background gaps inpainted.
- `order_rider_ready` — original storefront with the rider region inpainted so the supplied rider
  can be composited consistently.
- `order_pickup` — original combined employee/rider handover illustration (see limitations).
- `order_travel_bg/skyline/trees/road` — original travel background with the rider inpainted, then
  sliced into parallax bands.
- `order_delivered_bg`, `order_handover`, `order_scooter_parked` — original doorway background with
  people/scooter inpainted, plus the supplied standing handover pair and standalone scooter body.

RushIt branding on the delivery bag is preserved. No white/checkerboard background was removed from
inside characters; the board alpha was inspected before extraction.

## Known limitations

- The supplied pickup illustration is a combined employee + rider + parcel image. Reliable separate
  parcel/employee/rider layers were not available, so pickup uses a restrained fade-in rather than a
  fake articulated handover.
- The supplied walking poses were not used because the tracking journey travels by scooter; no walk
  cycle was fabricated from a single pose.
- Head/torso/arms were not split into independent puppets; the supplied seated rider is kept intact
  to avoid distortion.
- Parallax bands are sliced from the original combined background, not hand-painted separate layers;
  wrap seams are possible at the strip edges.
- No real GPS, ETA, rider identity, or delivery guarantee is implied. Delivered is shown only when
  the stored order status is DELIVERED.

## Validation

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
./gradlew :app:connectedDebugAndroidTest   # requires an emulator/device
```

The temporary `Verify order tracking` workflow runs unit tests, debug build, lint, and Compose UI
tests on an API 34 emulator for this branch.
