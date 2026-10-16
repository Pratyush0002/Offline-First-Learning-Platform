package com.studenthelper.crdt.engine;

import com.studenthelper.crdt.clock.VersionVector;
import com.studenthelper.crdt.model.BlockType;
import com.studenthelper.crdt.model.CrdtBlock;
import com.studenthelper.crdt.model.CrdtNote;
import com.studenthelper.crdt.model.FractionalIndex;
import com.studenthelper.crdt.operation.CrdtOperation;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Core CRDT Engine ensuring mathematical convergence (commutativity, associativity, idempotency)
 * across multiple offline devices synchronizing their edits.
 */
public class CrdtEngine {

    /**
     * Applies a single CRDT operation to the target note in an idempotent, deterministic manner.
     */
    public static boolean applyOperation(CrdtNote note, CrdtOperation op) {
        if (note == null || op == null) return false;

        boolean modified = false;

        switch (op.getType()) {
            case CREATE_NOTE -> {
                // If the note was created with a title
                if (op.getPayload() != null && !op.getPayload().isEmpty()) {
                    modified = note.applyTitleUpdate(op.getPayload(), op.getClientId(), op.getLamportClock());
                }
                if (op.getTargetId() != null && note.getLessonId() == null) {
                    note.setLessonId(op.getTargetId());
                    modified = true;
                }
            }
            case UPDATE_TITLE -> {
                modified = note.applyTitleUpdate(op.getPayload(), op.getClientId(), op.getLamportClock());
            }
            case INSERT_BLOCK -> {
                String blockId = op.getTargetId();
                String posKey = op.getPayload();
                String contentAndType = op.getSecondaryPayload();

                String content = "";
                BlockType type = BlockType.PARAGRAPH;

                if (contentAndType != null) {
                    int sep = contentAndType.lastIndexOf(":::");
                    if (sep != -1) {
                        content = contentAndType.substring(0, sep);
                        try {
                            type = BlockType.valueOf(contentAndType.substring(sep + 3));
                        } catch (Exception ignored) {}
                    } else {
                        content = contentAndType;
                    }
                }

                FractionalIndex fracPos = new FractionalIndex(posKey, op.getClientId(), op.getLamportClock());
                CrdtBlock existing = note.getBlocks().get(blockId);

                if (existing == null) {
                    CrdtBlock newBlock = new CrdtBlock(
                            blockId,
                            fracPos,
                            type,
                            content,
                            false,
                            false,
                            op.getLamportClock(),
                            op.getClientId(),
                            op.getTimestamp()
                    );
                    note.getBlocks().put(blockId, newBlock);
                    modified = true;
                } else {
                    // Block already exists, update if this op is newer
                    if (isNewer(op.getLamportClock(), op.getClientId(), existing.getLamportClock(), existing.getClientId())) {
                        existing.setPosition(fracPos);
                        existing.setType(type);
                        existing.setContent(content);
                        existing.setLamportClock(op.getLamportClock());
                        existing.setClientId(op.getClientId());
                        existing.setLastModified(op.getTimestamp());
                        modified = true;
                    }
                }
            }
            case UPDATE_BLOCK_CONTENT -> {
                String blockId = op.getTargetId();
                CrdtBlock block = note.getBlocks().get(blockId);
                if (block != null) {
                    if (isNewer(op.getLamportClock(), op.getClientId(), block.getLamportClock(), block.getClientId())) {
                        block.setContent(op.getPayload());
                        block.setLamportClock(op.getLamportClock());
                        block.setClientId(op.getClientId());
                        block.setLastModified(op.getTimestamp());
                        modified = true;
                    }
                }
            }
            case TOGGLE_BLOCK_CHECK -> {
                String blockId = op.getTargetId();
                CrdtBlock block = note.getBlocks().get(blockId);
                if (block != null) {
                    if (isNewer(op.getLamportClock(), op.getClientId(), block.getLamportClock(), block.getClientId())) {
                        block.setChecked(Boolean.parseBoolean(op.getPayload()));
                        block.setLamportClock(op.getLamportClock());
                        block.setClientId(op.getClientId());
                        block.setLastModified(op.getTimestamp());
                        modified = true;
                    }
                }
            }
            case MOVE_BLOCK -> {
                String blockId = op.getTargetId();
                CrdtBlock block = note.getBlocks().get(blockId);
                if (block != null) {
                    if (isNewer(op.getLamportClock(), op.getClientId(), block.getLamportClock(), block.getClientId())) {
                        block.setPosition(new FractionalIndex(op.getPayload(), op.getClientId(), op.getLamportClock()));
                        block.setLamportClock(op.getLamportClock());
                        block.setClientId(op.getClientId());
                        block.setLastModified(op.getTimestamp());
                        modified = true;
                    }
                }
            }
            case DELETE_BLOCK -> {
                String blockId = op.getTargetId();
                CrdtBlock block = note.getBlocks().get(blockId);
                if (block != null) {
                    if (op.getLamportClock() >= block.getLamportClock()) {
                        block.setDeleted(true);
                        block.setLamportClock(op.getLamportClock());
                        block.setClientId(op.getClientId());
                        block.setLastModified(op.getTimestamp());
                        modified = true;
                    }
                }
            }
            case ADD_TAG -> {
                note.addTag(op.getTargetId(), op.getLamportClock(), op.getClientId());
                modified = true;
            }
            case REMOVE_TAG -> {
                note.removeTag(op.getTargetId(), op.getLamportClock(), op.getClientId());
                modified = true;
            }
            case DELETE_NOTE -> {
                note.markDeleted(op.getLamportClock(), op.getClientId());
                modified = true;
            }
            default -> {}
        }

        note.getVersionVector().updateMax(op.getClientId(), op.getLamportClock());
        return modified;
    }

    /**
     * Applies a list of operations in total causal order (sorted by Lamport clock and ClientId).
     */
    public static void applyBatch(CrdtNote note, List<CrdtOperation> ops) {
        if (note == null || ops == null || ops.isEmpty()) return;

        List<CrdtOperation> sortedOps = new ArrayList<>(ops);
        Collections.sort(sortedOps);

        for (CrdtOperation op : sortedOps) {
            applyOperation(note, op);
        }
    }

    /**
     * Filters operations from history that the remote node has not yet observed.
     */
    public static List<CrdtOperation> computeMissingOperations(List<CrdtOperation> history, VersionVector remoteVector) {
        if (history == null) return Collections.emptyList();
        if (remoteVector == null) return new ArrayList<>(history);

        return history.stream()
                .filter(op -> op.getLamportClock() > remoteVector.getClock(op.getClientId()))
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Merges two complete CrdtNote instances into a new converged instance.
     */
    public static CrdtNote merge(CrdtNote a, CrdtNote b) {
        if (a == null) return b != null ? b.copy() : null;
        if (b == null) return a.copy();

        // Title LWW
        String title = a.getTitle();
        long titleClock = a.getTitleClock();
        String titleClient = a.getTitleClientId();

        if (isNewer(b.getTitleClock(), b.getTitleClientId(), a.getTitleClock(), a.getTitleClientId())) {
            title = b.getTitle();
            titleClock = b.getTitleClock();
            titleClient = b.getTitleClientId();
        }

        // Deleted status
        boolean deleted = a.isDeleted() || b.isDeleted();
        long deleteClock = Math.max(a.getDeleteClock(), b.getDeleteClock());

        // Merged Blocks
        Map<String, CrdtBlock> mergedBlocks = new HashMap<>();
        Set<String> allBlockIds = new HashSet<>(a.getBlocks().keySet());
        allBlockIds.addAll(b.getBlocks().keySet());

        for (String blockId : allBlockIds) {
            CrdtBlock blockA = a.getBlocks().get(blockId);
            CrdtBlock blockB = b.getBlocks().get(blockId);

            if (blockA != null && blockB != null) {
                // Both have block, pick newer version
                if (isNewer(blockB.getLamportClock(), blockB.getClientId(), blockA.getLamportClock(), blockA.getClientId())) {
                    mergedBlocks.put(blockId, copyBlock(blockB));
                } else {
                    mergedBlocks.put(blockId, copyBlock(blockA));
                }
                // If either marked it deleted with higher/equal clock, keep deleted
                if (blockA.isDeleted() && blockA.getLamportClock() >= mergedBlocks.get(blockId).getLamportClock()) {
                    mergedBlocks.get(blockId).setDeleted(true);
                } else if (blockB.isDeleted() && blockB.getLamportClock() >= mergedBlocks.get(blockId).getLamportClock()) {
                    mergedBlocks.get(blockId).setDeleted(true);
                }
            } else if (blockA != null) {
                mergedBlocks.put(blockId, copyBlock(blockA));
            } else {
                mergedBlocks.put(blockId, copyBlock(blockB));
            }
        }

        // Merged Tags
        Map<String, CrdtNote.TagRecord> mergedTags = new HashMap<>();
        Set<String> allTags = new HashSet<>(a.getTags().keySet());
        allTags.addAll(b.getTags().keySet());

        for (String tag : allTags) {
            CrdtNote.TagRecord recA = a.getTags().get(tag);
            CrdtNote.TagRecord recB = b.getTags().get(tag);

            long added = Math.max(recA != null ? recA.addedClock : 0, recB != null ? recB.addedClock : 0);
            long removed = Math.max(recA != null ? recA.removedClock : 0, recB != null ? recB.removedClock : 0);

            mergedTags.put(tag, new CrdtNote.TagRecord(added, removed));
        }

        // Merged Version Vector
        VersionVector mergedVector = a.getVersionVector().clone().merge(b.getVersionVector());

        return new CrdtNote(
                a.getId(),
                a.getLessonId() != null ? a.getLessonId() : b.getLessonId(),
                a.getAuthorId(),
                title,
                titleClock,
                titleClient,
                mergedBlocks,
                mergedTags,
                deleted,
                deleteClock,
                Math.min(a.getCreatedAt(), b.getCreatedAt()),
                Math.max(a.getUpdatedAt(), b.getUpdatedAt()),
                mergedVector
        );
    }

    private static boolean isNewer(long clock1, String client1, long clock2, String client2) {
        if (clock1 > clock2) return true;
        if (clock1 < clock2) return false;
        // Clock tie-breaker: deterministic client ID comparison
        return client1 != null && client2 != null && client1.compareTo(client2) > 0;
    }

    private static CrdtBlock copyBlock(CrdtBlock b) {
        return new CrdtBlock(
                b.getBlockId(),
                b.getPosition(),
                b.getType(),
                b.getContent(),
                b.isChecked(),
                b.isDeleted(),
                b.getLamportClock(),
                b.getClientId(),
                b.getLastModified()
        );
    }
}
