package li.cil.scannable.api.scanning;

/**
 * Describes different render contexts in which results may be rendered.
 */
public enum ScanResultRenderContext {
    /**
     * World render context, currently rendering into the regular world render target, right after all block rendering
     * has been performed (specifically in the context of {@link net.minecraftforge.client.event.RenderLevelStageEvent}
     * with stage {@link net.minecraftforge.client.event.RenderLevelStageEvent.Stage#AFTER_TRIPWIRE_BLOCKS}).
     */
    WORLD,

    /**
     * GUI render context, currently rendering for UI. However, OpenGL state has been adjusted such that matrices are
     * the same as when rendering in the {@link #WORLD} context, to facilitate rendering overlays at the result's
     * world position.
     */
    GUI,
}
