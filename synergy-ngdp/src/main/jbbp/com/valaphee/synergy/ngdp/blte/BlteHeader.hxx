>int magic;
>int size;
>ubyte flags;
>uint24 chunk_count;
chunk[chunk_count] {
    >int compressed_size;
    >int uncompressed_size;
    >ubyte[16] checksum;
}
