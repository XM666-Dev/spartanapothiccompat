package com.xm666.spartanapothiccompat.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apothic_attributes.impl.AttributeEvents;
import dev.shadowsoffire.apothic_attributes.util.AuxDmgTracker;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.xiyu.spartanweaponryunofficial.api.WeaponTraits;
import org.xiyu.spartanweaponryunofficial.init.ModDamageTypes;
import org.xiyu.spartanweaponryunofficial.item.SwordBaseItem;
import org.xiyu.spartanweaponryunofficial.item.ThrowingWeaponItem;

import java.util.function.Consumer;

public class ApothicMixin {
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

        @ModifyArg(method = "meleeDamageAttributes", at = @At(value = "INVOKE", target = "Ldev/shadowsoffire/apothic_attributes/util/AuxDmgTracker;executeWith(Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V"))
        private Consumer<AuxDmgTracker> wrapMeleeDamageAttributesDirectEntity(Consumer<AuxDmgTracker> consumer, @Local(argsOnly = true) LivingIncomingDamageEvent event, @Local(name = "attacker") LivingEntity attacker) {
            var source = event.getSource();
            if (!source.is(ModDamageTypes.KEY_THROWN_WEAPON_PLAYER) && !source.is(ModDamageTypes.KEY_THROWN_WEAPON_MOB))
                return consumer;

            var weaponStack = source.getWeaponItem();
            if (weaponStack == null) return consumer;

            var originalStack = attacker.getItemInHand(InteractionHand.MAIN_HAND);
            return tracker -> {
                attacker.setItemInHand(InteractionHand.MAIN_HAND, weaponStack);
                attacker.detectEquipmentUpdates();
                consumer.accept(tracker);
                attacker.setItemInHand(InteractionHand.MAIN_HAND, originalStack);
            };
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
}
