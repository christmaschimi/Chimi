# Chimi's Menu (Fabric, Minecraft 26.2)

Client-side mod. Press G to open the menu.

- Auction: pick the item from your inventory, set any duration (seconds or presets), start. Incoming "X paid you AMOUNT" messages count as bids; highest total wins.
- Profit: live HUD showing your real balance change this session. Reads the balance line on the right-side scoreboard and your /bal replies. DonutSMP API is optional (useApi in config, off by default).
  Type /api in game; the key is captured automatically or paste it in the Profit tab.
- Sky: preset selection (rendering hook not added yet).
- Inventory: add an image from your PC (Add image button or drag a file onto the menu) as your inventory background; darkness and scope options.
- Customize: accent color, panel opacity, HUD corner. Saved to config/chimismenu.json.

Requires Java 25, Fabric Loader 0.19.5+, Fabric API.
Build: `gradle build` -> build/libs/chimis-menu-1.0.0.jar
