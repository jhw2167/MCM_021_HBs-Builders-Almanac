package com.holybuckets.builder.client.render;

import com.holybuckets.builder.LoggerProject;
import com.holybuckets.builder.almanac.AlmanacCatalog;
import com.holybuckets.builder.almanac.AlmanacCategory;
import com.holybuckets.builder.almanac.AlmanacEntry;
import com.holybuckets.builder.core.BuildLayout;
import com.holybuckets.builder.core.BuildShapes;
import com.holybuckets.foundation.event.EventRegistrar;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.blay09.mods.balm.api.event.server.ServerStartedEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.joml.Matrix4f;

import javax.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * We mimic the mini-pixel rendering style of Chisel and Bits by sampling structures
 * into 16^3 grid cells to make a little rendering image for users to recognize builds
 *
 * Some AI code in here, but its just a codec algorithm for prefab renders. Not runtime code.
 */
public class BuildIconMesher {

    public static final String CLASS_ID = "006";
    public static final int ICON_SIZE = 16;

    // Vanilla directional face shading, indexed by Direction ordinal (DOWN, UP, NORTH, SOUTH, WEST, EAST)
    private static final float[] SHADE = {0.5F, 1.0F, 0.8F, 0.8F, 0.6F, 0.6F};

    private static final Map<String, IconMesh> MESHES = new HashMap<>();


    public static void init(EventRegistrar reg) {
        reg.registerOnServerStarted(BuildIconMesher::onServerStarted);
    }

    @Nullable
    public static IconMesh getMesh(AlmanacEntry entry) {
        if (entry == null) return null;
        return MESHES.get(entry.getId());
    }

    private static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        Map<String, Map<BlockPos, BlockState>> sampled = new LinkedHashMap<>();
        for (AlmanacEntry entry : allEntries()) {
            BuildLayout layout = getLayout(server, entry);
            if (layout.isEmpty()) {
                LoggerProject.logWarning(CLASS_ID + "001", "No build layout for almanac entry " + entry.getId() + ", skipping its icon mesh");
                continue;
            }
            sampled.put(entry.getId(), sample(layout, ICON_SIZE));
        }
        Minecraft.getInstance().execute(() -> buildMeshes(sampled));
    }

    private static List<AlmanacEntry> allEntries() {
        List<AlmanacEntry> entries = new ArrayList<>();
        for (AlmanacCategory category : AlmanacCategory.values()) {
            for (List<AlmanacEntry> row : AlmanacCatalog.getRows(category)) {
                for (AlmanacEntry entry : row) {
                    if (entry != null) entries.add(entry);
                }
            }
        }
        return entries;
    }

    private static BuildLayout getLayout(MinecraftServer server, AlmanacEntry entry) {
        if (entry.category().generated) return BuildShapes.getLayout(entry);

        ResourceLocation id = entry.getStructureId();
        ResourceLocation file = new ResourceLocation(id.getNamespace(), "structures/" + id.getPath() + ".nbt");
        Optional<Resource> resource = server.getResourceManager().getResource(file);
        if (resource.isEmpty()) return BuildLayout.EMPTY;

        try (InputStream in = resource.get().open()) {
            return readNbt(in);
        } catch (IOException e) {
            LoggerProject.logError(CLASS_ID + "002", "Could not read almanac structure " + file + ": " + e.getMessage());
            return BuildLayout.EMPTY;
        }
    }

    private static void buildMeshes(Map<String, Map<BlockPos, BlockState>> sampled) {
        clearMeshes();
        sampled.forEach((id, cells) -> MESHES.put(id, buildMesh(cells)));
        LoggerProject.logInfo(CLASS_ID + "003", "Built " + MESHES.size() + " almanac build icon meshes");
    }

    private static void clearMeshes() {
        MESHES.values().forEach(IconMesh::close);
        MESHES.clear();
    }


    public static BuildLayout readNbt(InputStream in) throws IOException {
        CompoundTag tag = NbtIo.readCompressed(in);
        int version = NbtUtils.getDataVersion(tag, 500);
        tag = DataFixTypes.STRUCTURE.updateToCurrentVersion(DataFixers.getDataFixer(), tag, version);

        StructureTemplate template = new StructureTemplate();
        template.load(BuiltInRegistries.BLOCK.asLookup(), tag);
        return BuildLayout.fromTemplate(template);
    }


    public static Map<BlockPos, BlockState> sample(BuildLayout layout, int maxSize) {
        Map<BlockPos, BlockState> cells = new LinkedHashMap<>();
        if (layout.isEmpty()) return cells;

        BoundingBox box = BoundingBox.encapsulatingPositions(layout.positions()).orElseThrow();
        int longest = Math.max(box.getXSpan(), Math.max(box.getYSpan(), box.getZSpan()));
        int factor = Math.max(1, (longest + maxSize - 1) / maxSize);

        Map<BlockPos, Map<BlockState, Integer>> counts = new LinkedHashMap<>();
        for (int i = 0; i < layout.positions().size(); i++) {
            BlockPos pos = layout.positions().get(i);
            BlockPos cell = new BlockPos(
                (pos.getX() - box.minX()) / factor,
                (pos.getY() - box.minY()) / factor,
                (pos.getZ() - box.minZ()) / factor);
            counts.computeIfAbsent(cell, c -> new HashMap<>()).merge(layout.states().get(i), 1, Integer::sum);
        }

        counts.forEach((cell, stateCounts) ->
            cells.put(cell, Collections.max(stateCounts.entrySet(), Map.Entry.comparingByValue()).getKey()));
        return cells;
    }

    public static IconMesh buildMesh(Map<BlockPos, BlockState> cells) {
        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
        BlockColors colors = mc.getBlockColors();
        RandomSource random = RandomSource.create();
        PoseStack poseStack = new PoseStack();
        Map<RenderType, BufferBuilder> builders = new HashMap<>();

        for (Map.Entry<BlockPos, BlockState> cell : cells.entrySet()) {
            BlockPos pos = cell.getKey();
            BlockState state = cell.getValue();
            if (state.getRenderShape() != RenderShape.MODEL) continue;

            BakedModel model = dispatcher.getBlockModel(state);
            BufferBuilder builder = builders.computeIfAbsent(ItemBlockRenderTypes.getChunkRenderType(state), type -> {
                BufferBuilder b = new BufferBuilder(type.bufferSize());
                b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
                return b;
            });

            poseStack.pushPose();
            poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
            for (Direction dir : Direction.values()) {
                BlockState neighbor = cells.get(pos.relative(dir));
                if (neighbor != null && neighbor.isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) continue;
                random.setSeed(42L);
                putQuads(builder, poseStack, state, model.getQuads(state, dir, random), colors);
            }
            random.setSeed(42L);
            putQuads(builder, poseStack, state, model.getQuads(state, null, random), colors);
            poseStack.popPose();
        }

        Map<RenderType, VertexBuffer> buffers = new HashMap<>();
        builders.forEach((type, builder) -> {
            BufferBuilder.RenderedBuffer rendered = builder.endOrDiscardIfEmpty();
            if (rendered == null) return;
            VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            buffer.bind();
            buffer.upload(rendered);
            VertexBuffer.unbind();
            buffers.put(type, buffer);
        });

        BoundingBox box = BoundingBox.encapsulatingPositions(cells.keySet()).orElse(new BoundingBox(BlockPos.ZERO));
        return new IconMesh(buffers, new Vec3i(box.getXSpan(), box.getYSpan(), box.getZSpan()));
    }

    private static void putQuads(VertexConsumer consumer, PoseStack poseStack, BlockState state, List<BakedQuad> quads, BlockColors colors) {
        for (BakedQuad quad : quads) {
            int tint = quad.isTinted() ? colors.getColor(state, null, null, quad.getTintIndex()) : -1;
            float shade = quad.isShade() ? SHADE[quad.getDirection().ordinal()] : 1.0F;
            float r = (tint >> 16 & 255) / 255F * shade;
            float g = (tint >> 8 & 255) / 255F * shade;
            float b = (tint & 255) / 255F * shade;
            consumer.putBulkData(poseStack.last(), quad, r, g, b, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        }
    }


    public static class IconMesh implements AutoCloseable {

        private final Map<RenderType, VertexBuffer> buffers;
        private final Vec3i size;

        private IconMesh(Map<RenderType, VertexBuffer> buffers, Vec3i size) {
            this.buffers = buffers;
            this.size = size;
        }

        public Vec3i getSize() {
            return size;
        }

        public void render(PoseStack poseStack) {
            Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(poseStack.last().pose());
            FogRenderer.setupNoFog();

            // Solid layers first so cutout and translucent blend over them
            for (RenderType type : RenderType.chunkBufferLayers()) {
                VertexBuffer buffer = buffers.get(type);
                if (buffer == null) continue;
                type.setupRenderState();
                buffer.bind();
                buffer.drawWithShader(modelView, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
                type.clearRenderState();
            }
            VertexBuffer.unbind();
        }

        @Override
        public void close() {
            buffers.values().forEach(VertexBuffer::close);
        }
    }
}
