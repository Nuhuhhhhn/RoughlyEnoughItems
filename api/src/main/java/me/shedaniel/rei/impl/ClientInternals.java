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

package me.shedaniel.rei.impl;

import com.mojang.serialization.DataResult;
import dev.architectury.utils.value.BooleanValue;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.ClientHelper;
import me.shedaniel.rei.api.client.entry.filtering.FilteringRuleTypeRegistry;
import me.shedaniel.rei.api.client.entry.renderer.EntryRenderer;
import me.shedaniel.rei.api.client.favorites.FavoriteEntry;
import me.shedaniel.rei.api.client.favorites.FavoriteMenuEntry;
import me.shedaniel.rei.api.client.gui.DrawableConsumer;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.*;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.ClickArea;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.InputIngredient;
import me.shedaniel.rei.api.common.plugins.PluginManager;
import me.shedaniel.rei.api.common.transfer.info.stack.SlotAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.data.models.blockstates.PropertyDispatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.apache.commons.io.function.IOQuadFunction;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

@ApiStatus.Internal
public final class ClientInternals {
    private static Supplier<ClientHelper> clientHelper = ClientInternals::throwNotSetup;
    private static Supplier<WidgetsProvider> widgetsProvider = ClientInternals::throwNotSetup;
    private static Supplier<ViewSearchBuilder> viewSearchBuilder = ClientInternals::throwNotSetup;
    private static Supplier<PluginManager<REIClientPlugin>> clientPluginManager = ClientInternals::throwNotSetup;
    private static Supplier<EntryRenderer<?>> emptyEntryRenderer = ClientInternals::throwNotSetup;
    private static Supplier<FilteringRuleTypeRegistry> filteringRuleTypeRegistry = ClientInternals::throwNotSetup;
    private static Supplier<SimpleTransferHandler> simpleTransferHandler = ClientInternals::throwNotSetup;
    private static BiFunction<Supplier<DataResult<FavoriteEntry>>, Supplier<CompoundTag>, FavoriteEntry> delegateFavoriteEntry = (supplier, toJson) -> throwNotSetup();
    private static BiFunction<Component, List<FavoriteMenuEntry>, FavoriteMenuEntry> subMenuEntry = (supplier, toJson) -> throwNotSetup();
    private static BiFunction<Component, BooleanValue, FavoriteMenuEntry> toggleEntry = (supplier, toJson) -> throwNotSetup();
    private static Function<CompoundTag, DataResult<FavoriteEntry>> favoriteEntryFromJson = (object) -> throwNotSetup();
    private static Function<Boolean, ClickArea.Result> clickAreaHandlerResult = (result) -> throwNotSetup();
    private static BiConsumer<List<ClientTooltipComponent>, TooltipComponent> clientTooltipComponentProvider = (tooltip, result) -> throwNotSetup();
    private static BiFunction<@Nullable Point, Collection<Tooltip.Entry>, Tooltip> tooltipProvider = (point, texts) -> throwNotSetup();
    private static PropertyDispatch.QuadFunction<Point, @Nullable TooltipFlag, Boolean, Item.TooltipContext, TooltipContext> tooltipContextProvider = (point, texts, search, context) -> throwNotSetup();
    private static Function<Object, Tooltip.Entry> tooltipEntryProvider = (component) -> throwNotSetup();
    private static Supplier<List<String>> jeiCompatMods = ClientInternals::throwNotSetup;
    private static Supplier<Object> builtinClientPlugin = ClientInternals::throwNotSetup;
    private static Function<List<EntryIngredient>, TooltipComponent> missingTooltip = (stacks) -> throwNotSetup();
    
    private static <T> T throwNotSetup() {
        throw new AssertionError("REI Internals have not been initialized!");
    }
    
    @ApiStatus.Internal
    public static <T> void attachInstance(T instance, Class<T> clazz) {
        attachInstanceSupplier(instance, clazz.getSimpleName());
    }
    
    @ApiStatus.Internal
    public static <T> void attachInstanceSupplier(T instance, String name) {
        attachInstance((Supplier<T>) () -> instance, name);
    }
    
    public static <T> void attachInstance(T instance, String name) {
        try {
            for (Field field : ClientInternals.class.getDeclaredFields()) {
                if (field.getName().equalsIgnoreCase(name)) {
                    field.setAccessible(true);
                    field.set(null, instance);
                    return;
                }
            }
            throw new RuntimeException("Failed to attach " + instance + " with field name: " + name);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static ClientHelper getClientHelper() {
        return clientHelper.get();
    }
    
    public static WidgetsProvider getWidgetsProvider() {
        return widgetsProvider.get();
    }
    
    public static ViewSearchBuilder createViewSearchBuilder() {
        return viewSearchBuilder.get();
    }
    
    public static Object getBuiltinPlugin() {
        return builtinClientPlugin.get();
    }
    
    public static ClickArea.Result createClickAreaHandlerResult(boolean applicable) {
        return clickAreaHandlerResult.apply(applicable);
    }
    
    public static void getClientTooltipComponent(List<ClientTooltipComponent> tooltip, TooltipComponent component) {
        clientTooltipComponentProvider.accept(tooltip, component);
    }
    
    public static Tooltip createTooltip(@Nullable Point point, Collection<Tooltip.Entry> texts) {
        return tooltipProvider.apply(point, texts);
    }
    
    public static TooltipContext createTooltipContext(Point point, @Nullable TooltipFlag flag, boolean isSearch, Item.TooltipContext context) {
        return tooltipContextProvider.apply(point, flag, isSearch, context);
    }
    
    public static Tooltip.Entry createTooltipEntry(Object component) {
        return tooltipEntryProvider.apply(component);
    }
    
    public static FavoriteEntry delegateFavoriteEntry(Supplier<DataResult<FavoriteEntry>> supplier, Supplier<CompoundTag> toJoin) {
        return delegateFavoriteEntry.apply(supplier, toJoin);
    }
    
    public static FavoriteMenuEntry createSubMenuEntry(Component name, List<FavoriteMenuEntry> entries) {
        return subMenuEntry.apply(name, entries);
    }
    
    public static FavoriteMenuEntry createToggleEntry(Component name, BooleanValue value) {
        return toggleEntry.apply(name, value);
    }
    
    public static DataResult<FavoriteEntry> favoriteEntryFromJson(CompoundTag tag) {
        return favoriteEntryFromJson.apply(tag);
    }
    
    public static <T> EntryRenderer<T> getEmptyEntryRenderer() {
        return emptyEntryRenderer.get().cast();
    }
    
    public static List<String> getJeiCompatMods() {
        return jeiCompatMods.get();
    }
    
    public static PluginManager<REIClientPlugin> getPluginManager() {
        return clientPluginManager.get();
    }
    
    public static TooltipComponent createMissingTooltip(List<EntryIngredient> stacks) {
        return missingTooltip.apply(stacks);
    }
    
    public static FilteringRuleTypeRegistry getFilteringRuleTypeRegistry() {
        return filteringRuleTypeRegistry.get();
    }
    
    public static SimpleTransferHandler getSimpleTransferHandler() {
        return simpleTransferHandler.get();
    }
    
    @Environment(EnvType.CLIENT)
    public interface WidgetsProvider {
        boolean isRenderingPanel(Panel panel);
        
        Widget wrapVanillaWidget(GuiEventListener element);
        
        WidgetWithBounds wrapRenderer(Supplier<Rectangle> bounds, Renderer renderer);
        
        WidgetWithBounds withTranslate(WidgetWithBounds widget, Supplier<Matrix4f> translate);
        
        Widget createDrawableWidget(DrawableConsumer drawable);
        
        Slot createSlot(Point point);
        
        Slot createSlot(Rectangle bounds);
        
        Button createButton(Rectangle bounds, Component text);
        
        Panel createPanelWidget(Rectangle bounds);
        
        Label createLabel(Point point, FormattedText text);
        
        Arrow createArrow(Rectangle rectangle);
        
        BurningFire createBurningFire(Rectangle rectangle);
        
        DrawableConsumer createTexturedConsumer(ResourceLocation texture, int x, int y, int width, int height, float u, float v, int uWidth, int vHeight, int textureWidth, int textureHeight);
        
        DrawableConsumer createFillRectangleConsumer(Rectangle rectangle, int color);
        
        Widget createShapelessIcon(Point point);
        
        Widget concatWidgets(List<Widget> widgets);
        
        WidgetWithBounds concatWidgetsWithBounds(Supplier<Rectangle> bounds, List<Widget> widgets);
        
        WidgetWithBounds noOp();
        
        WidgetWithBounds wrapOverflow(Rectangle bounds, WidgetWithBounds widget);
        
        WidgetWithBounds wrapScissored(Rectangle bounds, Widget widget);
        
        WidgetWithBounds wrapScissored(WidgetWithBounds widget);
        
        WidgetWithBounds wrapPadded(int padLeft, int padRight, int padTop, int padBottom, WidgetWithBounds widget);
    }
    
    public interface SimpleTransferHandler {
        TransferHandler.Result handle(TransferHandler.Context context, me.shedaniel.rei.api.client.registry.transfer.simple.SimpleTransferHandler.MissingInputRenderer missingInputRenderer, List<InputIngredient<ItemStack>> inputs, Iterable<SlotAccessor> inputSlots, Iterable<SlotAccessor> inventorySlots);
    }
}
