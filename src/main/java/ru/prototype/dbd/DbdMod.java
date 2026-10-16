package ru.prototype.dbd;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.literal;

public class DbdMod implements ModInitializer {
    public static final String MOD_ID = "dbd_fabric";
    public static final Block GENERATOR = registerBlock("generator",
            new Block(AbstractBlock.Settings.create().mapColor(MapColor.IRON_GRAY).strength(3.5f).requiresTool()));
    public static final Block HOOK = registerBlock("sacrificial_hook",
            new Block(AbstractBlock.Settings.create().mapColor(MapColor.DARK_RED).strength(2.5f).requiresTool()));
    public static final Item BLADE = Registry.register(Registries.ITEM, id("killer_blade"),
            new SwordItem(ToolMaterials.IRON, 2, -2.4f, new Item.Settings().maxCount(1)));
    public static final Item MEDKIT = Registry.register(Registries.ITEM, id("medkit"),
            new Item(new Item.Settings().maxCount(1)));
    private static final Map<String, Integer> GENERATOR_PROGRESS = new HashMap<>();
    private static final Map<UUID, Long> LAST_HEAL = new HashMap<>();
    private static boolean matchStarted = false;
    private static int completedGenerators = 0;

    private static Identifier id(String path) { return new Identifier(MOD_ID, path); }
    private static Block registerBlock(String name, Block block) {
        Registry.register(Registries.BLOCK, id(name), block);
        Registry.register(Registries.ITEM, id(name), new BlockItem(block, new Item.Settings()));
        return block;
    }

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("dbd").then(literal("start").executes(ctx -> {
                MinecraftServer server = ctx.getSource().getServer();
                matchStarted = true;
                completedGenerators = 0;
                GENERATOR_PROGRESS.clear();
                server.getPlayerManager().broadcast(Text.translatable("message.dbd_fabric.match_started"), false);
                return 1;
            })).then(literal("stop").executes(ctx -> {
                matchStarted = false;
                ctx.getSource().getServer().getPlayerManager().broadcast(Text.literal("Испытание остановлено."), false);
                return 1;
            })).then(literal("status").executes(ctx -> {
                ctx.getSource().sendFeedback(() -> Text.literal("DBD: " + (matchStarted ? "идёт матч" : "матч не запущен")
                        + " | Генераторы: " + completedGenerators + "/5"), false);
                return 1;
            })));
        });

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient || hand != Hand.MAIN_HAND || !(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            BlockPos pos = hit.getBlockPos();
            if (world.getBlockState(pos).isOf(GENERATOR)) {
                if (!matchStarted) {
                    serverPlayer.sendMessage(Text.literal("Сначала начните матч: /dbd start"), true);
                    return ActionResult.SUCCESS;
                }
                String key = world.getRegistryKey().getValue() + ":" + pos.toShortString();
                int progress = Math.min(100, GENERATOR_PROGRESS.getOrDefault(key, 0) + 10);
                GENERATOR_PROGRESS.put(key, progress);
                serverPlayer.sendMessage(Text.translatable("message.dbd_fabric.generator_progress", progress), true);
                if (progress >= 100) {
                    GENERATOR_PROGRESS.remove(key);
                    completedGenerators = Math.min(5, completedGenerators + 1);
                    world.getServer().getPlayerManager().broadcast(
                        Text.translatable("message.dbd_fabric.generator_complete", completedGenerators), false);
                    if (completedGenerators >= 5) {
                        world.getServer().getPlayerManager().broadcast(Text.translatable("message.dbd_fabric.match_ended"), false);
                    }
                }
                return ActionResult.SUCCESS;
            }
            if (world.getBlockState(pos).isOf(HOOK)) {
                serverPlayer.sendMessage(Text.literal("Крюк испытания: механика переноски и спасения появится в следующей версии."), true);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (hand != Hand.MAIN_HAND || !player.getStackInHand(hand).isOf(MEDKIT)) return TypedActionResult.pass(player.getStackInHand(hand));
            if (world.isClient) return TypedActionResult.pass(player.getStackInHand(hand));
            long now = world.getTime();
            long ready = LAST_HEAL.getOrDefault(player.getUuid(), 0L);
            if (now < ready) {
                player.sendMessage(Text.literal("Аптечка перезаряжается."), true);
            } else if (player instanceof ServerPlayerEntity) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1));
                LAST_HEAL.put(player.getUuid(), now + 20L * 60L);
                player.sendMessage(Text.literal("Вы использовали аптечку."), true);
            }
            return TypedActionResult.success(player.getStackInHand(hand));
        });
    }
}
