package com.xm666.spartanapothiccompat;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.Arrays;
import java.util.function.Predicate;

public class AttributeHandler {
    public static double calculateAttribute(LivingEntity living, Holder<Attribute> attribute, Predicate<AttributeModifier> predicate) {
        var baseValue = living.getAttributeBaseValue(attribute);
        var modifiers = getAttributeModifiers(living, attribute, predicate);
        return calculateAttribute(baseValue, modifiers, attribute);
    }

    public static AttributeModifier[] getItemModifiers(ItemStack stack, Holder<Attribute> attribute, Predicate<AttributeModifier> predicate) {
        return stack.getAttributeModifiers().modifiers().stream()
                .filter(entry -> entry.attribute() == attribute && predicate.test(entry.modifier()))
                .map(ItemAttributeModifiers.Entry::modifier)
                .toArray(AttributeModifier[]::new);
    }

    public static double calculateAttribute(double baseValue, AttributeModifier[] modifiers, Holder<Attribute> attribute) {
        return attribute.value().sanitizeValue(calculateAttribute(baseValue, modifiers));
    }

    private static double calculateAttribute(double baseValue, AttributeModifier[] modifiers) {
        for (var attributeModifier : Arrays.stream(modifiers).filter(m -> m.operation() == AttributeModifier.Operation.ADD_VALUE).toArray(AttributeModifier[]::new)) {
            baseValue += attributeModifier.amount();
        }

        var value = baseValue;

        for (var attributeModifier : Arrays.stream(modifiers).filter(m -> m.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE).toArray(AttributeModifier[]::new)) {
            value += baseValue * attributeModifier.amount();
        }

        for (var attributeModifier : Arrays.stream(modifiers).filter(m -> m.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL).toArray(AttributeModifier[]::new)) {
            value *= 1.0 + attributeModifier.amount();
        }

        return value;
    }

    private static AttributeModifier[] getAttributeModifiers(LivingEntity living, Holder<Attribute> attribute, Predicate<AttributeModifier> predicate) {
        var attributeInstance = living.getAttribute(attribute);
        if (attributeInstance == null) return new AttributeModifier[0];

        return attributeInstance.getModifiers().stream().filter(predicate).toArray(AttributeModifier[]::new);
    }
}
