package com.studenthelper.crdt.operation;

/**
 * Types of CRDT atomic operations generated during offline and online editing.
 */
public enum OperationType {
    CREATE_NOTE,
    UPDATE_TITLE,
    INSERT_BLOCK,
    UPDATE_BLOCK_CONTENT,
    MOVE_BLOCK,
    TOGGLE_BLOCK_CHECK,
    DELETE_BLOCK,
    ADD_TAG,
    REMOVE_TAG,
    DELETE_NOTE,
    UPDATE_PROGRESS
}
