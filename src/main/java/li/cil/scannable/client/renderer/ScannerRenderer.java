package li.cil.scannable.client.renderer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import li.cil.scannable.client.ScanManager;
import li.cil.scannable.client.shader.Shaders;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

@Environment(EnvType.CLIENT)
public enum ScannerRenderer {
    INSTANCE;

    // --------------------------------------------------------------------- //
    // Settings

    // --------------------------------------------------------------------- //
    // Frame buffer and depth texture IDs.

    private int depthCopyFbo;
    private int depthCopyColorBuffer;
    private int depthCopyDepthBuffer;

    // --------------------------------------------------------------------- //
    // State of the scanner, set when triggering a ping.

    private long currentStart;
    private Vec3 currentCenter;

    // --------------------------------------------------------------------- //

    public void ping(final Vec3 pos) {
        currentStart = System.currentTimeMillis();
        currentCenter = pos;
    }

    public static void render(final PoseStack poseStack) {
        INSTANCE.doRender(poseStack);
    }

    public void doRender(final PoseStack poseStack) {
        final int adjustedDuration = ScanManager.computeScanGrowthDuration();
        final boolean shouldRender = currentStart > 0 && adjustedDuration > (int) (System.currentTimeMillis() - currentStart);
        if (shouldRender) {
            if (depthCopyFbo == 0) {
                createDepthCopyBuffer();
            }

            render(poseStack.last().pose());
        } else {
            if (depthCopyFbo != 0) {
                deleteDepthCopyBuffer();
            }

            currentStart = 0;
        }
    }

    private void render(final Matrix4f viewMatrix) {
        final ShaderInstance shader = Shaders.getScanEffectShader();
        if (shader == null) {
            return;
        }

        final RenderTarget target = Minecraft.getInstance().getMainRenderTarget();

        updateDepthTexture(target);

        updateShaderUniforms(shader, viewMatrix);

        blit(target);
    }

    private void updateDepthTexture(final RenderTarget target) {
        final int oldBuffer = GlStateManager.getBoundFramebuffer();
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, target.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, depthCopyFbo);
        GL30.glBlitFramebuffer(0, 0, target.width, target.height,
                0, 0, target.width, target.height,
                GL30.GL_DEPTH_BUFFER_BIT, GL30.GL_NEAREST);
        GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, oldBuffer);
    }

    private void updateShaderUniforms(final ShaderInstance shader, final Matrix4f viewMatrix) {
        final Matrix4f invertedViewMatrix = new Matrix4f(viewMatrix);
        invertedViewMatrix.invert();

        final Matrix4f invertedProjectionMatrix = new Matrix4f(RenderSystem.getProjectionMatrix());
        invertedProjectionMatrix.invert();

        final Vec3 cameraPosition = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        final int adjustedDuration = ScanManager.computeScanGrowthDuration();
        final float radius = ScanManager.computeRadius(currentStart, (float) adjustedDuration);

        shader.setSampler("depthTex", depthCopyDepthBuffer);
        shader.safeGetUniform("center").set(new Vector3f(currentCenter));
        shader.safeGetUniform("invViewMat").set(invertedViewMatrix);
        shader.safeGetUniform("invProjMat").set(invertedProjectionMatrix);
        shader.safeGetUniform("pos").set(new Vector3f(cameraPosition));
        shader.safeGetUniform("radius").set(radius);
    }

    private void blit(final RenderTarget target) {
        final int width = target.width;
        final int height = target.height;

        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();

        final ShaderInstance oldShader = RenderSystem.getShader();
        RenderSystem.setShader(Shaders::getScanEffectShader);

        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(Matrix4f.orthographic(0, width, 0, height, 1, 100));

        final Tesselator tesselator = Tesselator.getInstance();
        final BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(0, height, -50).uv(0, 0).endVertex();
        buffer.vertex(width, height, -50).uv(1, 0).endVertex();
        buffer.vertex(width, 0, -50).uv(1, 1).endVertex();
        buffer.vertex(0, 0, -50).uv(0, 1).endVertex();
        tesselator.end();

        RenderSystem.restoreProjectionMatrix();

        RenderSystem.setShader(() -> oldShader);

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    // --------------------------------------------------------------------- //

    private void createDepthCopyBuffer() {
        final RenderTarget target = Minecraft.getInstance().getMainRenderTarget();

        depthCopyFbo = GlStateManager.glGenFramebuffers();

        // We don't use the color attachment on this FBO, but it's required for a complete FBO.
        depthCopyColorBuffer = createTexture(target.width, target.height, GL11.GL_RGBA8, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE);

        // Main reason why we create this FBO: readable depth buffer into which we can copy the MC one.
        depthCopyDepthBuffer = createTexture(target.width, target.height, GL30.GL_DEPTH_COMPONENT, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT);

        final int oldBuffer = GlStateManager.getBoundFramebuffer();
        GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, depthCopyFbo);
        GlStateManager._glFramebufferTexture2D(GlConst.GL_FRAMEBUFFER, GlConst.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, depthCopyColorBuffer, 0);
        GlStateManager._glFramebufferTexture2D(GlConst.GL_FRAMEBUFFER, GlConst.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, depthCopyDepthBuffer, 0);
        GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, oldBuffer);
    }

    private void deleteDepthCopyBuffer() {
        GlStateManager._glDeleteFramebuffers(depthCopyFbo);
        depthCopyFbo = 0;

        TextureUtil.releaseTextureId(depthCopyColorBuffer);
        depthCopyColorBuffer = 0;

        TextureUtil.releaseTextureId(depthCopyDepthBuffer);
        depthCopyDepthBuffer = 0;
    }

    private int createTexture(final int width, final int height, final int internalFormat, final int format, final int type) {
        final int texture = TextureUtil.generateTextureId();
        GlStateManager._bindTexture(texture);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, null);
        GlStateManager._bindTexture(0);
        return texture;
    }
}
