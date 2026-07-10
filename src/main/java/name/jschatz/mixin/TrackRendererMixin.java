package name.jschatz.mixin;

import io.github.foundationgames.splinecart.TrackType;
import io.github.foundationgames.splinecart.block.TrackTiesBlockEntity;
import io.github.foundationgames.splinecart.block.entity.TrackTiesBlockEntityRenderer;
import io.github.foundationgames.splinecart.util.Pose;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.joml.Matrix3d;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "io.github.foundationgames.splinecart.block.entity.TrackRenderer", remap = false)
public class TrackRendererMixin {
    private static final float NORMAL_TRACK_U0 = 0.0F;
    private static final float NORMAL_TRACK_U1 = 0.25F;
    private static final float SPECIAL_TRACK_U0 = 0.75F;
    private static final float SPECIAL_TRACK_U1 = 1F;
    private static final float SIDE_TRACK_1_U0 = 2F / 64F;
    private static final float SIDE_TRACK_1_U1 = 3F / 64F;
    private static final float SIDE_TRACK_2_U0 = 3F / 64F;
    private static final float SIDE_TRACK_2_U1 = 4F / 64F;
    private static final double BOTTOM_OFFSET = -1.25 / 16.0;
    private static final double LIFTED_OFFSET = 0.4 / 16.0;

    @Inject(
            method = "renderPart",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void splinecart3d$renderPart(
            World world,
            MatrixStack.Entry entry,
            VertexConsumer buffer,
            Pose start,
            Pose end,
            float u0,
            float u1,
            float vOffset,
            Vector3fc color,
            double t0,
            double t1,
            double[] blockProgress,
            Vector3d origin0,
            Matrix3d basis0,
            Vector3d deriv0,
            int overlay,
            CallbackInfo ci
    ) {
        double progressBefore = blockProgress[0];
        renderPane(world, entry, buffer, start, end, u0, u1, vOffset, color, t0, t1, blockProgress, origin0, basis0, deriv0, overlay, LIFTED_OFFSET);

        if (isBaseTrackPass(vOffset, color)) {
                double bottomY = LIFTED_OFFSET - (BOTTOM_OFFSET / 2);
                double topY = LIFTED_OFFSET + (BOTTOM_OFFSET / 2);
                boolean isSpecialTrack = (u0 != NORMAL_TRACK_U0 || u1 != NORMAL_TRACK_U1);

                float trackU0 = isSpecialTrack ? SPECIAL_TRACK_U0 : NORMAL_TRACK_U0;
                float trackU1 = isSpecialTrack ? SPECIAL_TRACK_U1 : NORMAL_TRACK_U1;

                // 1. Render original top and bottom horizontal panes
                renderPane(world, entry, buffer, start, end, trackU0, trackU1, vOffset, color, t0, t1, new double[] { progressBefore }, origin0, basis0, deriv0, overlay, bottomY);
                renderPane(world, entry, buffer, start, end, trackU0, trackU1, vOffset, color, t0, t1, new double[] { progressBefore }, origin0, basis0, deriv0, overlay, topY);

                // 2. Render the new vertical side walls using your custom texture coordinates
                // Left side wall (xOffset = -0.5)
                renderSidePane(world, entry, buffer, start, end, SIDE_TRACK_1_U0, SIDE_TRACK_1_U1, vOffset, color, t0, t1, new double[] { progressBefore }, origin0, basis0, deriv0, overlay, bottomY, topY, -6.0F / 16.0F);
                renderSidePane(world, entry, buffer, start, end, SIDE_TRACK_2_U0, SIDE_TRACK_2_U1, vOffset, color, t0, t1, new double[] { progressBefore }, origin0, basis0, deriv0, overlay, bottomY, topY, 4.0F / 16.0F);
                
                // Right side wall (xOffset = 0.5)
                renderSidePane(world, entry, buffer, start, end, SIDE_TRACK_1_U0, SIDE_TRACK_1_U1, vOffset, color, t0, t1, new double[] { progressBefore }, origin0, basis0, deriv0, overlay, bottomY, topY, 6.0F / 16.0F);
                renderSidePane(world, entry, buffer, start, end, SIDE_TRACK_2_U0, SIDE_TRACK_2_U1, vOffset, color, t0, t1, new double[] { progressBefore }, origin0, basis0, deriv0, overlay, bottomY, topY, -4.0F / 16.0F);
                

                if (u0 != NORMAL_TRACK_U0 || u1 != NORMAL_TRACK_U1) {
                        renderSidePane(world, entry, buffer, start, end, SIDE_TRACK_1_U0, SIDE_TRACK_1_U1, vOffset, color, t0, t1, new double[] { progressBefore }, origin0, basis0, deriv0, overlay, bottomY, topY, -2.0F / 16.0F);
                        renderSidePane(world, entry, buffer, start, end, SIDE_TRACK_2_U0, SIDE_TRACK_2_U1, vOffset, color, t0, t1, new double[] { progressBefore }, origin0, basis0, deriv0, overlay, bottomY, topY, 1.0F / 16.0F);
                        
                        renderSidePane(world, entry, buffer, start, end, SIDE_TRACK_1_U0, SIDE_TRACK_1_U0, vOffset, color, t0, t1, new double[] { progressBefore }, origin0, basis0, deriv0, overlay, bottomY, topY, 2.0F / 16.0F);
                        renderSidePane(world, entry, buffer, start, end, SIDE_TRACK_2_U0, SIDE_TRACK_2_U0, vOffset, color, t0, t1, new double[] { progressBefore }, origin0, basis0, deriv0, overlay, bottomY, topY, -1.0F / 16.0F);
                }
                System.out.println("Hi");
        }
        

        ci.cancel();
    }

    /**
     * Replaces the flat single-quad end-cap drawn at chain terminals with a
     * closed box matching the cross-section used by {@link #splinecart3d$renderPart}.
     * <p>
     * Draws, in the tie's local frame (using the same MatrixStack-baked basis
     * trick as the vanilla method): a top pane, a bottom pane, four side wall
     * strips (two per side, matching the -6/4 and 6/-4 sixteenth offsets used
     * in the main segment renderer), and a closing end wall that seals the box
     * off at the outward-facing edge.
     */
    @Inject(
            method = "renderExtraTrackEnd",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void splinecart3d$renderExtraTrackEnd(
            MatrixStack.Entry transform,
            VertexConsumer buffer,
            Pose pose,
            int overlay,
            int light,
            TrackTiesBlockEntity prevE,
            TrackTiesBlockEntity nextE,
            CallbackInfo ci
    ) {
        boolean isDeadEnd = (prevE == null) ^ (nextE == null);

        // prevE.nextType()  == type of the connection running from prevE into this tie
        // nextE.prevType()  == type of the connection running from this tie into nextE
        boolean prevSpecial = prevE != null && isSpecialTrack(prevE.nextType());
        boolean nextSpecial = nextE != null && isSpecialTrack(nextE.prevType());
        boolean renderDivider = prevSpecial ^ nextSpecial;

        if (isDeadEnd || renderDivider) {
                float edgeA = (float) (LIFTED_OFFSET - (BOTTOM_OFFSET / 2));
                float edgeB = (float) (LIFTED_OFFSET + (BOTTOM_OFFSET / 2));
                float bottomY = Math.min(edgeA, edgeB);
                float topY = Math.max(edgeA, edgeB);

                var matrices = new MatrixStack();
                matrices.push();
                matrices.peek().getNormalMatrix().set(transform.getNormalMatrix());
                matrices.peek().getPositionMatrix().set(transform.getPositionMatrix());

                var tl = pose.translation();
                matrices.translate(tl.x(), tl.y(), tl.z());

                var entry = matrices.peek();
                var posMat = entry.getPositionMatrix();
                var nmlMat = entry.getNormalMatrix();
                for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 3; y++) {
                        posMat.setRowColumn(x, y, (float) pose.basis().getRowColumn(x, y));
                        nmlMat.setRowColumn(x, y, (float) pose.basis().getRowColumn(x, y));
                }
                }

                if (isDeadEnd) {
                float z0 = -0.5f;
                float z1 = 0f;
                float v0 = 1f;
                float v1 = 0.5f;

                if (nextE == null) {
                        z0 = 0f;
                        z1 = 0.5f;
                        v0 = 0.5f;
                        v1 = 0f;
                }

                emitHorizontalFace(entry, buffer, overlay, light, topY, z0, z1, v0, v1, NORMAL_TRACK_U0, NORMAL_TRACK_U1, true);
                emitHorizontalFace(entry, buffer, overlay, light, bottomY, z0, z1, v0, v1, NORMAL_TRACK_U0, NORMAL_TRACK_U1, false);

                emitSideFace(entry, buffer, overlay, light, -6.0F / 16.0F, bottomY, topY, z0, z1, v0, v1);
                emitSideFace(entry, buffer, overlay, light, 4.0F / 16.0F, bottomY, topY, z0, z1, v0, v1);
                emitSideFace(entry, buffer, overlay, light, 6.0F / 16.0F, bottomY, topY, z0, z1, v0, v1);
                emitSideFace(entry, buffer, overlay, light, -4.0F / 16.0F, bottomY, topY, z0, z1, v0, v1);

                float zOuter = (nextE == null) ? z1 : z0;
                float normalZ = (nextE == null) ? 1f : -1f;
                emitEndCap(entry, buffer, overlay, light, zOuter, bottomY, topY, normalZ);
                }

                if (renderDivider) {
                emitDividerFace(entry, buffer, overlay, light, bottomY, topY);
                }

                matrices.pop();
        }

        ci.cancel();
        }

    private static boolean isBaseTrackPass(float vOffset, Vector3fc color) {
        return vOffset == 0.0F && color.x() == 1.0F && color.y() == 1.0F && color.z() == 1.0F;
    }

    private static boolean isSpecialTrack(TrackType type) {
        return type == TrackType.CHAIN_DRIVE || type == TrackType.MAGNETIC;
    }

        // Vertical pane sitting on the tie (z=0 in its local frame), full track width,
        // bottom edge on the bottom track face and top edge on the top track face.
        private static void emitDividerFace(
                MatrixStack.Entry entry,
                VertexConsumer buffer,
                int overlay,
                int light,
                float bottomY,
                float topY
        ) {
                buffer.vertex(entry, 0.5f, topY, 0f).color(TrackTiesBlockEntityRenderer.WHITE).texture(SPECIAL_TRACK_U1, 0f)
                        .overlay(overlay).light(light).normal(entry, 0, 0, 1);
                buffer.vertex(entry, -0.5f, topY, 0f).color(TrackTiesBlockEntityRenderer.WHITE).texture(SPECIAL_TRACK_U0, 0f)
                        .overlay(overlay).light(light).normal(entry, 0, 0, 1);
                buffer.vertex(entry, -0.5f, bottomY, 0f).color(TrackTiesBlockEntityRenderer.WHITE).texture(SPECIAL_TRACK_U0, 1f)
                        .overlay(overlay).light(light).normal(entry, 0, 0, 1);
                buffer.vertex(entry, 0.5f, bottomY, 0f).color(TrackTiesBlockEntityRenderer.WHITE).texture(SPECIAL_TRACK_U1, 1f)
                        .overlay(overlay).light(light).normal(entry, 0, 0, 1);
        }

    private static void emitHorizontalFace(
            MatrixStack.Entry entry,
            VertexConsumer buffer,
            int overlay,
            int light,
            float y,
            float z0,
            float z1,
            float v0,
            float v1,
            float u0,
            float u1,
            boolean facingUp
    ) {
        float ny = facingUp ? 1f : -1f;

        buffer.vertex(entry, 0.5f, y, z0).color(TrackTiesBlockEntityRenderer.WHITE).texture(u1, v0)
                .overlay(overlay).light(light).normal(entry, 0, ny, 0);
        buffer.vertex(entry, -0.5f, y, z0).color(TrackTiesBlockEntityRenderer.WHITE).texture(u0, v0)
                .overlay(overlay).light(light).normal(entry, 0, ny, 0);
        buffer.vertex(entry, -0.5f, y, z1).color(TrackTiesBlockEntityRenderer.WHITE).texture(u0, v1)
                .overlay(overlay).light(light).normal(entry, 0, ny, 0);
        buffer.vertex(entry, 0.5f, y, z1).color(TrackTiesBlockEntityRenderer.WHITE).texture(u1, v1)
                .overlay(overlay).light(light).normal(entry, 0, ny, 0);
    }

    private static void emitSideFace(
            MatrixStack.Entry entry,
            VertexConsumer buffer,
            int overlay,
            int light,
            float xOffset,
            float bottomY,
            float topY,
            float z0,
            float z1,
            float v0,
            float v1
    ) {
        float nx = xOffset > 0 ? 1f : -1f;

        float u0 = nx * xOffset > 5.0F / 16.0F ? SIDE_TRACK_1_U0 : SIDE_TRACK_2_U0;
        float u1 = nx * xOffset > 5.0F / 16.0F ? SIDE_TRACK_1_U1 : SIDE_TRACK_2_U1;

        buffer.vertex(entry, xOffset, bottomY, z0).color(TrackTiesBlockEntityRenderer.WHITE).texture(u0, v0)
                .overlay(overlay).light(light).normal(entry, nx, 0, 0);
        buffer.vertex(entry, xOffset, topY, z0).color(TrackTiesBlockEntityRenderer.WHITE).texture(u1, v0)
                .overlay(overlay).light(light).normal(entry, nx, 0, 0);
        buffer.vertex(entry, xOffset, topY, z1).color(TrackTiesBlockEntityRenderer.WHITE).texture(u1, v1)
                .overlay(overlay).light(light).normal(entry, nx, 0, 0);
        buffer.vertex(entry, xOffset, bottomY, z1).color(TrackTiesBlockEntityRenderer.WHITE).texture(u0, v1)
                .overlay(overlay).light(light).normal(entry, nx, 0, 0);
    }

    // Placeholder UVs -- revisit once a dedicated end-cap texture region exists.
    private static void emitEndCap(
            MatrixStack.Entry entry,
            VertexConsumer buffer,
            int overlay,
            int light,
            float zOuter,
            float bottomY,
            float topY,
            float normalZ
    ) {
        buffer.vertex(entry, 0.5f, topY, zOuter).color(TrackTiesBlockEntityRenderer.WHITE).texture(0.25f, 0f)
                .overlay(overlay).light(light).normal(entry, 0, 0, normalZ);
        buffer.vertex(entry, -0.5f, topY, zOuter).color(TrackTiesBlockEntityRenderer.WHITE).texture(0f, 0f)
                .overlay(overlay).light(light).normal(entry, 0, 0, normalZ);
        buffer.vertex(entry, -0.5f, bottomY, zOuter).color(TrackTiesBlockEntityRenderer.WHITE).texture(0f, 1f)
                .overlay(overlay).light(light).normal(entry, 0, 0, normalZ);
        buffer.vertex(entry, 0.5f, bottomY, zOuter).color(TrackTiesBlockEntityRenderer.WHITE).texture(0.25f, 1f)
                .overlay(overlay).light(light).normal(entry, 0, 0, normalZ);
    }

    private static void renderPane(
            World world,
            MatrixStack.Entry entry,
            VertexConsumer buffer,
            Pose start,
            Pose end,
            float u0,
            float u1,
            float vOffset,
            Vector3fc color,
            double t0,
            double t1,
            double[] blockProgress,
            Vector3d origin0,
            Matrix3d basis0,
            Vector3d deriv0,
            int overlay,
            double localYOffset
    ) {
        start.interpolate(end, t0, origin0, basis0, deriv0);
        offsetLocalY(origin0, basis0, localYOffset);
        Vector3d norm0 = new Vector3d(0.0, 1.0, 0.0).mul(basis0);

        Vector3d origin1 = new Vector3d(origin0);
        Matrix3d basis1 = new Matrix3d(basis0);
        Vector3d deriv1 = new Vector3d(deriv0);
        start.interpolate(end, t1, origin1, basis1, deriv1);
        offsetLocalY(origin1, basis1, localYOffset);
        Vector3d norm1 = new Vector3d(0.0, 1.0, 0.0).mul(basis1);

        float v0 = (float) blockProgress[0];
        while (v0 > 1.0F) {
            v0 -= 1.0F;
        }

        float v1 = v0 + (float) (deriv0.length() * (t1 - t0));
        blockProgress[0] = v1;
        v1 = 1.0F - v1 + vOffset;
        v0 = 1.0F - v0 + vOffset;

        BlockPos pos0 = new BlockPos(MathHelper.floor(origin0.x()), MathHelper.floor(origin0.y()), MathHelper.floor(origin0.z()));
        BlockPos pos1 = new BlockPos(MathHelper.floor(origin1.x()), MathHelper.floor(origin1.y()), MathHelper.floor(origin1.z()));
        int light0 = WorldRenderer.getLightmapCoordinates(world, pos0);
        int light1 = WorldRenderer.getLightmapCoordinates(world, pos1);

        Vector3f point = new Vector3f();
        emitVertex(buffer, entry, point, origin0, basis0, norm0, 0.5, u0, v0, color, overlay, light0);
        emitVertex(buffer, entry, point, origin0, basis0, norm0, -0.5, u1, v0, color, overlay, light0);
        emitVertex(buffer, entry, point, origin1, basis1, norm1, -0.5, u1, v1, color, overlay, light1);
        emitVertex(buffer, entry, point, origin1, basis1, norm1, 0.5, u0, v1, color, overlay, light1);
    }

    private static void renderSidePane(
        World world,
        MatrixStack.Entry entry,
        VertexConsumer buffer,
        Pose start,
        Pose end,
        float u0,
        float u1,
        float vOffset,
        Vector3fc color,
        double t0,
        double t1,
        double[] blockProgress,
        Vector3d origin0,
        Matrix3d basis0,
        Vector3d deriv0,
        int overlay,
        double bottomY,
        double topY,
        double xOffset
    ) {
        // Sample beginning of segment
        Vector3d originStartBottom = new Vector3d();
        Matrix3d basisStart = new Matrix3d();
        Vector3d derivStart = new Vector3d();

        start.interpolate(end, t0, originStartBottom, basisStart, derivStart);

        Vector3d originStartTop = new Vector3d(originStartBottom);

        offsetLocalY(originStartBottom, basisStart, bottomY);
        offsetLocalY(originStartTop, basisStart, topY);

        // Sample end of segment
        Vector3d originEndBottom = new Vector3d();
        Matrix3d basisEnd = new Matrix3d();
        Vector3d derivEnd = new Vector3d();

        start.interpolate(end, t1, originEndBottom, basisEnd, derivEnd);

        Vector3d originEndTop = new Vector3d(originEndBottom);

        offsetLocalY(originEndBottom, basisEnd, bottomY);
        offsetLocalY(originEndTop, basisEnd, topY);

        // Outward-facing normal
        Vector3d normal = new Vector3d(
                xOffset > 0 ? 1.0 : -1.0,
                0.0,
                0.0
        ).mul(basisStart);

        // Texture progression
        float v0 = (float) blockProgress[0];
        while (v0 > 1.0F) {
            v0 -= 1.0F;
        }

        float v1 = v0 + (float) (derivStart.length() * (t1 - t0));
        blockProgress[0] = v1;

        v1 = 1.0F - v1 + vOffset;
        v0 = 1.0F - v0 + vOffset;

        BlockPos pos0 = new BlockPos(
                MathHelper.floor(originStartBottom.x()),
                MathHelper.floor(originStartBottom.y()),
                MathHelper.floor(originStartBottom.z())
        );

        BlockPos pos1 = new BlockPos(
                MathHelper.floor(originEndBottom.x()),
                MathHelper.floor(originEndBottom.y()),
                MathHelper.floor(originEndBottom.z())
        );

        int light0 = WorldRenderer.getLightmapCoordinates(world, pos0);
        int light1 = WorldRenderer.getLightmapCoordinates(world, pos1);

        Vector3f point = new Vector3f();

        // Entire wall lies on one x position
        emitVertex(
                buffer, entry, point,
                originStartBottom, basisStart, normal,
                xOffset,
                u0, v0,
                color, overlay, light0
        );

        emitVertex(
                buffer, entry, point,
                originStartTop, basisStart, normal,
                xOffset,
                u1, v0,
                color, overlay, light0
        );

        emitVertex(
                buffer, entry, point,
                originEndTop, basisEnd, normal,
                xOffset,
                u1, v1,
                color, overlay, light1
        );

        emitVertex(
                buffer, entry, point,
                originEndBottom, basisEnd, normal,
                xOffset,
                u0, v1,
                color, overlay, light1
        );
    }

    private static void offsetLocalY(Vector3d origin, Matrix3d basis, double amount) {
        origin.add(new Vector3d(0.0, amount, 0.0).mul(basis));
    }

    private static void emitVertex(
            VertexConsumer buffer,
            MatrixStack.Entry entry,
            Vector3f point,
            Vector3d origin,
            Matrix3d basis,
            Vector3d normal,
            double xOffset,
            float u,
            float v,
            Vector3fc color,
            int overlay,
            int light
    ) {
        point.set(xOffset, 0.0, 0.0)
                .mul(basis)
                .add((float) origin.x(), (float) origin.y(), (float) origin.z());

        buffer.vertex(entry, point)
                .color(color.x(), color.y(), color.z(), 1.0F)
                .texture(u, v)
                .overlay(overlay)
                .light(light)
                .normal(entry, (float) normal.x(), (float) normal.y(), (float) normal.z());
    }
}
