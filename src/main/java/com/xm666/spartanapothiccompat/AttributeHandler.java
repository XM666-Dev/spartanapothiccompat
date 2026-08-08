package com.xm666.spartanapothiccompat;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Predicate;

public class AttributeHandler {
    public static double calculateAttribute(LivingEntity living, Attribute attribute, Predicate<AttributeModifier> predicate) {
        var baseValue = living.getAttributeBaseValue(attribute);
        var modifiers = getAttributeModifiers(living, attribute, predicate);
        return calculateAttribute(baseValue, modifiers, attribute);
    }

    public static AttributeModifier[] getItemModifiers(ItemStack stack, Attribute attribute, Predicate<AttributeModifier> predicate) {
        return stack.getAttributeModifiers(EquipmentSlot.MAINHAND).entries().stream()
                .filter(entry -> entry.getKey() == attribute && predicate.test(entry.getValue()))
                .map(Map.Entry::getValue)
                .toArray(AttributeModifier[]::new);
    }

    public static double calculateAttribute(double baseValue, AttributeModifier[] modifiers, Attribute attribute) {
        return attribute.sanitizeValue(calculateAttribute(baseValue, modifiers));
    }

    private static double calculateAttribute(double baseValue, AttributeModifier[] modifiers) {
        for (var attributeModifier : Arrays.stream(modifiers).filter(m -> m.getOperation() == AttributeModifier.Operation.ADDITION).toArray(AttributeModifier[]::new)) {
            baseValue += attributeModifier.getAmount();
        }

        var value = baseValue;

        for (var attributeModifier : Arrays.stream(modifiers).filter(m -> m.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE).toArray(AttributeModifier[]::new)) {
            value += baseValue * attributeModifier.getAmount();
        }

        for (var attributeModifier : Arrays.stream(modifiers).filter(m -> m.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL).toArray(AttributeModifier[]::new)) {
            value *= 1.0 + attributeModifier.getAmount();
        }

        return value;
    }

    private static AttributeModifier[] getAttributeModifiers(LivingEntity living, Attribute attribute, Predicate<AttributeModifier> predicate) {
        var attributeInstance = living.getAttribute(attribute);
        if (attributeInstance == null) return new AttributeModifier[0];

        return attributeInstance.getModifiers().stream().filter(predicate).toArray(AttributeModifier[]::new);
    }
}
