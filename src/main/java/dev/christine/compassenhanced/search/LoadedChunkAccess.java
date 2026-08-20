package dev.christine.compassenhanced.search;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

final class LoadedChunkAccess {
    private LoadedChunkAccess() {
    }

    static WorldChunk get(ServerWorld world, int chunkX, int chunkZ) {
        Chunk chunk = world.getChunkManager().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
        return chunk instanceof WorldChunk worldChunk ? worldChunk : null;
    }
}
