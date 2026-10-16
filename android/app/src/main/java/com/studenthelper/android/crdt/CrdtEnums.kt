package com.studenthelper.android.crdt

enum class OperationType {
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

enum class BlockType {
    PARAGRAPH,
    HEADING_1,
    HEADING_2,
    HEADING_3,
    BULLET_ITEM,
    NUMBERED_ITEM,
    TODO_ITEM,
    CODE_SNIPPET,
    CALLOUT_NOTE,
    KEY_FORMULA
}
