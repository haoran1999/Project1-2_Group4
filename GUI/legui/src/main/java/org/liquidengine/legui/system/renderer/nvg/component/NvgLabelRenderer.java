package org.liquidengine.legui.system.renderer.nvg.component;

import org.joml.Vector2f;
import org.joml.Vector4f;
import org.liquidengine.legui.component.Label;
import org.liquidengine.legui.component.optional.TextState;
import org.liquidengine.legui.component.optional.align.HorizontalAlign;
import org.liquidengine.legui.component.optional.align.VerticalAlign;
import org.liquidengine.legui.style.Style;
import org.liquidengine.legui.style.font.FontRegistry;
import org.liquidengine.legui.system.context.Context;
import org.liquidengine.legui.system.renderer.nvg.util.NvgText;

import static org.liquidengine.legui.style.util.StyleUtilities.*;
import static org.liquidengine.legui.system.renderer.nvg.util.NvgRenderUtils.createScissor;
import static org.liquidengine.legui.system.renderer.nvg.util.NvgRenderUtils.resetScissor;

/**
 * Created by ShchAlexander on 11.02.2017.
 */
public class NvgLabelRenderer extends NvgDefaultComponentRenderer<Label> {

    @Override
    public void renderSelf(Label label, Context context, long nanovg) {
        createScissor(nanovg, label);
        {
            Style style = label.getStyle();
            Vector2f pos = label.getAbsolutePosition();
            Vector2f size = label.getSize();

            /*Draw background rectangle*/
            renderBackground(label, context, nanovg);

            // draw text into box
            TextState textState = label.getTextState();
            Vector4f padding = getPadding(label, style);
            Vector4f rect = getInnerContentRectangle(pos, size, padding);
            NvgText.drawTextLineToRect(nanovg, rect, false,
                    getStyle(label, Style::getHorizontalAlign, HorizontalAlign.LEFT),
                    getStyle(label, Style::getVerticalAlign, VerticalAlign.MIDDLE),
                    getStyle(label, Style::getFontSize, 16F),
                    getStyle(label, Style::getFont, FontRegistry.getDefaultFont()),
                    textState.getText(),
                    getStyle(label, Style::getTextColor),
                    label.getTextDirection());
        }
        resetScissor(nanovg);
    }
}
