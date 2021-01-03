package bnorm.kdtree

// Thanks to DrussGT for the idea of this performance boost
class SmallSizedHeap<T : Comparable<T>>(size: Int) {
    private var _size = 0
    private val array: Array<Any?> = arrayOfNulls(size)

    val size: Int get() = _size

    val first: T get() = get(0)
    val last: T get() = get(_size - 1)

    private fun get(index: Int): T {
        @Suppress("UNCHECKED_CAST")
        return array[index] as T
    }

    fun add(value: T) {
        if (_size == 0) {
            array[0] = value
            _size++
            return
        } else if (_size == array.size && get(_size - 1) < value) {
            return
        } else {
            val search = array.binarySearch(value, toIndex = _size)
            val insert = if (search < 0) -search - 1 else search
            if (_size < array.size) {
                array.copyInto(array, insert + 1, insert, _size)
                _size++
            } else {
                array.copyInto(array, insert + 1, insert, _size - 1)
            }
            array[insert] = value
        }
    }

    fun toList(): List<T> {
        @Suppress("UNCHECKED_CAST")
        return array.asList().subList(0, _size) as List<T>
    }
}
