# kit721-assignment2
## Hasini Gayathri De Silva Andra Hannadhi - 771480
Interior Quoter — KIT721 Assignment 2


---

## Device & Orientation
- **Device:** Medium Phone API 37.0 (Android Emulator)
- **Orientation:** Portrait only

---

## Activities & Navigation

| Activity | Description |
|----------|-------------|
| `MainActivity` | Home screen with house list, search bar, and empty state |
| `AddHouseActivity` | Add or edit a house (doubles as edit screen when HOUSE_ID passed) |
| `HouseDetailActivity` | Room list for a selected house with Generate Quote button |
| `AddEditRoomActivity` | Add or edit a room with optional photo picker |
| `RoomDetailActivity` | Windows and floor spaces for a room with notes |
| `AddEditWindowActivity` | Add or edit a window with product API and 4 constraints |
| `AddEditFloorActivity` | Add or edit a floor space with product API |
| `QuoteActivity` | Itemised quote display with totals and OS share |

### Navigation Flow

### Navigation Flow
- **MainActivity** → AddHouseActivity (tap + Add House or edit icon)
- **MainActivity** → HouseDetailActivity (tap house card)
- **HouseDetailActivity** → AddEditRoomActivity (tap + Add Room or edit icon)
- **HouseDetailActivity** → QuoteActivity (tap Generate Quote)
- **HouseDetailActivity** → RoomDetailActivity (tap room card)
- **RoomDetailActivity** → AddEditWindowActivity (tap + Add next to Windows)
- **RoomDetailActivity** → AddEditFloorActivity (tap + Add next to Floor Spaces)


---

## Custom Feature
**Room Duplication** — Long press any room card in the room list to duplicate it. A confirmation dialog shows the new room name preview. On confirm, the room is copied with all its windows and floor spaces to a new Firestore document. The duplicated room appears in the list named "[Original Name] (Copy)".

---

## References

### Tutorials
- KIT305/KIT721 Tutorial 5 — RecyclerView and Firebase Firestore pattern used as base for all list screens and adapters

### Libraries
- Firebase Firestore — https://firebase.google.com/docs/firestore
- Glide image loading library — https://bumptech.github.io/glide/ (used only for photo display as per assignment spec)

### External API
- Product API: https://utasbot.dev/kit305_2026/product — provided by unit coordinator


### Official Android Documentation
- Sharing simple data (Intent.ACTION_SEND) —
  https://developer.android.com/training/sharing/send
- Photo picker —
  https://developer.android.com/training/data-storage/shared/photo-picker
- Save files on device internal storage —
  https://developer.android.com/training/data-storage

### Stack Overflow
- Copying Firestore documents to new collection —
  https://stackoverflow.com/questions/59660058/selectively-copy-data-from-one-firestore-collection-to-another
- Save images to internal file storage in Android —
  https://stackoverflow.com/questions/12559974/save-images-from-drawable-to-internal-file-storage-in-android
- How to know which intent is selected in Intent.ACTION_SEND —
  https://stackoverflow.com/questions/7495909/how-to-know-which-intent-is-selected-in-intent-action-send
### Generative AI Usage
Claude AI (Anthropic Claude Sonnet) was used as a development assistant for parts of
this assignment that go beyond the tutorial content covered in KIT305/KIT721.

For the following areas, Claude was used for debugging, understanding workflow concepts,
and syntax guidance — not for copying complete code solutions:

- **Window constraint logic** — Understanding panel splitting algorithm approach
- **Quote calculation** — Debugging data aggregation across rooms, windows and floor spaces
- **Room duplication** — Understanding Firestore document copying workflow
- **Local photo storage** — Debugging gallery image saving to local storage
- **XML layout structure** — Complex nested layouts (ScrollView, CardView combinations)
- **Share functionality** — Intent.ACTION_SEND implementation for quote sharing

Areas completed independently using tutorial knowledge and prior experience:
- Firebase Firestore setup, connection and CRUD operations
- RecyclerView adapter pattern (Tutorial 5 pattern)
- Activity navigation with Intents
- View Binding setup
- Data classes (House, Room, WindowSpace, FloorSpace)
- REST API integration and JSON parsing (prior backend engineering experience)
- Cascade delete logic (prior backend engineering experience)
- XML layout design fundamentals

**Shared AI conversation link:** https://claude.ai/chat/87181588-3865-4609-ad9f-22d4923e2ca1

---

## Notes
- All data in Firestore is sensible test data with real-looking names and addresses
- App tested on Medium Phone API 37.0 emulator in portrait orientation
- App does not crash on empty database