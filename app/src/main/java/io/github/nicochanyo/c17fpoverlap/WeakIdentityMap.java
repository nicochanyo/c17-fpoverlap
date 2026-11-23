package io.github.nicochanyo.c17fpoverlap;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Identity-based weak-key map safe for mutable Kotlin data classes whose
 * hashCode()/equals() depend on mutated fields, with zero-allocation lookups.
 */
final class WeakIdentityMap<K, V> {
    private static final class Key extends WeakReference<Object> {
        final int hash;

        Key(Object referent, ReferenceQueue<Object> queue) {
            super(referent, queue);
            this.hash = System.identityHashCode(referent);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof Key) {
                Object a = get();
                Object b = ((Key) o).get();
                return a != null && a == b;
            }
            if (o instanceof LookupKey) {
                Object a = get();
                return a != null && a == ((LookupKey) o).ref;
            }
            return false;
        }
    }

    private static final class LookupKey {
        Object ref;
        int hash;

        void set(Object ref) {
            this.ref = ref;
            this.hash = System.identityHashCode(ref);
        }

        void clear() {
            this.ref = null;
            this.hash = 0;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Key) {
                Object a = ((Key) o).get();
                return a != null && a == ref;
            }
            return o == this;
        }
    }

    private final ReferenceQueue<Object> queue = new ReferenceQueue<>();
    private final HashMap<Object, V> map = new HashMap<>();
    private final LookupKey lookup = new LookupKey();

    private void expunge() {
        Key k;
        while ((k = (Key) queue.poll()) != null) {
            map.remove(k);
        }
    }

    synchronized V get(K key) {
        if (key == null) {
            return null;
        }
        expunge();
        lookup.set(key);
        try {
            return map.get(lookup);
        } finally {
            lookup.clear();
        }
    }

    synchronized V put(K key, V value) {
        if (key == null) {
            return null;
        }
        expunge();
        return map.put(new Key(key, queue), value);
    }

    synchronized V remove(K key) {
        if (key == null) {
            return null;
        }
        expunge();
        lookup.set(key);
        try {
            return map.remove(lookup);
        } finally {
            lookup.clear();
        }
    }

    synchronized boolean containsKey(K key) {
        if (key == null) {
            return false;
        }
        expunge();
        lookup.set(key);
        try {
            return map.containsKey(lookup);
        } finally {
            lookup.clear();
        }
    }

    synchronized void clear() {
        while (queue.poll() != null) {
            // drain cleared references
        }
        map.clear();
    }

    synchronized boolean isEmpty() {
        expunge();
        return map.isEmpty();
    }

    @SuppressWarnings("unchecked")
    synchronized ArrayList<K> keySnapshot() {
        expunge();
        ArrayList<K> out = new ArrayList<>(map.size());
        for (Object raw : map.keySet()) {
            if (raw instanceof Key) {
                Object ref = ((Key) raw).get();
                if (ref != null) {
                    out.add((K) ref);
                }
            }
        }
        return out;
    }
}
