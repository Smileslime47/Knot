package moe.saikyo47.rope.index

class IndexedRopeIterator<E : Any>(
    private val rope: IndexedRope<E>
) : MutableIterator<E> {

    private var cursor: Int = 0
    private var lastReturnedIndex: Int = -1

    override fun hasNext(): Boolean = cursor < rope.size

    override fun next(): E {
        if (!hasNext()) throw NoSuchElementException()

        val value = rope[cursor]
        lastReturnedIndex = cursor
        cursor += 1
        return value
    }

    override fun remove() {
        if (lastReturnedIndex < 0) {
            throw IllegalStateException("Please call next() before remove()")
        }

        rope.removeAt(lastReturnedIndex)
        cursor = lastReturnedIndex
        lastReturnedIndex = -1
    }
}
