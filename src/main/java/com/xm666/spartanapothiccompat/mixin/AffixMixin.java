package com.xm666.spartanapothiccompat.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.xiyu.spartanweaponryunofficial.api.WeaponTraits;
import org.xiyu.spartanweaponryunofficial.entity.projectile.ThrowingWeaponEntity;
import org.xiyu.spartanweaponryunofficial.item.SwordBaseItem;
import org.xiyu.spartanweaponryunofficial.item.ThrowingWeaponItem;

public class AffixMixin {
    private static class DamageMixin {
        @Mixin(ThrowingWeaponEntity.class)
        private static class ThrowingWeaponEntityMixin {
            @ModifyVariable(method = "onHitEntity", at = @At(value = "STORE"), name = "src")
            private DamageSource onSetDamageSource(DamageSource damageSource, @Local(name = "level") Level level, @Local(name = "weapon") ItemStack weapon, @Local(name = "entity") Entity entity, @Local(name = "damage") LocalFloatRef damage) {
                if (!(level instanceof ServerLevel serverLevel)) return damageSource;

                damage.set(EnchantmentHelper.modifyDamage(serverLevel, weapon, entity, damageSource, damage.get()));
                return damageSource;
            }
        }
    }

    private static class EffectMixin {
        @Mixin(LootCategory.class)
        private static class LootCategoryMixin {
            @ModifyReturnValue(method = "forItem", at = @At("RETURN"))
            private static LootCategory modifyForItem(LootCategory original, ItemStack stack) {
                var item = stack.getItem();
                if (!(item instanceof ThrowingWeaponItem)
                        && !(item instanceof SwordBaseItem swordBaseItem
                        && swordBaseItem.hasWeaponTraitWithType(WeaponTraits.TYPE_THROWABLE))) return original;

                return Apoth.LootCategories.TRIDENT;
            }
        }

        @Mixin(ThrowingWeaponEntity.class)
        private static class ThrowingWeaponEntityMixin {
            @Inject(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lorg/xiyu/spartanweaponryunofficial/entity/projectile/ThrowingWeaponEntity;doPostHurtEffects(Lnet/minecraft/world/entity/LivingEntity;)V"))
            private void onPostHurtEffects(EntityHitResult hitResult, CallbackInfo ci, @Local(name = "level") Level level, @Local(name = "shooter") Entity shooter, @Local(name = "entitylivingbase") LivingEntity living, @Local(name = "src") DamageSource damageSource, @Local(name = "weapon") ItemStack weapon) {
                if (!(level instanceof ServerLevel serverLevel) || !(shooter instanceof LivingEntity livingShooter))
                    return;

                var originalStack = livingShooter.getMainHandItem();
                livingShooter.setItemInHand(InteractionHand.MAIN_HAND, weapon);
                EnchantmentHelper.doPostAttackEffectsWithItemSource(serverLevel, living, damageSource, weapon);
                livingShooter.setItemInHand(InteractionHand.MAIN_HAND, originalStack);
            }
        }

        @Mixin(AffixHelper.class)
        private static class AffixHelperMixin {
            @ModifyReturnValue(method = "getSourceWeapon", at = @At("RETURN"))
            private static ItemStack modifySourceWeapon(ItemStack original, Entity entity) {
                if (original != ItemStack.EMPTY || !(entity instanceof ThrowingWeaponEntity)) return original;

                return entity.getWeaponItem();
            }
        }
    }
}
