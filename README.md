# Security Cams (Forge 1.19.2)

Craftable security cameras you mount on any wall, floor or ceiling and watch live on a tablet, with night vision, panning and zoom.

## Get the jar (no setup on your PC)
1. Create a new GitHub repository and upload everything in this folder to it (keep the `.github` folder).
2. Open the repo's **Actions** tab. The "Build mod" workflow runs on every push. You can also click **Run workflow**.
3. When it finishes, open the run and download the **securitycams-jar** artifact (a zip containing the jar).
4. Put the jar in the server's `mods` folder and restart.
5. Players must also put the same jar in their own `mods` folder, because it adds a block and an item.

Optional: tag a version (`git tag v1.0.0 && git push --tags`) to get the jar as a permanent download on the repo's Releases page.

## Use
- Craft a Security Camera and a Security Tablet. Rename the camera in an anvil first to name its feed.
- Place the camera on any block face. It points away from the block it is mounted on.
- Right-click a camera with the tablet to link it (owner only). Sneak + right-click to unlink. Max 16 per tablet.
- Right-click anywhere else with the tablet to open the camera list, then pick a feed.
- In a feed: drag or WASD/arrows = pan, scroll = zoom, N = night vision, Q/E = switch camera, Esc = back.
- Getting hurt or putting the tablet away closes the feed.

## Recipes
- Camera:  I G I / R O R / I I I   (I iron ingot, G glass pane, R redstone, O observer)
- Tablet:  I I I / G D G / I R I   (D diamond)
Edit `src/main/resources/data/securitycams/recipes/` to change them.

## Limits
- A feed only works while the camera's chunk is loaded on your client (inside your render distance).
- Night vision is a client-side visual effect only. The server is not involved.
