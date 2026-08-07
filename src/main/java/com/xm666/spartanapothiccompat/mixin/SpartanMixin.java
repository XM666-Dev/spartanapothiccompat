package com.xm666.spartanapothiccompat.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.xm666.spartanapothiccompat.AttributeHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.xiyu.spartanweaponryunofficial.entity.projectile.ThrowingWeaponEntity;

public class SpartanMixin {
    @Mixin(ThrowingWeaponEntity.class)
    private static class ThrowingWeaponEntityMixin {
        @ModifyVariable(method = "onHitEntity", at = @At(value = "STORE"), name = "src")
        private static DamageSource onSetDamageSource(DamageSource damageSource, @Local(name = "level") Level level, @Local(name = "weapon") ItemStack weapon, @Local(name = "entity") Entity entity, @Local(name = "damage") LocalFloatRef damage) {
            if (!(level instanceof ServerLevel serverLevel)) return damageSource;

            damage.set(EnchantmentHelper.modifyDamage(serverLevel, weapon, entity, damageSource, damage.get()));
            return damageSource;
        }

        @Inject(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lorg/xiyu/spartanweaponryunofficial/entity/projectile/ThrowingWeaponEntity;doPostHurtEffects(Lnet/minecraft/world/entity/LivingEntity;)V"))
        private static void onPostHurtEffects(EntityHitResult hitResult, CallbackInfo ci, @Local(name = "level") Level level, @Local(name = "entitylivingbase") LivingEntity living, @Local(name = "src") DamageSource damageSource, @Local(name = "weapon") ItemStack weapon) {
            if (!(level instanceof ServerLevel serverLevel)) return;

            EnchantmentHelper.doPostAttackEffectsWithItemSource(serverLevel, living, damageSource, weapon);
        }

        @ModifyExpressionValue(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lorg/xiyu/spartanweaponryunofficial/entity/projectile/ThrowingWeaponEntity;getBaseDamage()D"))
        private double modifyBaseDamage(double original, @Local(name = "weapon") ItemStack weapon) {
            var modifiers = AttributeHandler.getItemModifiers(weapon, Attributes.ATTACK_DAMAGE, modifier ->
                    !modifier.is(ResourceLocation.parse("minecraft:base_attack_damage"))
            );
            return AttributeHandler.calculateAttribute(original, modifiers, Attributes.ATTACK_DAMAGE);
        }
    }
}
