package com.xm666.spartanapothiccompat.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.oblivioussp.spartanweaponry.entity.projectile.ThrowingWeaponEntity;
import com.xm666.spartanapothiccompat.AttributeHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

public class SpartanMixin {
    @Mixin(ThrowingWeaponEntity.class)
    private static class ThrowingWeaponEntityMixin {
        @ModifyVariable(method = "onHitEntity", at = @At(value = "STORE"), name = "src")
        private static DamageSource onSetDamageSource(DamageSource damageSource, @Local(name = "weapon") ItemStack weapon, @Local(name = "entity") Entity entity, @Local(name = "damage") LocalFloatRef damage) {
            if (!(entity instanceof LivingEntity living)) return damageSource;

            damage.set(damage.get() + EnchantmentHelper.getDamageBonus(weapon, living.getMobType()));
            return damageSource;
        }

        @ModifyExpressionValue(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lcom/oblivioussp/spartanweaponry/entity/projectile/ThrowingWeaponEntity;getBaseDamage()D"))
        private double modifyBaseDamage(double original, @Local(name = "weapon") ItemStack weapon) {
            var modifiers = AttributeHandler.getItemModifiers(weapon, Attributes.ATTACK_DAMAGE, modifier ->
                    modifier.getId() != Item.BASE_ATTACK_DAMAGE_UUID
            );
            return AttributeHandler.calculateAttribute(original, modifiers, Attributes.ATTACK_DAMAGE);
        }
    }
}
