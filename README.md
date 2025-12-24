![Minecraft](https://img.shields.io/badge/Minecraft-Server%20Plugin-informational)
![Java](https://img.shields.io/badge/Java-8-blue)
![Platform](https://img.shields.io/badge/Platform-Spigot%20%7C%20Paper-lightgrey)
![Status](https://img.shields.io/badge/Status-Experimental-orange)

# PacketPlots

PacketPlots is a Minecraft server plugin that virtualizes small, individual parts of the game world for each player. It is completely configurable through a YAML-config.

## Overview

PacketPlots intercepts and modifies both incoming and outgoing network packets to alter parts of the game world client-side. This makes it possible for each player to experience a customized version of specific world regions while still playing together on the same server.

The plugin is installed directly on the Minecraft server. Players do not need to install mods, resource packs, or additional software. From the client's perspective, gameplay appears normal. Every modification is abstracted completely from the 'normal' server, and intercepted at the packet-level.

## Features

-   Per-player world virtualization
-   Packet-level modification of world data
-   Server-side only (no client installation required)
-   (De)serialization of virtualized worlds
-   Visits between virtual worlds

## Compatibility

PacketPlots relies on low-level packet manipulation and may be sensitive to Minecraft and plugin changes. **The plugin is developed for version 1.8 exclusively**.

Due to the nature of the plugin, **it's sensitive with AntiCheats**. By default it ships with AntiCheat-compatability for Vulcan and NoCheatPlus, however additional compatibility modules can be added dynamically and later adopted into the plugin via pull requests.

## Disclaimer

The code comes with no warranty, simply the fact that the plugin has previously been battle-tested. Bugs may occur due to the nature of the plugin.
