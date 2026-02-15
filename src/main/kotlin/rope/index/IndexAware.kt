package moe.saikyo47.rope.index

/**
 * 给行号映射对象建立与行引用的双向绑定
 * 便于当行号映射对象有反查行号的需求时，能够通过内部的ref绑定来反向获取行号
 *
 * 使用递归泛型来约束LineRef的泛型和映射对象类型的一致性
 * 使用样例：
 * class Index:LineAware<Index>
 * override val ref: LineReference<Cli> = LineReference(this)
 */
abstract class IndexAware<E> {
    internal var _ref: IndexRef<E>? = null
    internal var ref: IndexRef<E>?
        get() = _ref
        set(value) {
            require(value?.value == this) { "Reference value must be self instance." }
            _ref = value
        }

    val index: Int get() = ref?.index ?: -1

    init{
        @Suppress("UNCHECKED_CAST")
        this.ref = ElementRef<E>(this as E)
    }
}

fun <E : IndexAware<E>> List<E?>.toRefList(): List<IndexRef<E>> {
    return this.map {
        if (it != null && it.ref != null) {
            it.ref!!
        } else {
            EmptyRef
        }
    }
}