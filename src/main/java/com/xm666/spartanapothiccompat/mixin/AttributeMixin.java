package com.xm666.spartanapothiccompat.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.oblivioussp.spartanweaponry.api.WeaponTraits;
import com.oblivioussp.spartanweaponry.entity.projectile.ThrowingWeaponEntity;
import com.oblivioussp.spartanweaponry.init.ModDamageTypes;
import com.oblivioussp.spartanweaponry.item.SwordBaseItem;
import com.oblivioussp.spartanweaponry.item.ThrowingWeaponItem;
import com.xm666.spartanapothiccompat.AttributeHandler;
import dev.shadowsoffire.attributeslib.impl.AttributeEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

public class AttributeMixin {
    private static class DamageMixin {
        @Mixin(ThrowingWeaponEntity.class)
        private static class ThrowingWeaponEntityMixin {
            @ModifyExpressionValue(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lcom/oblivioussp/spartanweaponry/entity/projectile/ThrowingWeaponEntity;getBaseDamage()D"))
            private double modifyBaseDamage(double original, @Local(name = "weapon") ItemStack weapon) {
                var modifiers = AttributeHandler.getItemModifiers(weapon, Attributes.ATTACK_DAMAGE, modifier ->
                        modifier.getId() != Item.BASE_ATTACK_DAMAGE_UUID
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

            @WrapMethod(method = "meleeDamageAttributes", remap = false)
            private void wrapMeleeDamageAttributes(LivingAttackEvent event, Operation<Void> original) {
                var source = event.getSource();
                if (!source.is(ModDamageTypes.KEY_THROWN_WEAPON_PLAYER) && !source.is(ModDamageTypes.KEY_THROWN_WEAPON_MOB)) {
                    original.call(event);
                    return;
                }

                if (!(source.getDirectEntity() instanceof ThrowingWeaponEntity weaponEntity)) {
                    original.call(event);
                    return;
                }

                var weaponStack = weaponEntity.getWeaponItem();
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

            @WrapMethod(method = "lifeStealOverheal", remap = false)
            private void wrapLifeStealOverheal(LivingHurtEvent event, Operation<Void> original) {
                var source = event.getSource();
                if (!source.is(ModDamageTypes.KEY_THROWN_WEAPON_PLAYER) && !source.is(ModDamageTypes.KEY_THROWN_WEAPON_MOB)) {
                    original.call(event);
                    return;
                }

                if (!(source.getDirectEntity() instanceof ThrowingWeaponEntity weaponEntity)) {
                    original.call(event);
                    return;
                }

                var weaponStack = weaponEntity.getWeaponItem();
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

            @ModifyReturnValue(method = "canBenefitFromDrawSpeed", at = @At(value = "RETURN"), remap = false)
            private boolean modifyBenefitFromDrawSpeed(boolean benefit, ItemStack stack) {
                return benefit
                        || stack.getItem() instanceof ThrowingWeaponItem
                        || stack.getItem() instanceof SwordBaseItem swordBaseItem
                        && swordBaseItem.hasWeaponTraitWithType(WeaponTraits.TYPE_THROWABLE);
            }
        }
    }
}
