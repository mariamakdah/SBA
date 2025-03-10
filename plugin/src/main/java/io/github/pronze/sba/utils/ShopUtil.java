package io.github.pronze.sba.utils;

import io.github.pronze.sba.MessageKeys;
import io.github.pronze.sba.SBA;
import io.github.pronze.sba.config.SBAConfig;
import io.github.pronze.sba.data.DegradableItem;
import io.github.pronze.sba.manager.ArenaManager;
import io.github.pronze.sba.game.IGameStorage;
import io.github.pronze.sba.game.StoreType;
import io.github.pronze.sba.inventories.SBAUpgradeStoreInventory;
import io.github.pronze.sba.lib.lang.LanguageService;
import io.github.pronze.sba.service.PlayerWrapperService;
import io.github.pronze.sba.wrapper.PlayerWrapper;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.screamingsandals.bedwars.Main;
import org.screamingsandals.bedwars.api.BedwarsAPI;
import org.screamingsandals.bedwars.api.TeamColor;
import org.screamingsandals.bedwars.api.game.Game;
import org.screamingsandals.bedwars.api.game.ItemSpawnerType;
import org.screamingsandals.bedwars.api.utils.ColorChanger;
import org.screamingsandals.lib.material.Item;
import org.screamingsandals.lib.material.meta.EnchantmentMapping;
import org.screamingsandals.lib.player.PlayerMapper;
import org.screamingsandals.lib.utils.AdventureHelper;
import org.screamingsandals.simpleinventories.builder.LocalOptionsBuilder;
import org.screamingsandals.simpleinventories.events.ItemRenderEvent;
import org.screamingsandals.simpleinventories.inventory.PlayerItemInfo;
import org.spongepowered.configurate.ConfigurationNode;

import java.io.File;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ShopUtil {

    public static final List<String> romanNumerals = List.of("NONE", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X");
    public static final List<String> orderOfArmor = List.of("GOLDEN,GOLD", "CHAINMAIL", "IRON", "DIAMOND", "NETHERITE");
    public static final List<String> orderOfTools = List.of("WOODEN,WOOD", "STONE", "GOLDEN,GOLD", "IRON", "DIAMOND");

    @NotNull
    public static Integer getLevelFromMaterialName(@NotNull String name, final List<String> list) {
        name = name.substring(0, name.contains("_") ? name.lastIndexOf("_") : name.length());
        @NotNull String finalName = name;
        return list.stream()
                .filter(value -> Arrays.stream(value.split(",")).anyMatch(names -> names.equalsIgnoreCase(finalName)))
                .map(list::indexOf)
                .findAny()
                .orElse(0);
    }

    @NotNull
    public static String getMaterialFromLevel(int level, DegradableItem itemType) {
        final var list = itemType == DegradableItem.ARMOR ? orderOfArmor : orderOfTools;
        var obj = list.get(level);
        if (obj == null) {
            obj = list.get(0);
        }

        final var toTest = itemType == DegradableItem.ARMOR ? "_BOOTS" : "_AXE";
        var toParse = obj.split(",");
        for (String matName : toParse) {
            try {
                Material.valueOf(matName + toTest);
                return matName;
            } catch (IllegalArgumentException ignored) {
            }
        }

        return list.get(0);
    }

    @NotNull
    public static String getMaterialFromArmorOrTools(@NotNull String material) {
        return material.substring(0, material.indexOf("_")).toUpperCase();
    }

    @NotNull
    public static String getMaterialFromArmorOrTools(@NotNull Material material) {
        return getMaterialFromArmorOrTools(material.name());
    }

    public static boolean buyArmor(Player player, Material mat_boots, IGameStorage gameStorage, Game game) {
        final var playerInventory = player.getInventory();
        final var playerBoots = playerInventory.getBoots();
        final var matName = getMaterialFromArmorOrTools(mat_boots);

        if (playerBoots != null) {
            final var currentMat = playerBoots.getType();
            final var currentMatName = getMaterialFromArmorOrTools(currentMat);

            int currentLevel = getLevelFromMaterialName(currentMatName, orderOfArmor);
            int newLevel = getLevelFromMaterialName(matName, orderOfArmor);

            if (!SBAConfig.getInstance().node("can-downgrade-item").getBoolean(false)) {
                if (currentLevel > newLevel) {
                    LanguageService
                            .getInstance()
                            .get(MessageKeys.CANNOT_DOWNGRADE_ITEM)
                            .replace("%item%", "armor")
                            .send(PlayerMapper.wrapPlayer(player));
                    return false;
                }
            }

            if (currentLevel == newLevel) {
                LanguageService
                        .getInstance()
                        .get(MessageKeys.ALREADY_PURCHASED)
                        .replace("%thing%", "armor")
                        .send(PlayerMapper.wrapPlayer(player));
                return false;
            }
        }

        final var boots = new ItemStack(mat_boots);
        final var leggings = new ItemStack(Material.valueOf(matName + "_LEGGINGS"));

        final var level = gameStorage.getProtectionLevel(game.getTeamOfPlayer(player)).orElseThrow();
        if (level != 0) {
            boots.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, level);
            leggings.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, level);
        }

        playerInventory.setLeggings(null);
        playerInventory.setBoots(null);
        playerInventory.setBoots(boots);
        playerInventory.setLeggings(leggings);
        return true;
    }


    static <K, V> List<K> getAllKeysForValue(Map<K, V> mapOfWords, V value) {
        return mapOfWords.entrySet()
                .stream()
                .filter((entry) -> entry.getValue().equals(value))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public static List<Game> getGamesWithSize(int size) {
        final List<String> maps = getAllKeysForValue(SBAConfig.game_size, size);
        if (maps == null || maps.isEmpty())
            return null;

        final ArrayList<Game> gameList = new ArrayList<>();

        maps.forEach(map -> {
            if (Main.getGameNames().contains(map))
                gameList.add(Main.getGame(map));
        });

        return gameList;
    }


    public static <K, V> K getKey(Map<K, V> map, V value) {
        return map.keySet()
                .stream()
                .filter(key -> value.equals(map.get(key)))
                .findAny()
                .orElse(null);
    }

    public static void giveItemToPlayer(@NotNull List<ItemStack> itemStackList, Player player, TeamColor teamColor) {
        itemStackList.forEach(itemStack -> {
            if (itemStack == null) {
                return;
            }

            ColorChanger colorChanger = BedwarsAPI.getInstance().getColorChanger();

            final String materialName = itemStack.getType().toString();
            final PlayerInventory playerInventory = player.getInventory();

            if (materialName.contains("HELMET")) {
                playerInventory.setHelmet(colorChanger.applyColor(teamColor, itemStack));
            } else if (materialName.contains("CHESTPLATE")) {
                playerInventory.setChestplate(colorChanger.applyColor(teamColor, itemStack));
            } else if (materialName.contains("LEGGINGS")) {
                playerInventory.setLeggings(colorChanger.applyColor(teamColor, itemStack));
            } else if (materialName.contains("BOOTS")) {
                playerInventory.setBoots(colorChanger.applyColor(teamColor, itemStack));
            } else if (materialName.contains("PICKAXE")) {
                playerInventory.setItem(7, itemStack);
            } else if (materialName.contains("AXE")) {
                playerInventory.setItem(8, itemStack);
            } else if (materialName.contains("SWORD")) {
                playerInventory.setItem(0, itemStack);
            } else {
                playerInventory.addItem(colorChanger.applyColor(teamColor, itemStack));
            }
        });

    }

    public static ItemStack downgradeItem(ItemStack currentItem, DegradableItem itemType) {
        final var currentItemName = getMaterialFromArmorOrTools(currentItem.getType());
        final int currentItemLevel = getLevelFromMaterialName(currentItemName, orderOfTools);

        if (currentItemLevel == 0) {
            return currentItem;
        }

        final var newItemLevel = currentItemLevel - 1;
        final var newMaterialName = getMaterialFromLevel(newItemLevel, itemType);
        final var newMaterial = Material.valueOf(newMaterialName + currentItem.getType().name().substring(currentItem.getType().name().lastIndexOf("_")).toUpperCase());
        final var newStack = new ItemStack(newMaterial);
        newStack.addEnchantments(currentItem.getEnchantments());
        return newStack;
    }

    public static int getIntFromMode(String mode) {
        return mode.equalsIgnoreCase("Solo") ? 1 : mode.equalsIgnoreCase("Double") ? 2 : mode.equalsIgnoreCase("Triples") ? 3 : mode.equalsIgnoreCase("Squads") ? 4 : 0;
    }

    public static String translateColors(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }


    public static void sendMessage(Player player, List<String> message) {
        message.forEach(st -> player.sendMessage(translateColors(st)));
    }

    public static File normalizeShopFile(String name) {
        if (name.split("\\.").length > 1) {
            return SBA.getPluginInstance().getDataFolder().toPath().resolve(name).toFile();
        }

        var fileg = SBA.getPluginInstance().getDataFolder().toPath().resolve(name + ".groovy").toFile();
        if (fileg.exists()) {
            return fileg;
        }
        return SBA.getPluginInstance().getDataFolder().toPath().resolve(name + ".yml").toFile();
    }

    public static Map<?, ?> nullValuesAllowingMap(Object... objects) {
        var map = new HashMap<>();
        Object key = null;
        for (var object : objects) {
            if (key == null) {
                key = Objects.requireNonNull(object);
            } else {
                map.put(key, object);
                key = null;
            }
        }
        return map;
    }

    public static void setLore(Item item, PlayerItemInfo itemInfo, String price, ItemSpawnerType type, Player player) {
        var enabled = itemInfo.getFirstPropertyByName("generateLore")
                .map(property -> property.getPropertyData().getBoolean())
                .orElseGet(() -> Main.getConfigurator().config.getBoolean("lore.generate-automatically", true));

        if (enabled) {
            final var originalList = item.getLore();

            final var isSharp = itemInfo.getFirstPropertyByName("sharpness").isPresent();
            final var isProt = itemInfo.getFirstPropertyByName("protection").isPresent();
            final var isEfficiency = itemInfo.getFirstPropertyByName("efficiency").isPresent();

            final var game = Main.getInstance().getGameOfPlayer(player);
            final var arena = ArenaManager
                    .getInstance()
                    .get(game.getName())
                    .orElseThrow();

            if (isSharp) {
                final var currentLevel = arena.getStorage().getSharpnessLevel(game.getTeamOfPlayer(player)).orElseThrow() + 1;
                final var limit = SBAConfig.getInstance().node("upgrades", "limit", "Sharpness").getInt(2);
                if (currentLevel <= limit) {
                    price = String.valueOf(SBAUpgradeStoreInventory.sharpnessPrices.get(arena.getStorage().getSharpnessLevel(game.getTeamOfPlayer(player)).orElseThrow() + 1));
                }
            }

            if (isProt) {
                final var currentLevel = arena.getStorage().getProtectionLevel(game.getTeamOfPlayer(player)).orElseThrow() + 1;
                final var limit = SBAConfig.getInstance().node("upgrades", "limit", "Protection").getInt(4);
                if (currentLevel <= limit) {
                    price = String.valueOf(SBAUpgradeStoreInventory.protectionPrices.get(arena.getStorage().getProtectionLevel(game.getTeamOfPlayer(player)).orElseThrow() + 1));
                }
            }

            if (isEfficiency) {
                final var currentLevel = arena.getStorage().getEfficiencyLevel(game.getTeamOfPlayer(player)).orElseThrow() + 1;
                final var limit = SBAConfig.getInstance().node("upgrades", "limit", "Efficiency").getInt(4);
                if (currentLevel <= limit) {
                    price = String.valueOf(SBAUpgradeStoreInventory.efficiencyPrices.get(arena.getStorage().getEfficiencyLevel(game.getTeamOfPlayer(player)).orElseThrow() + 1));
                }
            }

            String finalPrice = price;
            final var newList = itemInfo.getFirstPropertyByName("generatedLoreText")
                    .map(property -> property.getPropertyData().childrenList().stream().map(ConfigurationNode::getString))
                    .orElseGet(() -> Main.getConfigurator().config.getStringList("lore.text").stream())
                    .map(s -> s
                            .replaceAll("%price%", finalPrice)
                            .replaceAll("%resource%", type.getItemName())
                            .replaceAll("%amount%", Integer.toString(itemInfo.getStack().getAmount())))
                    .map(s -> ChatColor.translateAlternateColorCodes('&', s))
                    .map(AdventureHelper::toComponent).collect(Collectors.toCollection((Supplier<ArrayList<Component>>) ArrayList::new));
            newList.addAll(originalList);

            item.getLore().clear();
            item.getLore().addAll(newList);
        }
    }

    public static String getNameOrCustomNameOfItem(Item item) {
        try {
            if (item.getDisplayName() != null) {
                return AdventureHelper.toLegacy(item.getDisplayName());
            }
            if (item.getLocalizedName() != null) {
                return AdventureHelper.toLegacy(item.getLocalizedName());
            }
        } catch (Throwable ignored) {
        }

        var normalItemName = item.getMaterial().getPlatformName().replace("_", " ").toLowerCase();
        var sArray = normalItemName.split(" ");
        var stringBuilder = new StringBuilder();

        for (var s : sArray) {
            stringBuilder.append(Character.toUpperCase(s.charAt(0))).append(s.substring(1)).append(" ");
        }
        return stringBuilder.toString().trim();
    }


    public static void addEnchantsToPlayerArmor(Player player, int newLevel) {
        for (var item : player.getInventory().getArmorContents()) {
            if (item != null) {
                item.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, newLevel);
            }
        }
    }

    public static void clampOrApplyEnchants(Item item, int level, Enchantment enchantment, StoreType type, int maxLevel) {
        if (type == StoreType.UPGRADES) {
            level = level + 1;
        }
        if (level > maxLevel) {
            item.getLore().clear();
            LanguageService
                    .getInstance()
                    .get(MessageKeys.SHOP_MAX_ENCHANT)
                    .toComponentList()
                    .forEach(item::addLore);
            if (item.getEnchantments() != null) {
                item.getEnchantments().clear();
            }
        } else if (level > 0) {
            item.addEnchant(EnchantmentMapping.resolve(enchantment).orElseThrow().newLevel(level));
        }
    }


    /**
     * Applies enchants to displayed items in SBA store inventory.
     * Enchants are applied and are dependent on the team upgrades the player's team has.
     *
     * @param item
     * @param event
     */
    public static void applyTeamUpgradeEnchantsToItem(Item item, ItemRenderEvent event, StoreType type) {
        final var player = event.getPlayer().as(Player.class);
        final var game = Main.getInstance().getGameOfPlayer(player);
        final var typeName = item.getMaterial().getPlatformName();
        final var runningTeam = game.getTeamOfPlayer(player);

        var prices = event.getInfo().getOriginal().getPrices();
        if (!prices.isEmpty()) {
            item.addLore(LanguageService
                    .getInstance()
                    .get(MessageKeys.CLICK_TO_PURCHASE)
                    .toComponent());
        }

        SBA.getInstance()
                .getGameStorage(game)
                .ifPresent(gameStorage -> {
                    final var afterUnderscore = typeName.substring(typeName.contains("_") ? typeName.indexOf("_") + 1 : 0);
                    switch (afterUnderscore.toLowerCase()) {
                        case "sword":
                            int sharpness = gameStorage.getSharpnessLevel(runningTeam).orElseThrow();
                            clampOrApplyEnchants(item, sharpness, Enchantment.DAMAGE_ALL, type, SBAConfig.getInstance().node("upgrades", "limit", "Sharpness").getInt(1));
                            break;
                        case "chestplate":
                        case "boots":
                            int protection = gameStorage.getProtectionLevel(runningTeam).orElseThrow();
                            clampOrApplyEnchants(item, protection, Enchantment.PROTECTION_ENVIRONMENTAL, type, SBAConfig.getInstance().node("upgrades", "limit", "Protection").getInt(4));
                            break;
                        case "pickaxe":
                            final int efficiency = gameStorage.getEfficiencyLevel(runningTeam).orElseThrow();
                            clampOrApplyEnchants(item, efficiency, Enchantment.DIG_SPEED, type, SBAConfig.getInstance().node("upgrades", "limit", "Efficiency").getInt(2));
                            break;
                    }
                });
    }

    //TODO:
    public static void generateOptions(LocalOptionsBuilder localOptionsBuilder) {
        final var backItem = Main.getConfigurator().readDefinedItem("shopback", "BARRIER");
        final var backItemMeta = backItem.getItemMeta();

        //   backItemMeta.setDisplayName(Message.of());
//
        // backItem.setDisplayName(Message.of(LangKeys.IN_GAME_SHOP_SHOP_BACK).asComponent());
        //localOptionsBuilder.backItem(backItem);

        //final var pageBackItem = MainConfig.getInstance().readDefinedItem("pageback", "ARROW");
        //pageBackItem.setDisplayName(Message.of(LangKeys.IN_GAME_SHOP_PAGE_BACK).asComponent());
        //localOptionsBuilder.pageBackItem(pageBackItem);

        //final var pageForwardItem = MainConfig.getInstance().readDefinedItem("pageforward", "ARROW");
        //pageForwardItem.setDisplayName(Message.of(LangKeys.IN_GAME_SHOP_PAGE_FORWARD).asComponent());
        //localOptionsBuilder.pageForwardItem(pageForwardItem);

        //final var cosmeticItem = MainConfig.getInstance().readDefinedItem("shopcosmetic", "AIR");
        localOptionsBuilder
                //  .cosmeticItem(cosmeticItem)
                .renderHeaderStart(600)
                .renderFooterStart(600)
                .renderOffset(9)
                .rows(4)
                .renderActualRows(4)
                .showPageNumber(false);
    }

    public static String ChatColorChanger(Player player) {
        final PlayerWrapper db = PlayerWrapperService.getInstance().get(player).orElseThrow();
        if (db.getLevel() > 100 || player.isOp()) {
            return "§f";
        } else {
            return "§7";
        }
    }


}