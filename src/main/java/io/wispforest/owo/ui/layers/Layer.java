package io.wispforest.owo.ui.layers;

import io.wispforest.owo.ui.core.*;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Layer<S extends Screen, R extends ParentComponent> {

    protected final BiFunction<Sizing, Sizing, R> rootComponentMaker;
    protected final Consumer<Layer<S, R>.Instance> instanceInitializer;

    protected Layer(BiFunction<Sizing, Sizing, R> rootComponentMaker, Consumer<Layer<S, R>.Instance> instanceInitializer) {
        this.rootComponentMaker = rootComponentMaker;
        this.instanceInitializer = instanceInitializer;
    }

    public Instance instantiate(S screen) {
        return new Instance(screen);
    }

    public class Instance {

        /**
         * The screen this instance is attached to
         */
        public final S screen;

        /**
         * The UI adapter of this instance - get the {@link OwoUIAdapter#rootComponent}
         * from this to start building your UI tree
         */
        public final OwoUIAdapter<R> adapter;

        /**
         * Whether this layer should aggressively update widget-relative
         * positioning every frame - useful if the targeted widget moves frequently
         */
        public boolean aggressivePositioning = false;

        protected final List<Runnable> layoutUpdaters = new ArrayList<>();

        protected Instance(S screen) {
            this.screen = screen;
            this.adapter = OwoUIAdapter.createWithoutScreen(0, 0, screen.width, screen.height, Layer.this.rootComponentMaker);
            Layer.this.instanceInitializer.accept(this);
        }

        @ApiStatus.Internal
        public void resize(int width, int height) {
            this.adapter.moveAndResize(0, 0, width, height);
        }

        /**
         * Find a widget in the attached screen's widget tree
         *
         * @param locator A predicate to match which identifies the targeted widget
         * @return The targeted widget, or {@link null} if the predicate was never matched
         */
        public @Nullable ClickableWidget queryWidget(Predicate<ClickableWidget> locator) {
            var widgets = new ArrayList<ClickableWidget>();
            for (var element : this.screen.children()) collectChildren(element, widgets);

            ClickableWidget widget = null;
            for (var candidate : widgets) {
                if (!locator.test(candidate)) continue;
                widget = candidate;
                break;
            }

            return widget;
        }

        /**
         * Align the given component to a widget in the attached screen's
         * widget tree. The widget is located by passing the locator predicate to
         * {@link #queryWidget(Predicate)} and getting the position of the resulted widget.
         * <p>
         * If no widget can be found, the component gets positioned at 0,0
         *
         * @param locator       A predicate to match which identifies the targeted widget
         * @param anchor        On which side of the targeted widget to anchor the component
         * @param justification How far along the anchor side of the widget in positive axis direction
         *                      to position the component
         * @param component     The component to position
         */
        public void alignComponentToWidget(Predicate<ClickableWidget> locator, AnchorSide anchor, float justification, Component component) {
            this.layoutUpdaters.add(() -> {
                var widget = this.queryWidget(locator);

                if (widget == null) {
                    component.positioning(Positioning.absolute(0, 0));
                    return;
                }

                var size = component.fullSize();
                switch (anchor) {
                    case TOP -> component.positioning(Positioning.absolute(
                            (int) (widget.x + (widget.getWidth() - size.width()) * justification),
                            widget.y - size.height()
                    ));
                    case RIGHT -> component.positioning(Positioning.absolute(
                            widget.x + widget.getWidth(),
                            (int) (widget.y + (widget.getHeight() - size.height()) * justification)
                    ));
                    case BOTTOM -> component.positioning(Positioning.absolute(
                            (int) (widget.x + (widget.getWidth() - size.width()) * justification),
                            widget.y + widget.getHeight()
                    ));
                    case LEFT -> component.positioning(Positioning.absolute(
                            widget.x - size.width(),
                            (int) (widget.y + (widget.getHeight() - size.height()) * justification)
                    ));
                }
            });
        }

        @ApiStatus.Internal
        public void dispatchLayoutUpdates() {
            this.layoutUpdaters.forEach(Runnable::run);
        }

        private static void collectChildren(Element element, List<ClickableWidget> children) {
            if (element instanceof ClickableWidget widget) children.add(widget);
//            if (element instanceof WrapperWidgetInvoker wrapper) {
//                for (var widget : wrapper.owo$wrappedWidgets()) {
//                    collectChildren(widget, children);
//                }
//            }
        }

        public enum AnchorSide {
            TOP, BOTTOM, LEFT, RIGHT
        }
    }

}