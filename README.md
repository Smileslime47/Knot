 # Knot - A Rope Data Structure Library Implemented in Kotlin

## Why Use Rope?

**Rope** is a linear data structure designed to optimize the complexity of all core operations (insertion, query, and deletion) to . In most scenarios, you might not actually need a Rope, as `String` and `List` are perfectly capable of meeting your requirements.

The most classic use case for a Rope is in **large-scale document editing**. For a `String` (which is essentially a `Char` array), when you press Enter on the -th line of a document with  lines, the following happens under the hood: the content from lines  is shifted to lines , and then line  is set as an empty line. Obviously, this is an  operation. When a document exceeds 1MB, performance degradation becomes noticeable. The same limitation applies to a `List`.

Another scenario for Rope is **reverse index lookup**. When you need to quickly find the index of an element within a linear data structure, it typically requires  time complexity. Even if we cache the index information within the element fields, we still need to update the fields of all elements from  onwards whenever the -th element is inserted or deleted.