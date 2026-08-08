package com.xm666.spartanapothiccompat.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.oblivioussp.spartanweaponry.api.WeaponTraits;
import com.oblivioussp.spartanweaponry.entity.projectile.ThrowingWeaponEntity;
import com.oblivioussp.spartanweaponry.init.ModDamageTypes;
import com.oblivioussp.spartanweaponry.item.SwordBaseItem;
import com.oblivioussp.spartanweaponry.item.ThrowingWeaponItem;
import dev.shadowsoffire.apotheosis.adventure.affix.effect.PotionAffix;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.attributeslib.impl.AttributeEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

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

    @Mixin(PotionAffix.class)
    private static class PotionAffixMixin {
        @Shadow(remap = false)
        @Final
        protected PotionAffix.Target target;

        @ModifyExpressionValue(method = "doPostAttack", at = @At(value = "FIELD", target = "Ldev/shadowsoffire/apotheosis/adventure/affix/effect/PotionAffix$Target;ATTACK_SELF:Ldev/shadowsoffire/apotheosis/adventure/affix/effect/PotionAffix$Target;", opcode = Opcodes.GETSTATIC, remap = false), remap = false)
        private PotionAffix.Target modifyAttackSelf(PotionAffix.Target original, ItemStack stack) {
            var item = stack.getItem();
            if (!(item instanceof ThrowingWeaponItem)
                    && !(item instanceof SwordBaseItem swordBaseItem
                    && swordBaseItem.hasWeaponTraitWithType(WeaponTraits.TYPE_THROWABLE))) return original;

            return target == PotionAffix.Target.ARROW_SELF ? PotionAffix.Target.ARROW_SELF : original;
        }

        @ModifyExpressionValue(method = "doPostAttack", at = @At(value = "FIELD", target = "Ldev/shadowsoffire/apotheosis/adventure/affix/effect/PotionAffix$Target;ATTACK_TARGET:Ldev/shadowsoffire/apotheosis/adventure/affix/effect/PotionAffix$Target;", opcode = Opcodes.GETSTATIC, remap = false), remap = false)
        private PotionAffix.Target modifyAttackTarget(PotionAffix.Target original, ItemStack stack) {
            var item = stack.getItem();
            if (!(item instanceof ThrowingWeaponItem)
                    && !(item instanceof SwordBaseItem swordBaseItem
                    && swordBaseItem.hasWeaponTraitWithType(WeaponTraits.TYPE_THROWABLE))) return original;

            return target == PotionAffix.Target.ARROW_TARGET ? PotionAffix.Target.ARROW_TARGET : original;
        }
    }
}
