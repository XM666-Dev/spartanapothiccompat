package com.xm666.spartanapothiccompat.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.oblivioussp.spartanweaponry.api.WeaponTraits;
import com.oblivioussp.spartanweaponry.entity.projectile.ThrowingWeaponEntity;
import com.oblivioussp.spartanweaponry.item.SwordBaseItem;
import com.oblivioussp.spartanweaponry.item.ThrowingWeaponItem;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.socket.SocketHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

public class AffixMixin {
    private static class DamageMixin {
        @Mixin(ThrowingWeaponEntity.class)
        private static class ThrowingWeaponEntityMixin {
            @ModifyVariable(method = "onHitEntity", at = @At(value = "STORE"), name = "src")
            private DamageSource onSetDamageSource(DamageSource damageSource, @Local(name = "weapon") ItemStack weapon, @Local(name = "entity") Entity entity, @Local(name = "damage") LocalFloatRef damage) {
                if (!(entity instanceof LivingEntity living)) return damageSource;

                damage.set(damage.get() + EnchantmentHelper.getDamageBonus(weapon, living.getMobType()));
                return damageSource;
            }
        }
    }

    private static class EffectMixin {
        @Mixin(value = LootCategory.class, remap = false)
        private static class LootCategoryMixin {
            @ModifyReturnValue(method = "forItem", at = @At("RETURN"))
            private static LootCategory modifyForItem(LootCategory original, ItemStack stack) {
                var item = stack.getItem();
                if (!(item instanceof ThrowingWeaponItem)
                        && !(item instanceof SwordBaseItem swordBaseItem
                        && swordBaseItem.hasWeaponTraitWithType(WeaponTraits.TYPE_THROWABLE))) return original;

                return LootCategory.TRIDENT;
            }
        }

        @Mixin(ThrowingWeaponEntity.class)
        private static class ThrowingWeaponEntityMixin {
            @WrapOperation(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;doPostDamageEffects(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/Entity;)V"))
            private void wrapPostDamageEffects(LivingEntity shooter, Entity entity, Operation<Void> original, @Local(name = "weapon") ItemStack weapon) {
                var originalStack = shooter.getItemInHand(InteractionHand.MAIN_HAND);
                shooter.setItemInHand(InteractionHand.MAIN_HAND, weapon);
                original.call(shooter, entity);
                shooter.setItemInHand(InteractionHand.MAIN_HAND, originalStack);
            }
        }

        @Mixin(AffixHelper.class)
        private static class AffixHelperMixin {
            @ModifyExpressionValue(method = "getAffixes(Lnet/minecraft/world/entity/projectile/AbstractArrow;)Ljava/util/Map;", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;getCompound(Ljava/lang/String;)Lnet/minecraft/nbt/CompoundTag;", ordinal = 0))
            private static CompoundTag modifyAffixData(CompoundTag original, AbstractArrow arrow) {
                if (original != null && !original.isEmpty() || !(arrow instanceof ThrowingWeaponEntity throwingWeapon))
                    return original;

                var weapon = throwingWeapon.getWeaponItem();
                return weapon != null ? weapon.getTagElement(AffixHelper.AFFIX_DATA) : null;
            }

            @ModifyReturnValue(method = "getShooterCategory", at = @At("RETURN"), remap = false)
            private static LootCategory modifyShooterCategory(LootCategory original, Entity entity) {
                if (original != null || !(entity instanceof ThrowingWeaponEntity throwingWeapon)) return original;

                var weapon = throwingWeapon.getWeaponItem();
                return weapon != null ? LootCategory.forItem(weapon) : null;
            }
        }

        @Mixin(SocketHelper.class)
        private static class SocketHelperMixin {
            @ModifyExpressionValue(method = "getGems(Lnet/minecraft/world/entity/projectile/AbstractArrow;)Ljava/util/List;", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;getCompound(Ljava/lang/String;)Lnet/minecraft/nbt/CompoundTag;"))
            private static CompoundTag modifyAffixData(CompoundTag original, AbstractArrow arrow) {
                if (original != null && !original.isEmpty() || !(arrow instanceof ThrowingWeaponEntity throwingWeapon))
                    return original;

                var weapon = throwingWeapon.getWeaponItem();
                return weapon != null ? weapon.getTagElement(AffixHelper.AFFIX_DATA) : null;
            }
        }
    }
}
