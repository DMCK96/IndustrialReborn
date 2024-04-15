package me.munchii.industrialreborn.items;

import me.munchii.industrialreborn.IndustrialReborn;
import me.munchii.industrialreborn.init.IRContent;
import me.munchii.industrialreborn.utils.EntityCaptureUtils;
import me.munchii.industrialreborn.storage.entity.EntityStorage;
import me.munchii.industrialreborn.utils.EntityUtil;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class SoulVialItem extends Item {

    public SoulVialItem(boolean filled) {
        super(new Item.Settings().maxCount(filled ? 1 : 16));
    }

    public static void setup() {
        UseEntityCallback.EVENT.register(SoulVialItem::useOnEntity);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getWorld().isClient) {
            return ActionResult.FAIL;
        }

        PlayerEntity player = context.getPlayer();

        if (player == null) {
            return ActionResult.FAIL;
        }

        return releaseEntity(context.getWorld(), context.getStack(), context.getBlockPos(), emptyVial -> player.setStackInHand(context.getHand(), emptyVial));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        IndustrialReborn.LOGGER.error("AAABBB SoulVialItem use");
        return super.use(world, user, hand);
    }

    /*@Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        IndustrialReborn.LOGGER.error("AAABBB SoulVialItem {}", entity.getType().toString());
        if (user.getWorld().isClient) {
            IndustrialReborn.LOGGER.error("AAABBB SoulVialItem 1");
            return ActionResult.FAIL;
        }

        IndustrialReborn.LOGGER.error("AAABBB SoulVialItem 2");

        Optional<ItemStack> itemStack = catchEntity(stack, entity);
        if (itemStack.isPresent()) {
            ItemStack filledVial = itemStack.get();
            ItemStack handStack = user.getStackInHand(hand);
            if (handStack.isEmpty()) {
                handStack.setCount(1);
                user.setStackInHand(hand, filledVial);
            } else {
                if (!user.giveItemStack(filledVial)) {
                    user.dropItem(filledVial, false);
                }
            }

            return ActionResult.SUCCESS;
        }

        return ActionResult.FAIL;
    }*/

    public static ActionResult useOnEntity(PlayerEntity player, World world, Hand hand, Entity entity, HitResult hitResult) {
        if (entity instanceof PlayerEntity || player.getWorld().isClient || !(entity instanceof LivingEntity livingEntity && livingEntity.isAlive())) {
            return ActionResult.PASS;
        }

        ItemStack vialStack = player.getStackInHand(hand);
        if (vialStack.isOf(IRContent.EMPTY_SOUL_VIAL)) {
            Optional<ItemStack> itemStack = SoulVialItem.catchEntity(vialStack, (LivingEntity) entity);
            if (itemStack.isPresent()) {
                ItemStack filledVial = itemStack.get();
                ItemStack handStack = player.getStackInHand(hand);
                if (handStack.isEmpty()) {
                    handStack.setCount(1);
                    player.setStackInHand(hand, filledVial);
                } else {
                    if (!player.giveItemStack(filledVial)) {
                        player.dropItem(filledVial, false);
                    }
                }

                return ActionResult.SUCCESS;
            }
        }

        return ActionResult.PASS;
    }

    private static Optional<ItemStack> catchEntity(ItemStack soulVial, LivingEntity entity) {
        if (entity instanceof PlayerEntity) {
            return Optional.empty();
        }

        //noinspection unchecked
        EntityCaptureUtils.CapturableStatus capturableStatus = EntityCaptureUtils.getCapturableStatus((EntityType<? extends LivingEntity>) entity.getType(), entity);
        if (capturableStatus != EntityCaptureUtils.CapturableStatus.CAPTURABLE) {
            return Optional.empty();
        }

        if (!entity.isAlive()) {
            return Optional.empty();
        }

        if (entity instanceof MobEntity mob && mob.isLeashed()) {
            mob.detachLeash(true, true);
        }

        soulVial.decrement(1);
        ItemStack filledVial = IRContent.FILLED_SOUL_VIAL.getDefaultStack();
        EntityStorage.saveEntityData(filledVial, entity);

        entity.discard();
        return Optional.of(filledVial);
    }

    private static ActionResult releaseEntity(World world, ItemStack filledVial, BlockPos pos, Consumer<ItemStack> emptyVialSetter) {
        if (EntityStorage.hasStoredEntity(filledVial)) {
            Optional<NbtCompound> entityTag = EntityStorage.getEntityDataCompound(filledVial);
            if (entityTag.isEmpty()) {
                return ActionResult.FAIL;
            }

            Optional<Entity> entity = EntityUtil.createFromNbt((ServerWorld) world, entityTag.get());

            entity.ifPresent(ent -> {
                ent.setPosition(pos.toCenterPos().add(0, 0.5, 0));
                ent.applyRotation(BlockRotation.random(world.getRandom()));
                world.spawnEntity(ent);
            });

            emptyVialSetter.accept(IRContent.EMPTY_SOUL_VIAL.getDefaultStack());
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (stack.getItem() == IRContent.FILLED_SOUL_VIAL.asItem() && EntityStorage.hasStoredEntity(stack)) {
            Optional<NbtCompound> tag = EntityStorage.getEntityDataCompound(stack);
            tag.ifPresent(nbtCompound -> {
                Optional<Entity> entity = EntityType.getEntityFromNbt(nbtCompound, world);
                entity.ifPresent(ent -> tooltip.add(Text.translatable("item.industrialreborn.filled_soul_vial.tooltip", Objects.requireNonNull(ent.getDisplayName()).getString()).formatted(Formatting.GRAY)));
            });
        }
    }
}
