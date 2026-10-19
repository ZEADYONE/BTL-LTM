package com.bomberman.common.dto;

import com.bomberman.common.enums.Direction;

/**
 * One step request. {@code seq} increases per move so the client can match server acknowledgements
 * ({@link GamePlayerStateDto#lastMoveSeq()}) to its predicted steps; 0 means the client does not predict.
 */
public record MoveRequest(Direction direction, long seq) {

    public MoveRequest(Direction direction) {
        this(direction, 0);
    }
}
