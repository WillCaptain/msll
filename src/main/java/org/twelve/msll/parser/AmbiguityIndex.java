package org.twelve.msll.parser;

import org.twelve.msll.parsetree.ParseNode;
import org.twelve.msll.util.GrammarAmbiguity;

/**
 * Immutable 32-way hash-array mapped trie of ambiguity checkpoints.
 *
 * <p>A fork shares the parent root and copies only the path to the new entry.
 * Lookup uses {@link ParseNode} identity, the same contract as the HashMap this
 * index replaces. A later entry for the same node overrides the earlier one
 * without mutating the older root.
 */
final class AmbiguityIndex {
    static final AmbiguityIndex EMPTY = new AmbiguityIndex(Empty.INSTANCE);

    private final Node root;

    private AmbiguityIndex(Node root) {
        this.root = root;
    }

    GrammarAmbiguity get(ParseNode key) {
        if (key == null) return null;
        return root.get(key, key.hashCode(), 0);
    }

    AmbiguityIndex with(ParseNode key, GrammarAmbiguity value) {
        if (key == null || value == null) return this;
        Node next = root.assoc(key, key.hashCode(), value, 0);
        return next == root ? this : new AmbiguityIndex(next);
    }

    private interface Node {
        GrammarAmbiguity get(ParseNode key, int hash, int shift);

        Node assoc(ParseNode key, int hash, GrammarAmbiguity value, int shift);
    }

    private static final class Empty implements Node {
        static final Empty INSTANCE = new Empty();

        @Override
        public GrammarAmbiguity get(ParseNode key, int hash, int shift) {
            return null;
        }

        @Override
        public Node assoc(ParseNode key, int hash, GrammarAmbiguity value, int shift) {
            return new Leaf(key, hash, value);
        }
    }

    private static final class Leaf implements Node {
        final ParseNode key;
        final int hash;
        final GrammarAmbiguity value;

        Leaf(ParseNode key, int hash, GrammarAmbiguity value) {
            this.key = key;
            this.hash = hash;
            this.value = value;
        }

        @Override
        public GrammarAmbiguity get(ParseNode key, int hash, int shift) {
            return this.key.equals(key) ? value : null;
        }

        @Override
        public Node assoc(ParseNode key, int hash, GrammarAmbiguity value, int shift) {
            if (this.key.equals(key)) {
                return value == this.value ? this : new Leaf(key, hash, value);
            }
            if (shift >= 32) {
                return new Collision(new Leaf[]{this, new Leaf(key, hash, value)});
            }
            return new Bitmap(0, new Node[0]).assoc(this.key, this.hash, this.value, shift)
                    .assoc(key, hash, value, shift);
        }
    }

    private static final class Collision implements Node {
        final Leaf[] entries;

        Collision(Leaf[] entries) {
            this.entries = entries;
        }

        @Override
        public GrammarAmbiguity get(ParseNode key, int hash, int shift) {
            for (Leaf entry : entries) {
                if (entry.key.equals(key)) return entry.value;
            }
            return null;
        }

        @Override
        public Node assoc(ParseNode key, int hash, GrammarAmbiguity value, int shift) {
            for (int i = 0; i < entries.length; i++) {
                if (entries[i].key.equals(key)) {
                    if (entries[i].value == value) return this;
                    Leaf[] next = entries.clone();
                    next[i] = new Leaf(key, hash, value);
                    return new Collision(next);
                }
            }
            Leaf[] next = new Leaf[entries.length + 1];
            System.arraycopy(entries, 0, next, 0, entries.length);
            next[entries.length] = new Leaf(key, hash, value);
            return new Collision(next);
        }
    }

    private static final class Bitmap implements Node {
        final int bitmap;
        final Node[] children;

        Bitmap(int bitmap, Node[] children) {
            this.bitmap = bitmap;
            this.children = children;
        }

        @Override
        public GrammarAmbiguity get(ParseNode key, int hash, int shift) {
            int bit = bit(hash, shift);
            if ((bitmap & bit) == 0) return null;
            return children[index(bit)].get(key, hash, shift + 5);
        }

        @Override
        public Node assoc(ParseNode key, int hash, GrammarAmbiguity value, int shift) {
            int bit = bit(hash, shift);
            int idx = index(bit);
            if ((bitmap & bit) == 0) {
                Node[] next = new Node[children.length + 1];
                System.arraycopy(children, 0, next, 0, idx);
                next[idx] = new Leaf(key, hash, value);
                System.arraycopy(children, idx, next, idx + 1, children.length - idx);
                return new Bitmap(bitmap | bit, next);
            }
            Node updated = children[idx].assoc(key, hash, value, shift + 5);
            if (updated == children[idx]) return this;
            Node[] next = children.clone();
            next[idx] = updated;
            return new Bitmap(bitmap, next);
        }

        private int index(int bit) {
            return Integer.bitCount(bitmap & (bit - 1));
        }
    }

    private static int bit(int hash, int shift) {
        return 1 << ((hash >>> shift) & 31);
    }
}
