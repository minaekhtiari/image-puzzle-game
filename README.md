# Donald Duck Puzzle

A single screen sliding puzzle built with Jetpack Compose, Hilt, Coroutines, and Coil.

---

## Architecture decisions

I kept the architecture lightweight for this scope while still separating responsibilities clearly.

- The ViewModel owns puzzle state and game progress
- The repository handles data access
- Image loading and slicing are separated behind their own interfaces (`PuzzleImageLoader`, `PuzzleImageSlicer`)

This keeps the ViewModel mostly independent from framework specific code and makes the core logic easier to test.

I used a sealed UI state (`Loading`, `Error`, `Playing`, `Solved`) to keep rendering simple and avoid inconsistent states.

I also chose a layer based package structure since the app only contains a single feature. For a project of this size, feature-based packaging felt unnecessary.

---

## Key implementation choices

**Connected groups**
When neighboring tiles are already in the correct order, they move together as a group during drag.
Instead of creating merged bitmaps, I simply remove the spacing and corner radius between connected tiles so they visually appear as one block. This keeps rendering simpler and avoids extra bitmap work.

**Screen sizes**
The board size is calculated from the available screen width using `BoxWithConstraints`, and tile height is derived from that.
A minimum tile height is enforced so the puzzle remains usable on smaller or unusual screen sizes, including foldables.

**Drag state**
Drag related state (`draggedGroupRange`, `dragOffsetY`) is kept locally in Compose state instead of inside the ViewModel since it only matters during interaction.
An active drag is cancelled on rotation, while the puzzle state and loaded image survive through the ViewModel.

**Image loading**
Hardware bitmaps are disabled (`allowHardware(false)`) so the bitmap can be sliced on the CPU.
The puzzle screen becomes visible before image loading finishes, so the UI appears immediately while the image continues loading in the background.
I relied on Coil's built in memory and disk cache since the app only loads a single image.

---

## Testing

`PuzzleViewModelTest` covers the main state transitions and public ViewModel behavior using:

- `StandardTestDispatcher`
- `advanceUntilIdle()`
- Turbine for `StateFlow`
- MockK for mocks/stubs

`PuzzleGameEngineTest` focuses on the puzzle mechanics such as solved state detection, connected groups, and swap behavior.

All tests run as local JVM tests.

## AI usage

AI tools were occasionally used during development for brainstorming and discussing implementation ideas.
