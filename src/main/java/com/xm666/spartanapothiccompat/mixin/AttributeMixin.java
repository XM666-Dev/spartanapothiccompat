package com.xm666.spartanapothiccompat.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.xm666.spartanapothiccompat.AttributeHandler;
import dev.shadowsoffire.apothic_attributes.impl.AttributeEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.xiyu.spartanweaponryunofficial.api.WeaponTraits;
import org.xiyu.spartanweaponryunofficial.entity.projectile.ThrowingWeaponEntity;
import org.xiyu.spartanweaponryunofficial.init.ModDamageTypes;
import org.xiyu.spartanweaponryunofficial.item.SwordBaseItem;
import org.xiyu.spartanweaponryunofficial.item.ThrowingWeaponItem;

public class AttributeMixin {
    private static class DamageMixin {
        @Mixin(ThrowingWeaponEntity.class)
        private static class ThrowingWeaponEntityMixin {
            @ModifyExpressionValue(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lorg/xiyu/spartanweaponryunofficial/entity/projectile/ThrowingWeaponEntity;getBaseDamage()D"))
            private double modifyBaseDamage(double original, @Local(name = "weapon") ItemStack weapon) {
                var modifiers = AttributeHandler.getItemModifiers(weapon, Attributes.ATTACK_DAMAGE, modifier ->
                        !modifier.is(ResourceLocation.parse("minecraft:base_attack_damage"))
                );
                return AttributeHandler.calculateAttribute(original, modifiers, Attributes.ATTACK_DAMAGE);
            }
        }
    }

    private static class EffectMixin {
        @Mixin(AttributeEvents.class)
        private static class AttributeEventsMixin {
            @WrapOperation(method = "meleeDamageAttributes", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSource;getDirectEntity()Lnet/minecraft/world/entity/Entity;"))
            private Entity wrapMeleeDamageAttributesDirectEntity(DamageSource instance, Operation<Entity> original) {
                if (!instance.is(ModDamageTypes.KEY_THROWN_WEAPON_PLAYER) && !instance.is(ModDamageTypes.KEY_THROWN_WEAPON_MOB))
                    return original.call(instance);

                return instance.getEntity();
            }

            @WrapOperation(method = "lifeStealOverheal", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSource;getDirectEntity()Lnet/minecraft/world/entity/Entity;"))
            private Entity wrapLifeStealOverhealDirectEntity(DamageSource instance, Operation<Entity> original) {
                if (!instance.is(ModDamageTypes.KEY_THROWN_WEAPON_PLAYER) && !instance.is(ModDamageTypes.KEY_THROWN_WEAPON_MOB))
                    return original.call(instance);

                return instance.getEntity();
            }

            @WrapMethod(method = "meleeDamageAttributes")
            private void wrapMeleeDamageAttributesDirectEntity(LivingIncomingDamageEvent event, Operation<Void> original) {
                var source = event.getSource();
                if (!source.is(ModDamageTypes.KEY_THROWN_WEAPON_PLAYER) && !source.is(ModDamageTypes.KEY_THROWN_WEAPON_MOB)) {
                    original.call(event);
                    return;
                }

                var weaponStack = source.getWeaponItem();
                if (weaponStack == null) {
                    original.call(event);
                    return;
                }

                var entity = event.getSource().getEntity();
                if (!(entity instanceof LivingEntity attacker)) {
                    original.call(event);
                    return;
                }

                var originalStack = attacker.getItemInHand(InteractionHand.MAIN_HAND);
                attacker.setItemInHand(InteractionHand.MAIN_HAND, weaponStack);
                attacker.detectEquipmentUpdates();
                original.call(event);
                attacker.setItemInHand(InteractionHand.MAIN_HAND, originalStack);
            }

            @WrapMethod(method = "lifeStealOverheal")
            private void wrapLifeStealOverheal(LivingDamageEvent.Post event, Operation<Void> original) {
                var source = event.getSource();
                if (!source.is(ModDamageTypes.KEY_THROWN_WEAPON_PLAYER) && !source.is(ModDamageTypes.KEY_THROWN_WEAPON_MOB)) {
                    original.call(event);
                    return;
                }

                var weaponStack = source.getWeaponItem();
                if (weaponStack == null) {
                    original.call(event);
                    return;
                }

                var entity = event.getSource().getEntity();
                if (!(entity instanceof LivingEntity attacker)) {
                    original.call(event);
                    return;
                }

                var originalStack = attacker.getItemInHand(InteractionHand.MAIN_HAND);
                attacker.setItemInHand(InteractionHand.MAIN_HAND, weaponStack);
                attacker.detectEquipmentUpdates();
                original.call(event);
                attacker.setItemInHand(InteractionHand.MAIN_HAND, originalStack);
            }

            @ModifyReturnValue(method = "canBenefitFromDrawSpeed", at = @At(value = "RETURN"))
            private boolean modifyBenefitFromDrawSpeed(boolean benefit, ItemStack stack) {
                return benefit
                        || stack.getItem() instanceof ThrowingWeaponItem
                        || stack.getItem() instanceof SwordBaseItem swordBaseItem
                        && swordBaseItem.hasWeaponTraitWithType(WeaponTraits.TYPE_THROWABLE);
            }
        }
    }
}
