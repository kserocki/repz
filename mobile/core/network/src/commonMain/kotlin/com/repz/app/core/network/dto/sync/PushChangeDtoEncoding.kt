package com.repz.app.core.network.dto.sync

import com.repz.app.core.network.client.NetworkJson

/**
 * This change's size, in bytes, exactly as it goes on the wire inside a [PushRequestDto]'s
 * `changes` array (`core:sync`'s push batching, ADR-0020's 1 MiB request cap).
 */
fun PushChangeDto.encodedSizeInBytes(): Int = NetworkJson.encodeToString(PushChangeDto.serializer(), this).encodeToByteArray().size
