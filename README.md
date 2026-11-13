# Botania Applie

## Introduction

This branch contains the Minecraft 1.16.5 Forge port of Botania Applie. It integrates Botania's mana system with Applied Energistics 2 (AE2), storing mana in AE2 storage cells and managing mana transmission through ME networks.

***

## Main Features

### Mana Storage Cells

*   Multiple capacity mana storage cells: 1k, 4k, 16k, 64k, 256k, 1M, 4M, 16M, 64M, 256M, 1G
*   Compatible with AE2 drives
*   View stored mana in ME terminals

***

### Fluix Mana Pool

*   Combines Botania mana pool with AE2 network functionality
*   Occupies 1 ME channel
*   Access mana from the network when connected
*   Supports mana infusion recipes
*   Retains a local buffer of up to 2,147,483,647 mana while offline
*   Drains the local buffer before drawing from the network and retries depositing it when connected
*   Optional Jade integration displays exact long-valued mana storage and capacity with a fill bar

The Fluix Mana Pool Card supports ME Interfaces in block and part forms. ME Fluid Interfaces are not supported.

***

### ME Terminal Integration

*   Display virtual mana fluid in ME terminals
*   Quick interaction with mana containers:
    *   Left-click: Execute mana exchange (fill/extract)
    *   Right-click: Deposit item into network
*   Supports special mana containers like Terra Shatterer
*   Uses Terminal Interaction Integration for the 1.16.5 terminal API

### Channel Sparks and Wireless Mana

*   Main channel sparks bridge AE2 channels to channel sparks using native ME P2P connections
*   Nearby channel sparks relay discovery across the spark network
*   Wireless terminals in the player's inventory or Curios slots can supply mana to carried equipment
*   Wireless mana transfer is one-way: from ME storage to the player's equipment

### Languages

*   English, Simplified Chinese, and Russian

***

## Dependencies

| Mod | Version |
| --- | --- |
| Minecraft | 1.16.5 |
| Forge | 36.2.42 |
| Botania | 1.16.5-420.3 |
| Applied Energistics 2 | 8.4.7 |
| Terminal Interaction Integration | 1.2.4-1.16.5 |
| Patchouli (Botania dependency) | 1.16.4-53.3 |
| Curios (accessory support) | 1.16.5-4.x |
| Jade (optional tooltip integration) | 1.16.5-2.8.3, CurseForge file 3910873 |

The 1.16.5 port uses Jade instead of The One Probe. Legacy AE2FC and Mekanism Energistics compatibility is not included; the existing AE2, Botania, terminal, and Curios functionality is retained.
