# Minecraft Server Plugin

ViralCraft Server is a Minecraft Paper plugin for adding social server features to a single server: join and quit messages, login streaks, a daily starter kit, spawn teleportation, and scheduled announcements. It does not require Velocity or BungeeCord, and it does not create proxy or multi-server routing.

## Install

1. Build the project with Java 21 and Maven by running `mvn package`.
2. Copy `target/plugin.jar` into your Paper server’s `plugins` folder.
3. Start or restart the server.

## Use

- Run `/viral` to see the plugin’s command guide.
- Run `/spawn` to teleport to the configured spawn, or the first loaded world’s spawn if none is configured.
- Run `/kit` to claim an iron sword, 16 bread, and 16 torches. The kit has a 24-hour cooldown.
- Run `/streak` to check your login streak.
- Operators with `viral.admin` can run `/viral announce` to broadcast an event message.

---

More Minecraft plugin projects by EnderDevelopment: https://github.com/EnderDevelopment

Project page: https://dash.enderdevelopment.com/community/19f44343-ba6b-493a-87a4-1fab4ca2d9fa?utm_source=github&utm_medium=readme&utm_campaign=auto_publish&utm_content=19f44343-ba6b-493a-87a4-1fab4ca2d9fa
