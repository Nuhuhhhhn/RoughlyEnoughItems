/*
 * This file is licensed under the MIT License, part of Roughly Enough Items.
 * Copyright (c) 2018, 2019, 2020, 2021, 2022, 2023 shedaniel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.shedaniel.rei.plugin.client.forge;

import com.google.gson.internal.LinkedTreeMap;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.plugin.client.BuiltinClientPlugin;
import me.shedaniel.rei.plugin.client.DefaultClientPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.brewing.BrewingRecipe;
import net.neoforged.neoforge.common.brewing.IBrewingRecipe;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

public class DefaultClientPluginImpl extends DefaultClientPlugin {
    @Override
    public void registerForgePotions(DisplayRegistry registry, BuiltinClientPlugin clientPlugin) {
        PotionBrewing brewing = Minecraft.getInstance().level.potionBrewing();
        registerVanillaPotions(brewing, registry, clientPlugin);
        for (IBrewingRecipe recipe : brewing.getRecipes()) {
            if (recipe instanceof BrewingRecipe) {
                BrewingRecipe brewingRecipe = (BrewingRecipe) recipe;
                clientPlugin.registerBrewingRecipe(brewingRecipe.getInput(), brewingRecipe.getIngredient(), brewingRecipe.getOutput().copy());
            }
        }
    }
    
    private static void registerVanillaPotions(PotionBrewing brewing, DisplayRegistry registry, BuiltinClientPlugin clientPlugin) {
        Set<Holder<Potion>> potions = Collections.newSetFromMap(new LinkedTreeMap<>(Comparator.comparing(Holder::getRegisteredName), false));
        for (Ingredient container : brewing.containers) {
            for (PotionBrewing.Mix<Potion> mix : brewing.potionMixes) {
                Holder<Potion> from = mix.from();
                Ingredient ingredient = mix.ingredient;
                Holder<Potion> to = mix.to();
                Ingredient base = Ingredient.of(Arrays.stream(container.getItems())
                        .map(ItemStack::copy)
                        .peek(stack -> stack.set(DataComponents.POTION_CONTENTS, new PotionContents(from))));
                ItemStack output = Arrays.stream(container.getItems())
                        .map(ItemStack::copy)
                        .peek(stack -> stack.set(DataComponents.POTION_CONTENTS, new PotionContents(to)))
                        .findFirst().orElse(ItemStack.EMPTY);
                clientPlugin.registerBrewingRecipe(base, ingredient, output);
                potions.add(from);
                potions.add(to);
            }
        }
        for (Holder<Potion> potion : potions) {
            for (PotionBrewing.Mix<Item> mix : brewing.containerMixes) {
                Holder<Item> from = mix.from();
                Ingredient ingredient = mix.ingredient();
                Holder<Item> to = mix.to();
                ItemStack baseStack = new ItemStack(from);
                baseStack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
                Ingredient base = Ingredient.of(baseStack);
                ItemStack output = new ItemStack(to);
                output.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
                clientPlugin.registerBrewingRecipe(base, ingredient, output);
            }
        }
    }
}
