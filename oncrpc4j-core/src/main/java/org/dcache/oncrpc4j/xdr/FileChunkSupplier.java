package org.dcache.oncrpc4j.xdr;

import org.glassfish.grizzly.FileChunk;

/**
 * Provides something as a Grizzly {@link FileChunk}.
 * 
 * @author Christian Kohlschütter
 */
public interface FileChunkSupplier {
    /**
     * Returns a {@link FileChunk}.
     * 
     * @param usePadding If {@code true}, then {@link FileChunk} must be padded to 4 bytes. 
     * 
     * @return The {@link FileChunk}.
     */
    FileChunk toFileChunk(boolean usePadding);
}
